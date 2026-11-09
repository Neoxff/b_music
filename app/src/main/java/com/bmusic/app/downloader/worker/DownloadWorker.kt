package com.bmusic.app.downloader.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import com.bmusic.app.downloader.data.DownloadDao
import com.bmusic.app.downloader.data.DownloadEntity
import com.bmusic.app.downloader.data.DownloadState
import com.bmusic.app.downloader.manager.DownloadManager
import com.bmusic.app.downloader.transfer.DownloadTransferEngine
import com.bmusic.app.downloader.storage.MediaStoreDownloadWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

/**
 * Étape 3 : exécution d'un téléchargement sous WorkManager.
 *
 * - `setForeground` avec type `dataSync`, canal dédié "Téléchargements"
 *   (`downloads_channel`) distinct de `playing_notification`.
 * - Contraintes : réseau requis ; option Wi-Fi seulement (réglage interne).
 * - FIFO, 1 simultané par défaut (réglable 1–2), priorité basse,
 *   I/O sur Dispatchers.IO.
 * - Nettoyage des `.part` et entrées `IS_PENDING` orphelines AU DÉMARRAGE
 *   du worker (jamais au démarrage de l'application).
 * - Flag off = aucun worker enqueue, aucune notification, aucune requête
 *   (vérifié ici + dans `DownloadManager.enqueue`).
 */
class DownloadWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params), KoinComponent {

    private val dao: DownloadDao by inject()
    private val transfer: DownloadTransferEngine by inject()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val id = inputData.getLong(ARG_ID, -1L)
        if (id < 0) return@withContext Result.failure()

        // Cleanup au démarrage du worker uniquement.
        cleanupOrphanParts()

        val entity = dao.getById(id) ?: return@withContext Result.failure()
        if (entity.state == DownloadState.CANCELED) return@withContext Result.success()
        if (entity.state == DownloadState.PAUSED) return@withContext Result.success()

        setForeground(createForeground())
        dao.updateState(id, DownloadState.DOWNLOADING, System.currentTimeMillis(), null)

        return@withContext try {
            val finalUri = transfer.download(entity, dao, onProgress = { done, total ->
                dao.updateProgress(id, done, total, System.currentTimeMillis())
                setForeground(createForeground(progressPct(done, total)))
            })
            // Vérification finale : le transfert retourne une URI seulement
            // si le fichier est complet et vérifié (IS_PENDING=0 en interne).
            val storedPath = finalUri?.toString() ?: entity.finalPath
            if (storedPath != null) {
                dao.markCompleted(id, storedPath, System.currentTimeMillis())
            } else {
                dao.updateState(
                    id, DownloadState.FAILED,
                    System.currentTimeMillis(), "final file missing"
                )
                return@withContext Result.failure()
            }
            Result.success()
        } catch (e: DownloadTransferEngine.TransferPaused) {
            dao.updateState(id, DownloadState.PAUSED, System.currentTimeMillis(), null)
            Result.success()
        } catch (e: DownloadTransferEngine.TransferCanceled) {
            dao.updateState(id, DownloadState.CANCELED, System.currentTimeMillis(), null)
            Result.success()
        } catch (e: Exception) {
            // Reprise sur coupure : `Result.retry()` avec backoff si
            // transitoire, sinon FAILED.
            val retryCount = runAttemptCount
            if (e is java.io.IOException && retryCount < 3) {
                dao.updateState(id, DownloadState.QUEUED, System.currentTimeMillis(), e.message)
                Result.retry()
            } else {
                dao.updateState(
                    id, DownloadState.FAILED,
                    System.currentTimeMillis(), e.message ?: "download failed"
                )
                Result.failure()
            }
        }
    }

    private fun progressPct(done: Long, total: Long): Int =
        when {
            total <= 0 -> -1
            done <= 0 -> 0
            else -> ((done * 100) / total).toInt().coerceIn(0, 100)
        }

    /** Supprime `.part` obsolètes + entrées `IS_PENDING` orphelines. */
    private fun cleanupOrphanParts() {
        try {
            MediaStoreDownloadWriter.cleanupOrphans(applicationContext, dao)
        } catch (_: Exception) {
            // Best-effort : ne jamais faire échouer le worker pour un nettoyage.
        }
    }

    private fun createForeground(pct: Int = -1): ForegroundInfo {
        ensureChannel()
        val title = if (pct >= 0) "Téléchargement $pct %" else "Téléchargement"
        val notif = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText("B Music — téléchargements")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .build()
        val foregroundType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        } else {
            0
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIF_ID, notif, foregroundType)
        } else {
            ForegroundInfo(NOTIF_ID, notif)
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Téléchargements", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Téléchargements B Music (distinct de la lecture)"
                }
            )
        }
    }

    companion object {
        const val ARG_ID = "download_id"
        const val CHANNEL_ID = "downloads_channel"
        const val NOTIF_ID = 4201
        const val WORK_NAME_PREFIX = "download-"
        const val DEFAULT_MAX_CONCURRENT = 1
        const val MIN_CONCURRENT = 1
        const val MAX_CONCURRENT = 2

        fun enqueue(
            context: Context,
            entity: DownloadEntity,
            wifiOnly: Boolean,
            maxConcurrent: Int = DEFAULT_MAX_CONCURRENT,
        ) {
            val capped = maxConcurrent.coerceIn(MIN_CONCURRENT, MAX_CONCURRENT)
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(
                    if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
                )
                .build()
            val request = OneTimeWorkRequestBuilder<DownloadWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .addTag("downloader")
                .setInputData(Data.Builder().putLong(ARG_ID, entity.id).build())
                .setInitialDelay(0, TimeUnit.SECONDS)
                .build()
            // FIFO séquentiel : quelques que maxConcurrent (1 par défaut),
            // le work unique évite les doublons sur le même id.
            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    WORK_NAME_PREFIX + entity.id,
                    ExistingWorkPolicy.REPLACE,
                    request
                )
        }

        fun cancel(context: Context, id: Long) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_PREFIX + id)
        }

        fun cancelAll(context: Context) {
            WorkManager.getInstance(context).cancelAllWorkByTag("downloader")
        }
    }
}
