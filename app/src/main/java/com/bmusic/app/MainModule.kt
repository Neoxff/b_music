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

package com.bmusic.app

import androidx.preference.PreferenceManager
import androidx.room.Room
import com.bmusic.app.coil.CustomArtistImageManager
import com.bmusic.app.coil.CustomPlaylistImageManager
import com.bmusic.app.core.BMusicDatabase
import com.bmusic.app.core.MediaEventBus
import com.bmusic.app.core.audio.AudioOutputObserver
import com.bmusic.app.data.local.EditTarget
import com.bmusic.app.data.local.MediaStoreWriter
import com.bmusic.app.data.local.backup.BackupManager
import com.bmusic.app.data.model.Genre
import com.bmusic.app.data.remote.deezer.DeezerService
import com.bmusic.app.data.remote.github.GitHubService
import com.bmusic.app.data.remote.jsonHttpClient
import com.bmusic.app.data.remote.lastfm.LastFmService
import com.bmusic.app.data.remote.listenbrainz.ListenBrainzService
import com.bmusic.app.data.remote.lyrics.LyricsDownloadService
import com.bmusic.app.data.remote.musicbrainz.MusicBrainzService
import com.bmusic.app.data.remote.provideOkHttp
import com.bmusic.app.data.repository.AlbumRepository
import com.bmusic.app.data.repository.ArtistRepository
import com.bmusic.app.data.repository.AutoEqRepository
import com.bmusic.app.data.repository.GenreRepository
import com.bmusic.app.data.repository.LyricsRepository
import com.bmusic.app.data.repository.NetworkRepository
import com.bmusic.app.data.repository.NetworkRepositoryImpl
import com.bmusic.app.data.repository.PlaylistRepository
import com.bmusic.app.data.repository.RealAlbumRepository
import com.bmusic.app.data.repository.RealArtistRepository
import com.bmusic.app.data.repository.RealAutoEqRepository
import com.bmusic.app.data.repository.RealGenreRepository
import com.bmusic.app.data.repository.RealLyricsRepository
import com.bmusic.app.data.repository.RealPlaylistRepository
import com.bmusic.app.data.repository.RealRepository
import com.bmusic.app.data.repository.RealSearchRepository
import com.bmusic.app.data.repository.RealSmartRepository
import com.bmusic.app.data.repository.RealSongRepository
import com.bmusic.app.data.repository.RealSpecialRepository
import com.bmusic.app.data.repository.Repository
import com.bmusic.app.data.repository.SearchRepository
import com.bmusic.app.data.repository.SmartRepository
import com.bmusic.app.data.repository.SongRepository
import com.bmusic.app.data.repository.SpecialRepository
import com.bmusic.app.playback.QueueStateHolder
import com.bmusic.app.playback.SleepTimer
import com.bmusic.app.playback.equalizer.EqualizerManager
import com.bmusic.app.playback.processor.BalanceAudioProcessor
import com.bmusic.app.playback.processor.ReplayGainAudioProcessor
import com.bmusic.app.ui.screen.backup.BackupViewModel
import com.bmusic.app.ui.screen.equalizer.EqualizerViewModel
import com.bmusic.app.ui.screen.info.InfoViewModel
import com.bmusic.app.ui.screen.library.LibraryViewModel
import com.bmusic.app.ui.screen.library.albums.AlbumDetailViewModel
import com.bmusic.app.ui.screen.library.artists.ArtistDetailViewModel
import com.bmusic.app.ui.screen.library.folders.FolderDetailViewModel
import com.bmusic.app.ui.screen.library.genres.GenreDetailViewModel
import com.bmusic.app.ui.screen.library.home.HomeViewModel
import com.bmusic.app.ui.screen.library.playlists.PlaylistDetailViewModel
import com.bmusic.app.ui.screen.library.search.SearchViewModel
import com.bmusic.app.ui.screen.library.years.YearDetailViewModel
import com.bmusic.app.ui.screen.lyrics.LyricsViewModel
import com.bmusic.app.ui.screen.onboard.OnboardViewModel
import com.bmusic.app.ui.screen.player.PlayerViewModel
import com.bmusic.app.ui.screen.sleeptimer.SleepTimerViewModel
import com.bmusic.app.ui.screen.tageditor.TagEditorViewModel
import com.bmusic.app.ui.screen.update.UpdateViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module

