package com.bmusic.app.ui.screen.update

import android.app.DownloadManager
import android.content.Context
import android.util.Log
import androidx.core.content.getSystemService
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.bmusic.app.core.model.task.Event
import com.bmusic.app.data.remote.github.GitHubService
import com.bmusic.app.data.remote.github.model.GitHubRelease
import com.bmusic.app.util.Preferences
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date

class UpdateViewModel(private val updateService: GitHubService): ViewModel() {

    private val ioHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e("UpdateViewModel", "Update search failed!", throwable)
    }

    private val _updateSearch = MutableLiveData(UpdateSearchResult())
    val updateEventObservable = _updateSearch.map { Event(it) }
    val updateEvent get() = updateEventObservable.value

    val latestRelease get() = updateEvent?.peekContent()?.data

    fun searchForUpdate(fromUser: Boolean, allowExperimental: Boolean = Preferences.experimentalUpdates) =
        viewModelScope.launch(Dispatchers.IO) {
            val current = updateEvent?.peekContent() ?: UpdateSearchResult(executedAtMillis = Preferences.lastUpdateSearch)
            if (current.shouldStartNewSearchFor(fromUser, allowExperimental)) {
                _updateSearch.postValue(
                    current.copy(
                        state = UpdateSearchResult.State.Searching,
                        wasFromUser = fromUser,
                        wasExperimentalQuery = allowExperimental
                    )
                )

                val result = runCatching {
                    updateService.latestRelease(allowExperimental = allowExperimental)
                }
                val executedAtMillis = Date().time.also {
                    Preferences.lastUpdateSearch = it
                }
                val newState = if (result.isSuccess) {
                    UpdateSearchResult(
                        state = UpdateSearchResult.State.Completed,
                        data = result.getOrThrow(),
                        executedAtMillis = executedAtMillis,
                        wasFromUser = fromUser,
                        wasExperimentalQuery = allowExperimental
                    )
                } else {
                    UpdateSearchResult(
                        state = UpdateSearchResult.State.Failed,
                        data = null,
                        executedAtMillis = executedAtMillis,
                        wasFromUser = fromUser,
                        wasExperimentalQuery = allowExperimental
                    )
                }
                _updateSearch.postValue(newState)
            }
        }

    fun downloadUpdate(context: Context, release: GitHubRelease) =
        viewModelScope.launch(Dispatchers.IO + ioHandler) {
            val downloadRequest = release.getDownloadRequest(context)
            if (downloadRequest != null) {
                val downloadManager = context.getSystemService<DownloadManager>()
                if (downloadManager != null) {
                    val lastUpdateId = Preferences.lastUpdateId
                    if (lastUpdateId != -1L) {
                        downloadManager.remove(lastUpdateId)
                    }
                    Preferences.lastUpdateId = downloadManager.enqueue(downloadRequest)
                }
            }
        }
}