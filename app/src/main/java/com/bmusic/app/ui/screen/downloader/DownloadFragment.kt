package com.bmusic.app.ui.screen.downloader

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import com.bmusic.app.ui.theme.BMusicTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

class DownloadFragment : Fragment() {
    private val vm: DownloadViewModel by viewModel()
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Pré-remplissage depuis ACTION_SEND — sans action automatique.
        arguments?.getString(EXTRA_PREFILL_URL)?.let { url ->
            vm.prefill(url, toLink = true)
        }
        arguments?.getString(EXTRA_PREFILL_QUERY)?.let { q ->
            // Texte libre : pré-rempli en Recherche, sans lancer la recherche (étape 6).
            vm.prefill(q, toLink = false)
        }
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { BMusicTheme { DownloadScreen(vm) } }
        }
    }
    companion object {
        const val EXTRA_PREFILL_URL = "download_prefill_url"
        const val EXTRA_PREFILL_QUERY = "download_prefill_query"
    }
}