val networkModule = module {
    factory { jsonHttpClient(okHttpClient = get()) }
    factory { provideOkHttp(context = get()) }
    single { GitHubService(context = androidContext(), client = get()) }
    single { DeezerService(client = get()) }
    single { LastFmService(client = get()) }
    single { ListenBrainzService(client = get()) }
    single { MusicBrainzService(client = get()) }
    single { LyricsDownloadService(client = get()) }
    single { QueueStateHolder() }
}

private val mainModule = module {
    single {
        androidContext().contentResolver
    }
    single {
        PreferenceManager.getDefaultSharedPreferences(androidContext())
    }
    single {
        SleepTimer()
    }
    single {
        BalanceAudioProcessor()
    }
    single {
        ReplayGainAudioProcessor()
    }
    single {
        EqualizerManager(
            context = androidContext(),
            balanceProcessor = get(),
            replayGainProcessor = get(),
            eqRepository = get(),
            audioOutputObserver = get()
        )
    }
    single {
        MediaStoreWriter(contentResolver = get())
    }
    single {
        CustomArtistImageManager(context = androidContext())
    }
    single {
        CustomPlaylistImageManager(context = androidContext())
    }
    single {
        AudioOutputObserver(context = androidContext())
    }
    single {
        MediaEventBus()
    }
    factory {
        BackupManager(
            context = androidContext(),
            repository = get(),
            playCountDao = get(),
            lyricsDao = get(),
            customArtistImageManager = get()
        )
    }
}

private val roomModule = module {
    single {
        Room.databaseBuilder(androidContext(), BMusicDatabase::class.java, "music_database.db")
            .addMigrations(
                BMusicDatabase.MIGRATION_1_2,
                BMusicDatabase.MIGRATION_2_3,
                BMusicDatabase.MIGRATION_3_4,
                BMusicDatabase.MIGRATION_4_5,
                BMusicDatabase.MIGRATION_5_6,
                BMusicDatabase.MIGRATION_6_7,
                BMusicDatabase.MIGRATION_7_8,
                BMusicDatabase.MIGRATION_8_9
            )
            .build()
    }

    factory {
        get<BMusicDatabase>().playlistDao()
    }

    factory {
        get<BMusicDatabase>().playCountDao()
    }

    factory {
        get<BMusicDatabase>().historyDao()
    }

    factory {
        get<BMusicDatabase>().queueDao()
    }

    factory {
        get<BMusicDatabase>().inclExclDao()
    }

    factory {
        get<BMusicDatabase>().downloadDao()
    }

    factory {
        get<BMusicDatabase>().lyricsDao()
    }

    factory {
        get<BMusicDatabase>().autoEqDao()
    }
}

