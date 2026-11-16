package com.bmusic.app.downloader.extractor

import com.bmusic.app.downloader.engine.ExtractorError
import com.bmusic.app.downloader.engine.MediaExtractorEngine
import com.bmusic.app.downloader.engine.MediaInfo
import com.bmusic.app.downloader.engine.MediaStream
import com.bmusic.app.downloader.engine.StreamKind
import com.bmusic.app.downloader.error.DownloadError
import com.bmusic.app.downloader.manager.DownloaderHttpClient
import com.bmusic.app.downloader.registry.SiteRegistry
import com.bmusic.app.downloader.registry.SiteStatus
import com.bmusic.app.downloader.security.UrlPolicy
import com.bmusic.app.downloader.security.UrlValidator
import com.bmusic.app.downloader.trace.LinkReduction
import com.bmusic.app.downloader.trace.TraceSession
import com.bmusic.app.downloader.trace.TraceStep
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

    override suspend fun resolve(url: String, trace: TraceSession?): MediaInfo = withContext(Dispatchers.IO) {
        // Défense en profondeur : le validateur s'applique ici aussi, même si
        // l'appelant (ViewModel) a déjà validé. Aucune URL non conforme
        // n'atteint le moteur.
        val validated = UrlValidator.validate(url)
        val cleanUrl = when (validated) {
            is UrlValidator.Validation.Invalid -> {
                val error = ExtractorError.UrlInvalid(validated.reason.name)
                trace?.fail(TraceStep.VALIDATION, DownloadError.fromExtractor(error))
                throw error
            }
            is UrlValidator.Validation.Valid -> validated.normalized
        }
        val domain = LinkReduction.domain(cleanUrl)
        if (!UrlPolicy.isSupportedScheme(cleanUrl)) {
            val error = ExtractorError.UrlInvalid(domain ?: "schema")
            trace?.fail(TraceStep.VALIDATION, DownloadError.fromExtractor(error))
            throw error
        }
        trace?.ok(TraceStep.VALIDATION, domain)
        ensureInit()
        trace?.ok(TraceStep.MOTEUR_INITIALISE)

        val site = SiteRegistry.lookup(cleanUrl)
        if (site == null || site.status == SiteStatus.NON_PRIS_EN_CHARGE) {
            val error = ExtractorError.Unsupported(site?.id ?: "unknown-site")
            trace?.fail(TraceStep.SITE_RECONNU, DownloadError.fromExtractor(error))
            throw error
        }

        val service = try {
            NewPipe.getServiceByUrl(cleanUrl)
        } catch (e: ExtractionException) {
            // Domaine connu mais forme d'URL refusée par NewPipe.
            val error = ExtractorError.Unsupported(site.id)
            trace?.fail(TraceStep.SITE_RECONNU, DownloadError.fromExtractor(error))
            throw error
        }
        // Nom du moteur retenu : lisible dans le journal (« YoutubeService »…).
        trace?.ok(TraceStep.SITE_RECONNU, service.javaClass.simpleName)

        val info = try {
            StreamInfo.getInfo(service, cleanUrl)
        } catch (e: ContentNotAvailableException) {
            // Private, deleted, region-locked or otherwise protected content.
            val error = ExtractorError.ProtectedContent()
            trace?.fail(TraceStep.EXTRACTION, DownloadError.fromExtractor(error))
            throw error
        } catch (e: ReCaptchaException) {
            val error = ExtractorError.SourceUnavailable("captcha")
            trace?.fail(TraceStep.EXTRACTION, DownloadError.fromExtractor(error))
            throw error
        } catch (e: StreamInfo.StreamExtractException) {
            val error = ExtractorError.SourceUnavailable("site changed")
            trace?.fail(TraceStep.EXTRACTION, DownloadError.fromExtractor(error))
            throw error
        } catch (e: ExtractionException) {
            val error = ExtractorError.SourceUnavailable("extraction failed")
            trace?.fail(TraceStep.EXTRACTION, DownloadError.fromExtractor(error))
            throw error
        } catch (e: IOException) {
            val error = ExtractorError.Network(e.message ?: "io")
            trace?.fail(TraceStep.EXTRACTION, DownloadError.fromExtractor(error))
            throw error
        }
        trace?.ok(TraceStep.EXTRACTION, info.name)

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
            val error = ExtractorError.SourceUnavailable("no downloadable streams found")
            trace?.fail(TraceStep.FLUX_OUVERT, DownloadError.fromExtractor(error))
            throw error
        }
        trace?.ok(TraceStep.FLUX_OUVERT, "${streams.size} flux")

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
