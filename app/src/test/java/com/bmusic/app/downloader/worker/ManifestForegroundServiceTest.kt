package com.bmusic.app.downloader.worker

import android.content.ComponentName
import android.content.pm.ServiceInfo
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Correctif du crash de téléchargement sur Android 14/15 (API 34+).
 *
 * `DownloadWorker` appelle
 * `setForeground(ForegroundInfo(id, notif, FOREGROUND_SERVICE_TYPE_DATA_SYNC))`.
 * Depuis Android 14, la plateforme **exige** que le service qui exécute le
 * worker déclare le type de service au premier plan correspondant ; sinon elle
 * lève `MissingForegroundServiceTypeException` et fait crasher le processus
 * (constaté sur Android 15, build release).
 *
 * Ce test lit le **manifeste fusionné** (celui que le système reçoit) et
 * vérifie la déclaration : il échoue sans le correctif de manifeste.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = com.bmusic.app.test.TestApplication::class)
class ManifestForegroundServiceTest {

    @Test
    fun `le service WorkManager declare le type dataSync`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val component = ComponentName(context, "androidx.work.impl.foreground.SystemForegroundService")
        val info = context.packageManager.getServiceInfo(component, 0)

        assertTrue(
            "le manifeste fusionne doit déclarer android:foregroundServiceType=" +
                "\"dataSync\" sur le service WorkManager (sinon crash au premier " +
                "setForeground sur Android 14+)",
            info.foregroundServiceType and ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC != 0,
        )
    }

    @Test
    fun `la permission FOREGROUND_SERVICE_DATA_SYNC est declaree`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val permissions = context.packageManager
            .getPackageInfo(context.packageName, android.content.pm.PackageManager.GET_PERMISSIONS)
            .requestedPermissions
            .orEmpty()

        assertTrue(
            "permission FOREGROUND_SERVICE_DATA_SYNC manquante",
            permissions.contains("android.permission.FOREGROUND_SERVICE_DATA_SYNC"),
        )
    }
}
