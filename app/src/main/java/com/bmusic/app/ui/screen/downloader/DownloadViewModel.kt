package com.bmusic.app.ui.screen.downloader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bmusic.app.downloader.data.DownloadEntity
import com.bmusic.app.downloader.data.DownloadState
import com.bmusic.app.downloader.engine.ExtractorError
import com.bmusic.app.downloader.engine.MediaExtractorEngine
import com.bmusic.app.downloader.engine.MediaInfo
import com.bmusic.app.downloader.engine.StreamKind
import com.bmusic.app.downloader.manager.DownloadManager
import com.bmusic.app.downloader.data.DownloadDao
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

    private val _tab = MutableStateFlow(0) // 0 = Recherche, 1 = Lien
    val tab: StateFlow<Int> = _tab.asStateFlow()

    val isEnabled: Boolean get() = prefs.downloaderEnabled

    init {
        refreshDownloads()
    }

    fun selectTab(i: Int) { _tab.value = i.coerceIn(0, 1) }

    fun onUrlChange(u: String) { _link.value = _link.value.copy(url = u, error = null) }

    fun prefill(url: String, toLink: Boolean) {
        _tab.value = if (toLink) 1 else 0
        _link.value = _link.value.copy(url = if (toLink) url else _link.value.url, error = null)
        if (toLink) {
            _link.value = _link.value.copy(url = url)
            // Aucun démarrage automatique (règle du plan).
        }
        // Texte libre -> Recherche pré-remplie sans recherche : géré par
        // l'étape 6 via l'état partagé (ici on garde l'URL en Recherche future).
    }

    fun resolveLink() {
        val url = _link.value.url.trim()
        if (!prefs.downloaderEnabled) return
        _link.value = _link.value.copy(busy = true, error = null)
        viewModelScope.launch {
            try {
                val info = engine.resolve(url)
                val audio = info.streams.filter { it.kind == StreamKind.AUDIO }
                val video = info.streams.filter { it.kind == StreamKind.VIDEO }
                val preferred = if (_link.value.audioVideo == StreamKind.AUDIO) {
                    audio.maxByOrNull { it.bitrateKbps ?: 0 }
                } else {
                    video.maxByOrNull { it.bitrateKbps ?: 0 }
                }
                _link.value = _link.value.copy(
                    info = info,
                    busy = false,
                    quality = preferred?.format,
                    duplicate = false,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: ExtractorError.UrlInvalid) {
                _link.value = _link.value.copy(busy = false, error = "Lien invalide")
            } catch (e: ExtractorError.Unsupported) {
                _link.value = _link.value.copy(busy = false, error = "Source non prise en charge")
            } catch (e: ExtractorError.ProtectedContent) {
                _link.value = _link.value.copy(busy = false, error = "Contenu protégé ou privé")
            } catch (e: ExtractorError.SourceUnavailable) {
                _link.value = _link.value.copy(busy = false, error = "Source temporairement indisponible, moteur à mettre à jour")
            } catch (e: ExtractorError.Network) {
                _link.value = _link.value.copy(busy = false, error = "Hors ligne")
            } catch (e: Exception) {
                _link.value = _link.value.copy(busy = false, error = "Erreur moteur")
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

    fun enqueue() = enqueue(force = false)

    fun forceDownload() = enqueue(force = true)

    private fun enqueue(force: Boolean) {
        val s = _link.value
        val info = s.info ?: return
        if (!prefs.downloaderEnabled) return
        val url = s.url.trim()
        val kind = s.audioVideo
        val stream = info.streams.filter { it.kind == kind }
            .maxByOrNull { it.bitrateKbps ?: 0 } ?: return
        val platformId = url.extractId()
        viewModelScope.launch {
            // E1 : duplicate propagé depuis EnqueueResult (était silencieux, flag toujours vrai).
            val result = downloadManager.enqueue(
                info = info,
                platformId = platformId,
                sourceUrl = url,
                streamUrl = stream.url,
                format = stream.format.substringBefore(' ').take(10),
                isVideo = kind == StreamKind.VIDEO,
                wifiOnly = false,
                force = force,
            )
            _link.value = _link.value.copy(queuedId = result?.entity?.id, duplicate = result?.duplicate ?: false)
            refreshDownloads()
        }
    }

    fun dismissDuplicate() {
        _link.value = _link.value.copy(duplicate = false)
    }

    private fun refreshDownloads() {
        viewModelScope.launch {
            _downloads.value = try {
                dao.recent(50)
            } catch (_: Exception) { emptyList() }
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
