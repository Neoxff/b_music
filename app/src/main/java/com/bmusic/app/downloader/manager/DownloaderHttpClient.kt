package com.bmusic.app.downloader.manager

import okhttp3.CookieJar
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Step 1: dedicated HTTP client for the downloader module ONLY.
 *
 * - Not shared with Last.fm / lyrics / GitHub-update clients (see
 *   `data/remote/KtorClient.kt`): the downloader fetches untrusted,
 *   user-provided URLs, so its network surface is isolated.
 * - No cookies, no authentication, fixed neutral User-Agent.
 * - Timeouts: connect 10 s, read 30 s, write 30 s, whole call 120 s.
 * - Redirects are followed here; the full SSRF re-validation of every hop
 *   lands in step 6 (security hardening).
 */
class DownloaderHttpClient {
    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(120, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .cookieJar(CookieJar.NO_COOKIES)
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", USER_AGENT)
                    .build()
            )
        }
        .build()

    fun newRequest(url: String): Request = Request.Builder()
        .url(url)
        .get()
        .build()

    companion object {
        const val USER_AGENT = "B Music/1.1 (Android)"
    }
}
