package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

sealed interface UpdateDownloadState {
    data object Idle : UpdateDownloadState
    data object NeedsInstallPermission : UpdateDownloadState
    data class Downloading(val progressPercent: Int) : UpdateDownloadState
    data class ReadyToInstall(val apkFile: File) : UpdateDownloadState
    data class Error(val message: String, val fallbackUrl: String = "") : UpdateDownloadState
}

object ApkUpdateManager {
    private val _state = MutableStateFlow<UpdateDownloadState>(UpdateDownloadState.Idle)
    val state: StateFlow<UpdateDownloadState> = _state.asStateFlow()

    private val downloadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var downloadJob: Job? = null
    private var activeTargetFile: File? = null

    fun canRequestPackageInstalls(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                // Fallback to general security/app settings if OEM blocks direct intent
                val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
            }
        }
    }

    fun openInBrowserFallback(context: Context, url: String) {
        val clean = normalizeDownloadUrl(url)
        if (clean.isBlank()) return
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(clean)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    fun resetState() {
        downloadJob?.cancel()
        _state.value = UpdateDownloadState.Idle
    }

    fun onHostResumed(context: Context) {
        val currentState = _state.value
        val file = activeTargetFile
        if (currentState is UpdateDownloadState.NeedsInstallPermission &&
            file != null &&
            file.exists() &&
            file.length() > 0L &&
            canRequestPackageInstalls(context)
        ) {
            _state.value = UpdateDownloadState.ReadyToInstall(file)
            installApkFile(context, file)
        }
    }

    fun normalizeDownloadUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank()) return ""
        // Auto-convert GitHub blob URLs to raw download URLs
        if (trimmed.contains("github.com/") && trimmed.contains("/blob/")) {
            return trimmed.replaceFirst("/blob/", "/raw/")
        }
        return trimmed
    }

    fun startUpdateDownload(
        context: Context,
        apkUrl: String,
        versionTag: String
    ) {
        val cleanUrl = normalizeDownloadUrl(apkUrl)
        if (cleanUrl.isBlank() || (!cleanUrl.startsWith("https://") && !cleanUrl.startsWith("http://"))) {
            _state.value = UpdateDownloadState.Error("অ্যাডমিন প্যানেল বা আপডেট পোর্টালে সঠিক APK লিংক দেওয়া হয়নি।")
            return
        }

        val appContext = context.applicationContext
        downloadJob?.cancel()
        _state.value = UpdateDownloadState.Downloading(0)

        downloadJob = downloadScope.launch {
            var connection: HttpURLConnection? = null
            try {
                val updatesDir = File(appContext.filesDir, "updates").apply { mkdirs() }
                val partFile = File(updatesDir, "alap-update-${versionTag.ifBlank { "latest" }}.apk.part")
                val targetFile = File(updatesDir, "alap-update.apk")
                if (partFile.exists()) partFile.delete()

                var currentUrl = cleanUrl
                var redirects = 0
                while (redirects <= 10) {
                    val conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                        instanceFollowRedirects = false
                        requestMethod = "GET"
                        connectTimeout = 15_000
                        readTimeout = 30_000
                        setRequestProperty("User-Agent", "AlapAndroidUpdater/1.0.1")
                        setRequestProperty("Accept", "application/vnd.android.package-archive, application/octet-stream, */*")
                        setRequestProperty("Accept-Encoding", "identity")
                    }
                    val code = conn.responseCode
                    if (code in listOf(301, 302, 303, 307, 308)) {
                        val location = conn.getHeaderField("Location")?.trim()
                        conn.disconnect()
                        if (location.isNullOrBlank()) {
                            _state.value = UpdateDownloadState.Error(
                                message = "সার্ভার রিডাইরেক্ট লিংক খালি (HTTP $code)।",
                                fallbackUrl = cleanUrl
                            )
                            return@launch
                        }
                        currentUrl = URL(URL(currentUrl), location).toString()
                        redirects++
                    } else {
                        connection = conn
                        break
                    }
                }

                val activeConn = connection ?: run {
                    _state.value = UpdateDownloadState.Error(
                        message = "অনেকগুলো রিডাইরেক্টের কারণে ডাউনলোড ব্যর্থ হয়েছে।",
                        fallbackUrl = cleanUrl
                    )
                    return@launch
                }

                val statusCode = activeConn.responseCode
                if (statusCode !in 200..299) {
                    _state.value = UpdateDownloadState.Error(
                        message = "APK ডাউনলোড ব্যর্থ (HTTP $statusCode)। GitHub বা Admin-এ APK লিংকটি ঠিক আছে কি না দেখুন।",
                        fallbackUrl = cleanUrl
                    )
                    return@launch
                }

                val contentType = activeConn.contentType?.lowercase().orEmpty()
                if (contentType.contains("text/html")) {
                    _state.value = UpdateDownloadState.Error(
                        message = "এই লিংকটি সরাসরি APK ফাইল নয় (একটি ওয়েবপেজ)। GitHub Release-এর .apk ফাইলের ডাইরেক্ট লিংক বসান।",
                        fallbackUrl = cleanUrl
                    )
                    return@launch
                }

                val totalBytes = activeConn.contentLengthLong
                val buffer = ByteArray(16 * 1024)
                var downloadedBytes = 0L
                var lastReportedPct = -1

                activeConn.inputStream.use { input ->
                    val firstRead = input.read(buffer)
                    if (firstRead <= 0) {
                        _state.value = UpdateDownloadState.Error(
                            message = "সার্ভার থেকে পাওয়া ফাইলটি খালি (0 bytes)।",
                            fallbackUrl = cleanUrl
                        )
                        return@launch
                    }

                    // Check if server returned HTML instead of a binary APK (ZIP starts with 'PK' = 0x50 0x4B)
                    val headerPreview = String(buffer, 0, minOf(firstRead, 64), Charsets.UTF_8).trimStart().lowercase()
                    if (headerPreview.startsWith("<!doc") || headerPreview.startsWith("<html") || headerPreview.startsWith("<head")) {
                        _state.value = UpdateDownloadState.Error(
                            message = "লিংকটি সরাসরি APK নয়, বরং একটি HTML পেজ। অনুগ্রহ করে .apk ফাইলের ডাইরেক্ট লিংক দিন।",
                            fallbackUrl = cleanUrl
                        )
                        return@launch
                    }
                    if (firstRead < 2 || buffer[0] != 0x50.toByte() || buffer[1] != 0x4B.toByte()) {
                        _state.value = UpdateDownloadState.Error(
                            message = "ডাউনলোড হওয়া ফাইলটি বৈধ Android APK প্যাকেজ নয়।",
                            fallbackUrl = cleanUrl
                        )
                        return@launch
                    }

                    FileOutputStream(partFile).use { output ->
                        output.write(buffer, 0, firstRead)
                        downloadedBytes += firstRead
                        if (totalBytes > 0L) {
                            val pct = ((downloadedBytes * 100L) / totalBytes).toInt().coerceIn(0, 99)
                            if (pct != lastReportedPct) {
                                lastReportedPct = pct
                                _state.value = UpdateDownloadState.Downloading(pct)
                            }
                        }

                        while (isActive) {
                            val count = input.read(buffer)
                            if (count == -1) break
                            output.write(buffer, 0, count)
                            downloadedBytes += count
                            if (totalBytes > 0L) {
                                val pct = ((downloadedBytes * 100L) / totalBytes).toInt().coerceIn(0, 99)
                                if (pct != lastReportedPct) {
                                    lastReportedPct = pct
                                    _state.value = UpdateDownloadState.Downloading(pct)
                                }
                            } else {
                                // Smooth indeterminate progress feel if server omits Content-Length
                                val kbPct = ((downloadedBytes / 102_400L).toInt()).coerceIn(5, 95)
                                if (kbPct != lastReportedPct) {
                                    lastReportedPct = kbPct
                                    _state.value = UpdateDownloadState.Downloading(kbPct)
                                }
                            }
                        }
                        output.flush()
                    }
                }

                if (!isActive) return@launch

                if (!partFile.exists() || partFile.length() <= 0L) {
                    _state.value = UpdateDownloadState.Error(
                        message = "ডাউনলোড করা APK ফাইলটি খালি বা সেভ হয়নি।",
                        fallbackUrl = cleanUrl
                    )
                    return@launch
                }

                if (targetFile.exists()) targetFile.delete()
                if (!partFile.renameTo(targetFile)) {
                    partFile.copyTo(targetFile, overwrite = true)
                    partFile.delete()
                }

                activeTargetFile = targetFile
                handleDownloadSuccess(appContext, targetFile)
            } catch (e: Exception) {
                _state.value = UpdateDownloadState.Error(
                    message = "ডাউনলোড সমস্যা: ${e.localizedMessage ?: "নেটওয়ার্ক ত্রুটি"}",
                    fallbackUrl = cleanUrl
                )
            } finally {
                connection?.disconnect()
            }
        }
    }

    private fun handleDownloadSuccess(appContext: Context, file: File) {
        if (!file.exists() || file.length() <= 0L) {
            _state.value = UpdateDownloadState.Error("ডাউনলোড করা APK ফাইলটি পাওয়া যায়নি বা খালি।")
            return
        }
        if (!canRequestPackageInstalls(appContext)) {
            _state.value = UpdateDownloadState.NeedsInstallPermission
            openInstallPermissionSettings(appContext)
        } else {
            _state.value = UpdateDownloadState.ReadyToInstall(file)
            installApkFile(appContext, file)
        }
    }

    fun installApkFile(context: Context, file: File) {
        if (!file.exists() || file.length() <= 0L) {
            _state.value = UpdateDownloadState.Error("APK ফাইলটি ফোনে খুঁজে পাওয়া যায়নি। অনুগ্রহ করে আবার ডাউনলোড করুন।")
            return
        }
        if (!canRequestPackageInstalls(context)) {
            _state.value = UpdateDownloadState.NeedsInstallPermission
            openInstallPermissionSettings(context)
            return
        }
        try {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            _state.value = UpdateDownloadState.Error("ইনস্টলার চালু করা যায়নি: ${e.localizedMessage}")
        }
    }
}