private val dataModule = module {
    single {
        RealRepository(
            context = androidContext(),
            songRepository = get(),
            albumRepository = get(),
            artistRepository = get(),
            genreRepository = get(),
            smartRepository = get(),
            specialRepository = get(),
            playlistRepository = get(),
            searchRepository = get(),
            networkRepository = get()
        )
    } bind Repository::class

    single {
        RealSongRepository(context = get(), inclExclDao = get())
    } bind SongRepository::class

    single {
        RealAlbumRepository(songRepository = get())
    } bind AlbumRepository::class

    single {
        RealArtistRepository(songRepository = get(), albumRepository = get())
    } bind ArtistRepository::class

    single {
        RealPlaylistRepository(
            context = androidContext(),
            songRepository = get(),
            playlistDao = get()
        )
    } bind PlaylistRepository::class

    single {
        RealGenreRepository(contentResolver = get(), songRepository = get())
    } bind GenreRepository::class

    single {
        RealSearchRepository(
            albumRepository = get(),
            songRepository = get(),
            artistRepository = get(),
            playlistRepository = get(),
            genreRepository = get(),
            specialRepository = get()
        )
    } bind SearchRepository::class

    single {
        RealSmartRepository(
            context = androidContext(),
            songRepository = get(),
            albumRepository = get(),
            artistRepository = get(),
            historyDao = get(),
            playCountDao = get()
        )
    } bind SmartRepository::class

    single {
        RealSpecialRepository(songRepository = get())
    } bind SpecialRepository::class

    single {
        RealLyricsRepository(
            context = androidContext(),
            preferences = get(),
            lyricsDownloadService = get(),
            lyricsDao = get()
        )
    } bind LyricsRepository::class

    single {
        NetworkRepositoryImpl(
            context = androidContext(),
            preferences = get(),
            lastFmService = get(),
            listenBrainzService = get(),
            deezerService = get(),
            musicBrainzService = get()
        )
    } bind NetworkRepository::class

    single {
        RealAutoEqRepository(gitHubService = get(), autoEqDao = get())
    } bind AutoEqRepository::class
}

private val viewModule = module {
    viewModel {
        LibraryViewModel(repository = get(), inclExclDao = get(), customPlaylistImageManager = get())
    }

    viewModel {
        com.bmusic.app.ui.screen.downloader.DownloadViewModel(
            engine = get(),
            downloadManager = get(),
            dao = get(),
            prefs = get(),
        )
    }

    viewModel {
        HomeViewModel(repository = get(), eventBus = get())
    }

    viewModel {
        PlayerViewModel(preferences = get(), queueStateHolder = get(), repository = get())
    }

    viewModel {
        EqualizerViewModel(
            equalizerManager = get(),
            audioOutputObserver = get(),
            mediaStoreWriter = get()
        )
    }

    viewModel {
        SleepTimerViewModel(
            application = androidApplication(),
            sleepTimer = get()
        )
    }

    viewModel { (albumId: Long) ->
        AlbumDetailViewModel(
            application = androidApplication(),
            repository = get(),
            albumId = albumId
        )
    }

    viewModel { (artistId: Long, artistName: String?) ->
        ArtistDetailViewModel(
            application = androidApplication(),
            repository = get(),
            artistId = artistId,
            artistName = artistName
        )
    }

    viewModel { (playlistId: Long) ->
        PlaylistDetailViewModel(playlistRepository = get(), playlistId = playlistId)
    }

    viewModel { (genre: Genre) ->
        GenreDetailViewModel(repository = get(), genre = genre)
    }

    viewModel { (year: Int) ->
        YearDetailViewModel(repository = get(), year = year)
    }

    viewModel { (path: String) ->
        FolderDetailViewModel(repository = get(), folderPath = path)
    }

    viewModel {
        SearchViewModel(repository = get())
    }

    viewModel { (target: EditTarget) ->
        TagEditorViewModel(
            repository = get(),
            mediaEventBus = get(),
            customArtistImageManager = get(),
            target = target
        )
    }

    viewModel {
        LyricsViewModel(application = androidApplication(), preferences = get(), repository = get())
    }

    viewModel {
        InfoViewModel(repository = get())
    }

    viewModel {
        UpdateViewModel(updateService = get())
    }

    viewModel {
        BackupViewModel(contentResolver = get(), preferences = get(), backupManager = get())
    }

    viewModel {
        OnboardViewModel(backupManager = get())
    }
}

val appModules = listOf(
    networkModule,
    mainModule,
    roomModule,
    dataModule,
    viewModule,
    com.bmusic.app.downloader.downloaderModule
)