package com.bmusic.app.ui.screen.downloader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bmusic.app.downloader.data.SearchHistoryDao
import com.bmusic.app.downloader.data.SearchHistoryEntity
import com.bmusic.app.downloader.engine.ExtractorError
import com.bmusic.app.downloader.engine.MediaExtractorEngine
import com.bmusic.app.downloader.engine.SearchFilter
import com.bmusic.app.downloader.engine.SearchPage
import com.bmusic.app.downloader.engine.SearchResultItem
import com.bmusic.app.downloader.manager.DownloadManager
import com.bmusic.app.downloader.engine.StreamKind
import com.bmusic.app.downloader.security.QueryValidator
import com.bmusic.app.util.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Étape 6 : SearchViewModel — flux d'état unique.
 *
 * Réseau maîtrisé : debounce 400 ms, min 2 (QueryValidator),
 * annulation de la requête précédente à chaque nouvelle frappe,
 * AUCUNE requête quand l'onglet n'est pas visible (cancel onLeave),
 * cache borné, retry manuel seulement.
 */
@OptIn(ExperimentalStdlibApi::class)
class SearchViewModel(
    private val engine: MediaExtractorEngine,
    private val downloadManager: DownloadManager,
    private val historyDao: SearchHistoryDao,
    private val prefs: Preferences,
    val scope: CoroutineScope? = null,
) : ViewModel() {
    val effectiveScope: CoroutineScope get() = scope ?: viewModelScope

    sealed interface UiState {
        object Empty : UiState
        object Typing : UiState
        object Loading : UiState
        data class Results(val items: List<SearchResultItem>, val nextPage: Int?, val loadingMore: Boolean = false) : UiState
        object NoResult : UiState
        data class Error(val message: String) : UiState
        object Offline : UiState
        object SourceGone : UiState
        object TooShort : UiState
    }

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _state = MutableStateFlow<UiState>(UiState.Empty)
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _history = MutableStateFlow<List<SearchHistoryEntity>>(emptyList())
    val history: StateFlow<List<SearchHistoryEntity>> = _history.asStateFlow()

    private var debounceJob: Job? = null
    private var searchJob: Job? = null
    private var currentPage = 0
    private var isVisible = false

    fun setVisible(visible: Boolean) {
        isVisible = visible
        if (!visible) {
            // Aucune requête quand l'onglet n'est pas visible.
            effectiveScope.launch {
                debounceJob?.cancelAndJoin()
                searchJob?.cancelAndJoin()
            }
        }
    }

    init {
        refreshHistory()
    }

    fun onQueryChange(text: String) {
        _query.value = text
        debounceJob?.cancel()
        debounceJob = effectiveScope.launch {
            delay(400)
            if (!isVisible && !prefs.downloaderEnabled) return@launch
            if (!QueryValidator.isValid(text)) {
                _state.value = if (text.trim().isEmpty()) UiState.Empty else UiState.TooShort
                return@launch
            }
            runSearch(text, page = 0)
        }
    }

    fun searchNow() {
        val text = _query.value
        if (!QueryValidator.isValid(text)) {
            _state.value = UiState.TooShort
            return
        }
        debounceJob?.cancel()
        runSearch(text, page = 0)
    }

    fun loadNextPage() {
        val s = _state.value
        if (s is UiState.Results && s.nextPage != null && !s.loadingMore) {
            val next = s.nextPage
            _state.value = s.copy(loadingMore = true)
            searchJob = effectiveScope.launch {
                val before: UiState.Results = _state.value as? UiState.Results ?: return@launch
                try {
                    val page = engine.search(_query.value, SearchFilter.ALL, next)
                    val merged: List<SearchResultItem> =
                        (before.items + page.items).distinctBy { it.id }
                    _state.value = UiState.Results(merged, page.nextPageIndex)
                    currentPage = next
                } catch (_: Exception) {
                    val cur = _state.value as? UiState.Results ?: return@launch
                    _state.value = cur.copy(loadingMore = false)
                }
            }
        }
    }

    private fun runSearch(text: String, page: Int) {
        if (!prefs.downloaderEnabled || !isVisible) return
        searchJob?.cancel()
        searchJob = effectiveScope.launch {
            _state.value = UiState.Loading
            try {
                val cleaned = QueryValidator.cleaned(text) ?: run {
                    _state.value = UiState.TooShort
                    return@launch
                }
                val result = engine.search(cleaned, SearchFilter.ALL, page)
                if (result.items.isEmpty()) {
                    _state.value = UiState.NoResult
                } else {
                    _state.value = UiState.Results(result.items, result.nextPageIndex)
                    effectiveScope.launch { saveHistory(cleaned) }
                }
            } catch (e: ExtractorError.QueryInvalid) {
                _state.value = UiState.TooShort
            } catch (e: ExtractorError.Network) {
                _state.value = UiState.Offline
            } catch (e: ExtractorError.SourceUnavailable) {
                _state.value = UiState.SourceGone
            } catch (_: Exception) {
                _state.value = UiState.Offline
            }
        }
    }

    fun deleteHistory(id: Long) {
        effectiveScope.launch {
            historyDao.deleteById(id)
            refreshHistory()
        }
    }

    fun clearHistory() {
        effectiveScope.launch {
            historyDao.clearAll()
            refreshHistory()
        }
    }

    private fun refreshHistory() {
        effectiveScope.launch {
            _history.value = try { historyDao.recent() } catch (_: Exception) { emptyList() }
        }
    }

    private suspend fun saveHistory(cleaned: String) {
        try {
            historyDao.insert(SearchHistoryEntity(normalizedQuery = cleaned))
            historyDao.trimToMax10()
            refreshHistory()
        } catch (_: Exception) {}
    }

    /** Feuille d'aperçu : resolve(url) pour obtenir les streams. */
    suspend fun resolveResult(item: SearchResultItem): com.bmusic.app.downloader.engine.MediaInfo =
        engine.resolve(item.url)

    suspend fun enqueueResult(item: SearchResultItem) {
        val info = engine.resolve(item.url)
        val stream = info.streams.filter { it.kind == StreamKind.AUDIO }.maxByOrNull { it.bitrateKbps ?: 0 }
            ?: return
        // E1 : EnqueueResult.duplicate=true => doublon ignoré côté recherche (pas de forcer depuis la feuille).
        downloadManager.enqueue(
            info = info,
            platformId = item.id,
            sourceUrl = item.url,
            streamUrl = stream.url,
            format = stream.format.substringBefore(' ').take(10),
            isVideo = false,
            wifiOnly = false,
        )
    }
}
