package com.bmusic.app.downloader

import com.bmusic.app.downloader.engine.MediaExtractorEngine
import com.bmusic.app.downloader.extractor.StubExtractorEngine
import com.bmusic.app.downloader.manager.DownloadManager
import com.bmusic.app.downloader.manager.DownloaderHttpClient
import org.koin.dsl.module

/**
 * Step 1: Koin wiring for the downloader. Every binding is a `factory`
 * (lazy): nothing is instantiated at app startup, so cold-start is
 * unaffected. Registered in `appModules` (MainModule.kt); removing this
 * module + the `downloader/` package is the documented rollback.
 */
val downloaderModule = module {
    factory { DownloaderHttpClient() }
    factory<MediaExtractorEngine> { StubExtractorEngine() }
    factory { DownloadManager(get(), get()) }
}
