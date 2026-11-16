package com.bmusic.app.downloader.manager

import com.bmusic.app.downloader.security.RedirectGuard
import com.bmusic.app.downloader.security.SsrfGuard
import okhttp3.CookieJar
import okhttp3.Dns
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Client HTTP **dédié au module téléchargeur**.
 *
 * - Non partagé avec les clients Last.fm / paroles / mise à jour GitHub
 *   (`data/remote/KtorClient.kt`) : le téléchargeur interroge des URL
 *   fournies par l'utilisateur, sa surface réseau est donc isolée.
 * - **Aucun cookie**, aucune authentification, User-Agent neutre et fixe.
 * - Timeouts : connect 10 s, read 30 s, write 30 s, appel 120 s.
 *
 * Durcissement (audit sécurité v3) :
 * - les redirections sont **pilotées par l'application** (`followRedirects(false)`)
 *   et **chaque saut est re-validé** par [RedirectGuard] : syntaxe, résolution
 *   DNS et plages IP interdites. Avant, OkHttp suivait les redirections
 *   sans aucune validation (SSRF possible via un `Location` vers
 *   `http://127.0.0.1/` ou une IP privée) ;
 * - le DNS est **épinglé** : la connexion utilise exactement les adresses
 *   validées par [GuardedDns], jamais une résolution ultérieure qui pourrait
 *   renvoyer une IP privée (rebinding DNS).
 */
class DownloaderHttpClient(
    private val resolver: SsrfGuard.HostResolver = SsrfGuard.HostResolver { host ->
        runCatching {
            InetAddress.getAllByName(host).map { it.hostAddress }
        }.getOrDefault(emptyList())
    },
) {
    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(120, TimeUnit.SECONDS)
        // Les redirections sont gérées par l'application (re-validation).
        .followRedirects(false)
        .followSslRedirects(false)
        .cookieJar(CookieJar.NO_COOKIES)
        .dns(GuardedDns(resolver))
        .addInterceptor { chain -> followWithGuardedRedirects(chain) }
        .build()

    /**
     * Suit les redirections en **re-validant chaque saut**.
     * Un `Location` vers une adresse privée est refusé (IOException).
     */
    private fun followWithGuardedRedirects(chain: Interceptor.Chain): Response {
        val original = chain.request()
        var request = original.newBuilder().header("User-Agent", USER_AGENT).build()
        var hops = 0
        while (true) {
            val response = chain.proceed(request)
            val location = response.headers["location"]
            if (!response.isRedirect || location == null) return response
            val next = try {
                RedirectGuard.nextUrl(response.request.url, location, hops, resolver)
            } catch (e: RedirectGuard.RedirectRefused) {
                response.close()
                throw IOException("redirect refused: ${e.message}", e)
            }
            response.close()
            request = original.newBuilder()
                .header("User-Agent", USER_AGENT)
                .url(next)
                .build()
            hops++
        }
    }

    fun newRequest(url: String): Request = Request.Builder()
        .url(url)
        .get()
        .build()

    /**
     * Résolution DNS **épinglée** : les adresses sont validées une fois
     * (interdiction des plages loopback / privées / link-local / CGNAT) puis
     * conservées : la connexion ne peut pas être redirigée vers une autre
     * adresse (rebinding DNS).
     */
    private class GuardedDns(private val resolver: SsrfGuard.HostResolver) : Dns {
        private val cache = ConcurrentHashMap<String, List<InetAddress>>()

        override fun lookup(hostname: String): List<InetAddress> =
            cache.computeIfAbsent(hostname) { host ->
                val addresses = resolver.resolve(host)
                if (addresses.isEmpty()) throw UnknownHostException(host)
                val blocked = addresses.firstOrNull { SsrfGuard.isBlockedAddress(it) }
                if (blocked != null) {
                    throw UnknownHostException("blocked address for $host")
                }
                addresses.map { InetAddress.getByName(it) }
            }
    }

    companion object {
        const val USER_AGENT = "B Music/1.1 (Android)"
    }
}
