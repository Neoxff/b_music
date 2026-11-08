package com.bmusic.app.downloader.manager

import com.bmusic.app.downloader.engine.MediaExtractorEngine
import com.bmusic.app.downloader.engine.MediaInfo

/**
 * Step 1 skeleton: single entry point for the downloader module.
 * WorkManager scheduling, queue and file transfer land in step 3;
 * nothing here is wired to playback, theme or animations.
 */
class DownloadManager(
    private val engine: MediaExtractorEngine,
    private val httpClient: DownloaderHttpClient
) {
    suspend fun probe(url: String): MediaInfo = engine.resolve(url)
}
