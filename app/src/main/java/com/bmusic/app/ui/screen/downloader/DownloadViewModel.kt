package com.bmusic.app.ui.screen.downloader

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bmusic.app.downloader.data.DownloadEntity
import com.bmusic.app.downloader.data.DownloadState
import com.bmusic.app.downloader.engine.MediaExtractorEngine
import com.bmusic.app.downloader.engine.MediaInfo
import com.bmusic.app.downloader.engine.StreamKind
import com.bmusic.app.downloader.error.DownloadError
import com.bmusic.app.downloader.error.DownloadErrorCode
import com.bmusic.app.downloader.error.ErrorAction
import com.bmusic.app.downloader.manager.DownloadManager
import com.bmusic.app.downloader.data.DownloadDao
import com.bmusic.app.downloader.security.UrlValidator
import com.bmusic.app.downloader.trace.DownloadTrace
import com.bmusic.app.downloader.trace.TraceStep
import com.bmusic.app.util.Preferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Étape 5 : état partagé de l'écran Télécharger (socle + onglet Lien).
 * Onglet Recherche ajouté à l'étape 6 dans le même écran.
 */
class DownloadViewModel(
    private val engine: MediaExtractorEngine,
    private val downloadManager: DownloadManager,
    private val dao: DownloadDao,
    private val prefs: Preferences,
) : ViewModel() {

    data class LinkState(
        val url: String = "",
        val error: String? = null,
        /** Code typé de la dernière erreur (jamais null si [error] est non null). */
        val errorCode: DownloadErrorCode? = null,
        /** Action proposée à l'utilisateur pour cette erreur. */
        val errorAction: ErrorAction? = null,
        val info: MediaInfo? = null,
        val audioVideo: StreamKind = StreamKind.AUDIO,
        val quality: String? = null,
        val busy: Boolean = false,
        val queuedId: Long? = null,
        val duplicate: Boolean = false,
    )

    private val _link = MutableStateFlow(LinkState())
    val link: StateFlow<LinkState> = _link.asStateFlow()

    private val _downloads = MutableStateFlow<List<DownloadEntity>>(emptyList())
    val downloads: StateFlow<List<DownloadEntity>> = _downloads.asStateFlow()

    private val _tab = MutableStateFlow(0) // 0 = Lien (défaut), 1 = Historique
    val tab: StateFlow<Int> = _tab.asStateFlow()

    /** Session de journal courante (résolution → aperçu → téléchargement). */
    private var currentSessionId: String? = null

    val isEnabled: Boolean get() = prefs.downloaderEnabled

    init {
        refreshDownloads()
    }

    fun selectTab(i: Int) { _tab.value = i.coerceIn(0, 1) }

    fun onUrlChange(u: String) { _link.value = _link.value.copy(url = u, error = null) }

    fun prefill(url: String, toLink: Boolean) {
        // L'onglet Lien est le seul onglet de saisie (index 0) : tout partage
        // atterrit dessus. La recherche YouTube n'existe plus.
        _tab.value = 0
        _link.value = _link.value.copy(url = if (toLink) url else _link.value.url, error = null)
        if (toLink) {
            _link.value = _link.value.copy(url = url)
            // Aucun démarrage automatique (règle du plan).
        }
    }

    fun resolveLink() {
        val url = _link.value.url.trim()
        if (!prefs.downloaderEnabled) return
        // Une session de journal par tentative : c'est elle que l'utilisateur
        // retrouve dans l'écran « Journal » et qu'il partage.
        val session = DownloadTrace.startSession(url)
        currentSessionId = session.id

        // VALIDATION : **toute** entrée (champ, presse-papiers, partage,
        // retéléchargement depuis l'historique) passe par le validateur avant
        // d'atteindre le moteur. Une URL malformée n'est jamais requêtée.
        val validation = UrlValidator.validate(url)
        val cleanUrl = when (validation) {
            is UrlValidator.Validation.Invalid -> {
                val typed = DownloadError(DownloadErrorCode.LIEN_INVALIDE, validation.reason.name)
                session.fail(TraceStep.VALIDATION, typed)
                _link.value = _link.value.copy(
                    busy = false,
                    error = typed.userMessage,
                    errorCode = typed.code,
                    errorAction = typed.action,
                )
                return
            }
            is UrlValidator.Validation.Valid -> validation.normalized
        }
        _link.value = _link.value.copy(
            url = cleanUrl,
            busy = true,
            error = null,
            errorCode = null,
            errorAction = null,
        )
        viewModelScope.launch {
            try {
                val info = engine.resolve(cleanUrl, session)
                session.ok(TraceStep.APERCU, info.title)
                val audio = info.streams.filter { it.kind == StreamKind.AUDIO }
                val video = info.streams.filter { it.kind == StreamKind.VIDEO }
                val preferred = if (_link.value.audioVideo == StreamKind.AUDIO) {
                    audio.maxByOrNull { it.bitrateKbps ?: 0 }
                } else {
                    video.maxByOrNull { it.bitrateKbps ?: 0 }
                }
                session.ok(TraceStep.FORMAT_CHOISI, preferred?.format)
                _link.value = _link.value.copy(
                    info = info,
                    busy = false,
                    quality = preferred?.format,
                    duplicate = false,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Une seule conversion typée : aucune exception ne se perd,
                // aucun message n'est affiché sous un autre nom.
                val typed = DownloadError.fromExtractor(e)
                session.fail(TraceStep.EXTRACTION, typed)
                _link.value = _link.value.copy(
                    busy = false,
                    error = typed.userMessage,
                    errorCode = typed.code,
                    errorAction = typed.action,
                )
            }
        }
    }

    fun selectKind(kind: StreamKind) {
        val info = _link.value.info ?: return
        val list = info.streams.filter { it.kind == kind }
        val pick = list.maxByOrNull { it.bitrateKbps ?: 0 }
        _link.value = _link.value.copy(audioVideo = kind, quality = pick?.format)
    }

    fun selectQuality(format: String) { _link.value = _link.value.copy(quality = format) }

    fun enqueue() {
        val s = _link.value
        val info = s.info ?: return
        if (!prefs.downloaderEnabled) return
        val url = s.url.trim()
        val kind = s.audioVideo
        val stream = info.streams.filter { it.kind == kind }
            .maxByOrNull { it.bitrateKbps ?: 0 } ?: return
        val platformId = url.extractId()
        viewModelScope.launch {
            val entity = downloadManager.enqueue(
                info = info,
                platformId = platformId,
                sourceUrl = url,
                streamUrl = stream.url,
                format = stream.format.substringBefore(' ').take(10),
                isVideo = kind == StreamKind.VIDEO,
                wifiOnly = false,
                traceSessionId = currentSessionId,
            )
            _link.value = _link.value.copy(queuedId = entity?.id, duplicate = (entity != null))
            refreshDownloads()
        }
    }

    private fun refreshDownloads() {
        viewModelScope.launch {
            _downloads.value = try {
                dao.recent(50)
            } catch (e: Exception) {
                // Erreur de lecture de l'historique : tracée, pas avalée.
                Log.w("DownloadViewModel", "history refresh failed: ${e.message}")
                emptyList()
            }
        }
    }

    fun pause(id: Long) { viewModelScope.launch { downloadManager.pause(id) } }
    fun cancel(id: Long) { viewModelScope.launch { downloadManager.cancel(id) } }
    fun resume(id: Long) { viewModelScope.launch { downloadManager.resume(id) } }

    private fun String.extractId(): String {
        // Dérivons platformId du dernier segment — jamais utilisé pour le réseau.
        val clean = this.substringBefore('?').substringBefore('#')
        return clean.substringAfterLast('/').ifBlank { this }
    }
}
