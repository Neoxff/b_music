package com.bmusic.app.downloader.extractor

import com.bmusic.app.downloader.engine.ExtractorError
import com.bmusic.app.downloader.engine.MediaExtractorEngine
import com.bmusic.app.downloader.engine.MediaInfo
import com.bmusic.app.downloader.error.DownloadError
import com.bmusic.app.downloader.trace.TraceSession
import com.bmusic.app.downloader.trace.TraceStep

/**
 * Step 1 placeholder. Replaced by the NewPipeExtractor-backed engine in
 * step 2; until then every resolution fails cleanly with [ExtractorError].
 *
 * The failure is also reported to the step journal, so a diagnostic run shows
 * exactly where the attempt stopped.
 */
class StubExtractorEngine : MediaExtractorEngine {
    override suspend fun resolve(url: String, trace: TraceSession?): MediaInfo {
        val error = ExtractorError.Unsupported("extraction engine not installed yet")
        trace?.fail(TraceStep.EXTRACTION, DownloadError.fromExtractor(error))
        throw error
    }
}
