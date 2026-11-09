package com.bmusic.app.downloader.engine

/**
 * Step 1 (isolated groundwork): extraction-engine contract for the media
 * downloader (v1.1). The real engine (NewPipeExtractor) lands in step 2
 * behind this interface, so the rest of the module never depends on a
 * concrete extractor. No implementation runs at startup: instances are
 * created lazily through Koin `factory` bindings.
 */
interface MediaExtractorEngine {
    /**
     * Resolves a user-provided URL into metadata and downloadable streams.
     * @throws ExtractorError on any failure (invalid URL, unsupported source,
     * protected/private content, source unavailable, network).
     */
    suspend fun resolve(url: String): MediaInfo

    /**
     * YouTube search (étape 6). Default stub throws Unsupported on engines
     * that do not support search; [com.bmusic.app.downloader.extractor.NewPipeExtractorEngine]
     * overrides with the real implementation.
     */
    suspend fun search(
        query: String,
        filter: SearchFilter = SearchFilter.ALL,
        pageIndex: Int = 0,
    ): SearchPage = throw ExtractorError.Unsupported("search not supported")
}

enum class SearchFilter { ALL, MUSIC }

data class SearchResultItem(
    val id: String,
    val url: String,
    val title: String,
    val channel: String,
    val durationMs: Long,
    val thumbnailUrl: String?,
    val viewCount: Long? = null,
)

data class SearchPage(
    val items: List<SearchResultItem>,
    val nextPageIndex: Int? = null,
)

data class MediaInfo(
    val title: String,
    val author: String,
    val durationMs: Long,
    val artworkUrl: String?,
    val streams: List<MediaStream>
)

data class MediaStream(
    val kind: StreamKind,
    /** Container/codec label as reported by the source (e.g. "m4a", "opus", "mp4"). */
    val format: String,
    val bitrateKbps: Int? = null,
    val sizeBytes: Long? = null,
    val url: String
)

enum class StreamKind { AUDIO, VIDEO }

/** Typed failures surfaced to the user as clear, actionable messages. */
sealed class ExtractorError(message: String) : Exception(message) {
    class UrlInvalid(cause: String) : ExtractorError("Invalid URL: $cause")
    class Unsupported(source: String) : ExtractorError("Unsupported source: $source")
    class ProtectedContent : ExtractorError("Content is protected or private")
    class SourceUnavailable(reason: String) : ExtractorError("Source unavailable: $reason")
    class Network(cause: String) : ExtractorError("Network error: $cause")
    class QueryInvalid(reason: String) : ExtractorError("Invalid query: $reason")
}
