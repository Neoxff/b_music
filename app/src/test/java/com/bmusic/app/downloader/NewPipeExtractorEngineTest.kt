package com.bmusic.app.downloader

import com.bmusic.app.downloader.engine.ExtractorError
import com.bmusic.app.downloader.extractor.NewPipeExtractorEngine
import com.bmusic.app.downloader.manager.DownloaderHttpClient
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Step 2 offline tests: invalid/unsupported URLs fail with clear typed
 * errors without touching the network (service resolution is local).
 * Real YouTube/SoundCloud resolution is covered by the optional manual
 * integration test (needs network + device).
 */
class NewPipeExtractorEngineTest {

    private val engine = NewPipeExtractorEngine(DownloaderHttpClient())

    @Test
    fun resolve_nonHttpUrl_throwsUrlInvalid() = runBlocking {
        try {
            engine.resolve("ftp://example.com/file.mp3")
            fail("expected UrlInvalid")
        } catch (e: ExtractorError.UrlInvalid) {
            assertTrue(e.message!!.isNotEmpty())
        }
    }

    @Test
    fun resolve_unsupportedHost_throwsUnsupported() = runBlocking {
        try {
            engine.resolve("https://example.com/watch?v=1")
            fail("expected Unsupported")
        } catch (e: ExtractorError.Unsupported) {
            assertTrue(e.message!!.isNotEmpty())
        }
    }
}
