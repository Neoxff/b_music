/*
 * Copyright (c) 2024 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.bmusic.app.ui.screen.library.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bmusic.app.R
import com.bmusic.app.core.model.shuffle.OpenShuffleMode
import com.bmusic.app.core.sort.SongSortMode
import com.bmusic.app.data.model.Album
import com.bmusic.app.data.model.Artist
import com.bmusic.app.data.model.ContentType
import com.bmusic.app.data.model.Song
import com.bmusic.app.extensions.navigation.albumDetailArgs
import com.bmusic.app.extensions.navigation.artistDetailArgs
import com.bmusic.app.extensions.navigation.detailArgs
import com.bmusic.app.extensions.navigation.playlistDetailArgs
import com.bmusic.app.extensions.requestView
import com.bmusic.app.extensions.topLevelTransition
import com.bmusic.app.ui.IScrollHelper
import com.bmusic.app.ui.component.base.AbsMainActivityFragment
import com.bmusic.app.ui.theme.BMusicTheme
import com.bmusic.app.util.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : AbsMainActivityFragment(), IScrollHelper {

    private var scrollToTopTrigger by mutableStateOf(false)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                BMusicTheme {
                    HomeScreen(
                        onActionClick = { type ->
                            findNavController().navigate(R.id.nav_detail_list, detailArgs(type))
                        },
                        onShuffleClick = {
                            libraryViewModel.allSongs().observe(viewLifecycleOwner) {
                                playerViewModel.openAndShuffleQueue(it)
                            }
                        },
                        onSongItemClick = { songs, index ->
                            playerViewModel.openQueue(songs, index, shuffleMode = OpenShuffleMode.Off)
                        },
                        onItemClick = { item ->
                            when (item) {
                                is Album -> {
                                    findNavController().navigate(
                                        R.id.nav_album_detail,
                                        albumDetailArgs(item.id),
                                        null
                                    )
                                }
                                is Artist -> {
                                    findNavController().navigate(
                                        R.id.nav_artist_detail,
                                        artistDetailArgs(item),
                                        null
                                    )
                                }
                                is Song -> {
                                    playerViewModel.openSongs(0, listOf(item), Preferences.songClickAction)
                                }
                            }
                        },
                        onPlayAlbumClick = { album ->
                            lifecycleScope.launch {
                                val albumSongs = withContext(Dispatchers.IO) {
                                    with(SongSortMode.AlbumSongs) {
                                        libraryViewModel.albumById(album.id).songs.sorted()
                                    }
                                }
                                playerViewModel.openQueue(albumSongs, shuffleMode = OpenShuffleMode.Off)
                            }
                        },
                        onOpenSuggestion = { suggestion ->
                            when (suggestion.type) {
                                ContentType.Favorites -> {
                                    libraryViewModel.favoritePlaylist().observe(viewLifecycleOwner) {
                                        findNavController().navigate(R.id.nav_playlist_detail, playlistDetailArgs(it.playListId))
                                    }
                                }

                                else -> {
                                    findNavController().navigate(R.id.nav_detail_list, detailArgs(suggestion.type))
                                }
                            }
                        },
                        onSearchClick = {
                            findNavController().navigate(R.id.nav_search)
                        },
                        onSettingsClick = {
                            findNavController().navigate(R.id.nav_settings)
                        },
                        scrollToTopTrigger = scrollToTopTrigger,
                        onScrollToTopDone = { scrollToTopTrigger = false },
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        topLevelTransition(view)
        checkForMargins(view)
    }

    override fun onResume() {
        super.onResume()
        requestView { checkForMargins(it) }
    }

    override fun scrollToTop() {
        scrollToTopTrigger = true
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {}

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean = false
}
