package com.bmusic.app.downloader

import com.bmusic.app.downloader.data.DownloadDao
import com.bmusic.app.downloader.engine.MediaExtractorEngine
import com.bmusic.app.downloader.extractor.NewPipeExtractorEngine
import com.bmusic.app.downloader.manager.DownloadManager
import com.bmusic.app.downloader.manager.DownloaderHttpClient
import com.bmusic.app.downloader.transfer.DownloadTransferEngine
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Step 3 : Koin wiring for the downloader. Every binding is a `factory`
 * (lazy): nothing is instantiated at app startup, so cold-start is
 * unaffected. Registered in `appModules` (MainModule.kt); removing this
 * module + the `downloader/` package is the documented rollback.
 */
val downloaderModule = module {
    factory { DownloaderHttpClient() }
    factory<MediaExtractorEngine> { NewPipeExtractorEngine(get()) }
    factory { DownloadManager(get(), get(), get<DownloadDao>(), androidContext()) }
    factory { DownloadTransferEngine(androidContext(), get()) }
}
