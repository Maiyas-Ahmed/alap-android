package com.example.util

import com.example.data.model.AlapConstants
import com.example.data.model.RemoteVersionInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object WebsiteUpdateChecker {

    suspend fun fetchLatestVersion(): RemoteVersionInfo? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("${AlapConstants.UPDATE_VERSION_JSON_URL}?t=${System.currentTimeMillis()}")
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 7000
                readTimeout = 7000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Cache-Control", "no-cache")
            }
            if (connection.responseCode !in 200..299) return@withContext null
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            if (body.isBlank()) return@withContext null

            val json = JSONObject(body)
            val rawApkUrl = json.optString("apkUrl", "").trim()
            val resolvedApkUrl = when {
                rawApkUrl.startsWith("https://") || rawApkUrl.startsWith("http://") -> rawApkUrl
                rawApkUrl.startsWith("/") -> "${AlapConstants.UPDATE_PORTAL_URL}$rawApkUrl"
                rawApkUrl.isNotBlank() -> "${AlapConstants.UPDATE_PORTAL_URL}/$rawApkUrl"
                else -> ""
            }
            val changelogArr = json.optJSONArray("changelog")
            val changelogList = buildList {
                if (changelogArr != null) {
                    for (i in 0 until changelogArr.length()) {
                        val entry = changelogArr.optString(i, "").trim()
                        if (entry.isNotEmpty()) add(entry)
                    }
                }
            }
            RemoteVersionInfo(
                latestVersionName = json.optString("latestVersionName", json.optString("versionName", "")).trim(),
                latestVersionCode = json.optLong("latestVersionCode", json.optLong("versionCode", 0L)),
                forceUpdateRequired = json.optBoolean("forceUpdateRequired", json.optBoolean("forceUpdate", false)),
                apkUrl = resolvedApkUrl,
                releaseDate = json.optString("releaseDate", "").trim(),
                changelog = changelogList
            )
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    fun isVersionLower(current: String, minimum: String): Boolean {
        val cleanMin = minimum.trim().removePrefix("v")
        if (cleanMin.isBlank()) return false
        val curParts = current.trim().removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
        val minParts = cleanMin.split(".").map { it.toIntOrNull() ?: 0 }
        val maxLen = maxOf(curParts.size, minParts.size)
        for (i in 0 until maxLen) {
            val c = curParts.getOrElse(i) { 0 }
            val m = minParts.getOrElse(i) { 0 }
            if (c < m) return true
            if (c > m) return false
        }
        return false
    }

    fun maxVersionString(first: String, second: String): String {
        val a = first.trim()
        val b = second.trim()
        if (a.isBlank()) return b
        if (b.isBlank()) return a
        return if (isVersionLower(a, b)) b else a
    }
}
