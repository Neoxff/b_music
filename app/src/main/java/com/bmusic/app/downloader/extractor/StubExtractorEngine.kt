package com.bmusic.app.downloader.extractor

import com.bmusic.app.downloader.engine.ExtractorError
import com.bmusic.app.downloader.engine.MediaExtractorEngine
import com.bmusic.app.downloader.engine.MediaInfo

/**
 * Step 1 placeholder. Replaced by the NewPipeExtractor-backed engine in
 * step 2; until then every resolution fails cleanly with [ExtractorError].
 */
class StubExtractorEngine : MediaExtractorEngine {
    override suspend fun resolve(url: String): MediaInfo {
        throw ExtractorError.Unsupported("extraction engine not installed yet")
    }
}
