package ru.caola.apla.data

import java.net.URI

/**
 * HTTP is allowed only in debug builds and only for loopback/private development hosts.
 * Public endpoints must use HTTPS, including in debug builds.
 */
object NetworkEndpointPolicy {
    fun isAllowed(value: String, debugBuild: Boolean): Boolean {
        return try {
            val uri = URI(value.trim())
            val scheme = uri.scheme?.lowercase() ?: return false
            val host = uri.host?.removePrefix("[")?.removeSuffix("]")?.lowercase() ?: return false
            if (uri.userInfo != null || uri.query != null || uri.fragment != null) return false
            if (!uri.path.isNullOrEmpty() && uri.path != "/") return false
            if (uri.port != -1 && uri.port !in 1..65535) return false
            when (scheme) {
                "https" -> true
                "http" -> debugBuild && isLocalDevelopmentHost(host)
                else -> false
            }
        } catch (_: Exception) {
            false
        }
    }

    fun isLocalDevelopmentHost(host: String): Boolean {
        if (host == "localhost" || host.endsWith(".localhost") || host == "::1") return true
        val parts = host.split('.')
        if (parts.size != 4) return false
        val octets = parts.map { it.toIntOrNull() ?: return false }
        if (octets.any { it !in 0..255 }) return false
        return when {
            octets[0] == 10 -> true
            octets[0] == 172 && octets[1] in 16..31 -> true
            octets[0] == 192 && octets[1] == 168 -> true
            octets[0] == 127 -> true
            else -> false
        }
    }
}
