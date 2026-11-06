package com.bmusic.app.ui.screen.update

import com.bmusic.app.data.remote.github.model.GitHubRelease

data class UpdateSearchResult(
    val state: State = State.Idle,
    val data: GitHubRelease? = null,
    val executedAtMillis: Long = -1,
    val wasFromUser: Boolean = false,
    val wasExperimentalQuery: Boolean = true,
) {
    fun shouldStartNewSearchFor(fromUser: Boolean, allowExperimental: Boolean): Boolean {
        return when (state) {
            State.Idle -> true
            State.Completed, State.Failed -> fromUser || wasExperimentalQuery != allowExperimental
            State.Searching -> false
        }
    }

    enum class State {
        Idle, Searching, Completed, Failed
    }
}