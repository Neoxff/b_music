package com.bmusic.app.downloader.extractor

import com.bmusic.app.downloader.engine.ExtractorError
import com.bmusic.app.downloader.engine.MediaExtractorEngine
import com.bmusic.app.downloader.engine.MediaInfo
import com.bmusic.app.downloader.engine.MediaStream
import com.bmusic.app.downloader.engine.StreamKind
import com.bmusic.app.downloader.manager.DownloaderHttpClient
import com.bmusic.app.downloader.security.UrlPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import org.schabi.newpipe.extractor.stream.StreamInfo
import java.io.IOException

/**
 * Step 2: NewPipeExtractor-backed [MediaExtractorEngine].
 *
 * - Lazy init: [NewPipe.init] runs on first [resolve] call only, never at
 *   app startup (cold-start unaffected; verified by trace in step 2 QA).
 * - HTTP goes through the dedicated [DownloaderHttpClient] from step 1 —
 *   never the Last.fm / lyrics / GitHub-update clients.
 * - Supported services are those registered in NewPipeExtractor (YouTube,
 *   SoundCloud, Bandcamp, Vimeo, PeerTube, mediaCCC). Instagram, TikTok and
 *   X are not registered: [NewPipe.getServiceByUrl] fails and the caller
 *   gets [ExtractorError.Unsupported] — nothing is promised for them.
 */
class NewPipeExtractorEngine(
    private val httpClient: DownloaderHttpClient
) : MediaExtractorEngine {

    private val initLock = Any()

    @Volatile
    private var initialized = false

    private fun ensureInit() {
        if (initialized) return
        synchronized(initLock) {
            if (!initialized) {
                NewPipe.init(NewPipeDownloader(httpClient))
                initialized = true
            }
        }
    }

    override suspend fun resolve(url: String): MediaInfo = withContext(Dispatchers.IO) {
        if (!UrlPolicy.isSupportedScheme(url)) {
            throw ExtractorError.UrlInvalid(url)
        }
        ensureInit()

        val service = try {
            NewPipe.getServiceByUrl(url)
        } catch (e: ExtractionException) {
            throw ExtractorError.Unsupported(url)
        }

        val info = try {
            StreamInfo.getInfo(service, url)
        } catch (e: ContentNotAvailableException) {
            // Private, deleted, region-locked or otherwise protected content.
            throw ExtractorError.ProtectedContent()
        } catch (e: ReCaptchaException) {
            throw ExtractorError.SourceUnavailable(
                "source temporarily unavailable, the engine may need an update"
            )
        } catch (e: StreamInfo.StreamExtractException) {
            throw ExtractorError.SourceUnavailable(
                "source temporarily unavailable, the engine may need an update"
            )
        } catch (e: ExtractionException) {
            throw ExtractorError.SourceUnavailable(
                "source temporarily unavailable, the engine may need an update"
            )
        } catch (e: IOException) {
            throw ExtractorError.Network(e.message ?: "I/O error")
        }

        val audioStreams = info.audioStreams.mapNotNull { stream ->
            val streamUrl = stream.url ?: stream.content ?: return@mapNotNull null
            MediaStream(
                kind = StreamKind.AUDIO,
                format = stream.format?.suffix ?: "audio",
                bitrateKbps = stream.averageBitrate.takeIf { it > 0 },
                url = streamUrl
            )
        }.sortedByDescending { it.bitrateKbps ?: 0 }

        val videoStreams = (info.videoStreams + info.videoOnlyStreams).mapNotNull { stream ->
            val streamUrl = stream.url ?: stream.content ?: return@mapNotNull null
            MediaStream(
                kind = StreamKind.VIDEO,
                format = "${stream.format?.suffix ?: "video"} ${stream.resolution}",
                bitrateKbps = stream.bitrate.takeIf { it > 0 },
                url = streamUrl
            )
        }.sortedByDescending { it.bitrateKbps ?: 0 }

        val streams = audioStreams + videoStreams
        if (streams.isEmpty()) {
            throw ExtractorError.SourceUnavailable("no downloadable streams found")
        }

        MediaInfo(
            title = info.name ?: "Unknown title",
            author = info.uploaderName ?: "Unknown author",
            durationMs = info.duration * 1000L,
            artworkUrl = info.thumbnails.firstOrNull()?.url,
            streams = streams
        )
    }

    /** Bridges NewPipe's HTTP layer to the downloader's dedicated client. */
    private class NewPipeDownloader(
        private val downloaderClient: DownloaderHttpClient
    ) : Downloader() {
        override fun execute(request: Request): Response {
            val body = request.dataToSend()?.toRequestBody()
            val builder = okhttp3.Request.Builder()
                .url(request.url())
                .method(request.httpMethod(), body)
            request.headers()?.forEach { (name, values) ->
                values.forEach { value -> builder.addHeader(name, value) }
            }
            downloaderClient.client.newCall(builder.build()).execute().use { response ->
                return Response(
                    response.code,
                    response.message,
                    response.headers.toMultimap(),
                    response.body?.string(),
                    response.request.url.toString()
                )
            }
        }
    }
}
