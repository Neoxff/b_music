package com.bmusic.app.downloader.search

import android.content.Context
import coil3.ImageLoader
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.CachePolicy
import com.bmusic.app.downloader.security.ImageUrlPolicy
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Étape 6 : chargeur d'images dédié aux miniatures de résultats.
 * Ne modifie pas la configuration d'images globale de l'app.
 * HTTPS uniquement, anti-SSRF via ImageUrlPolicy, taille bornée (budgets Lean).
 */
object DownloaderCoilFactory {

    fun imageLoader(context: Context): ImageLoader {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(CoilGuardInterceptor())
            .build()
        return ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(client)) }
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25) // ~32 Mo mémoire bornée
                    .build()
            }
            .build()
    }

    private class CoilGuardInterceptor : okhttp3.Interceptor {
        override fun intercept(chain: okhttp3.Interceptor.Chain): okhttp3.Response {
            val url = chain.request().url.toString()
            if (!ImageUrlPolicy.isAllowedHost(chain.request().url.host)) {
                throw java.io.IOException("Blocked image host: ${chain.request().url.host}")
            }
            if (!url.startsWith("https://")) {
                throw java.io.IOException("Only https thumbnails allowed")
            }
            val response = chain.proceed(chain.request())
            if (response.body.contentLength() > ImageUrlPolicy.MAX_IMAGE_BYTES) {
                response.close()
                throw java.io.IOException("Image too large")
            }
            return response
        }
    }
}
