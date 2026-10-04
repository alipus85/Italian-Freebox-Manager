package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

data class GitHubRelease(
    val tagName: String,
    val name: String,
    val body: String,
    val apkDownloadUrl: String?,
    val apkSize: Long,
    val publishedAt: String,
    val htmlUrl: String
)

data class GitHubCommit(
    val sha: String,
    val message: String,
    val authorName: String,
    val date: String,
    val htmlUrl: String
) {
    val shortSha: String get() = if (sha.length >= 7) sha.substring(0, 7) else sha
}

data class UpdateCheckResult(
    val isUpdateAvailable: Boolean,
    val currentVersion: String,
    val latestVersion: String,
    val release: GitHubRelease? = null,
    val recentCommits: List<GitHubCommit> = emptyList(),
    val errorMessage: String? = null
)

sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(val progress: Float, val downloadedBytes: Long, val totalBytes: Long) : DownloadState()
    data class Success(val apkFile: File) : DownloadState()
    data class Error(val message: String) : DownloadState()
}

class AppUpdateManager(private val context: Context) {

    companion object {
        const val DEFAULT_REPO = "alipus85/Italian-Freebox-Manager"
        const val APK_FILE_NAME = "ItalianFreeboxManager.apk"
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Controlla la presenza di una nuova release su GitHub ed estrae gli ultimi commit.
     */
    suspend fun checkForUpdates(
        repo: String = DEFAULT_REPO,
        token: String? = null,
        forceCheck: Boolean = false
    ): UpdateCheckResult = withContext(Dispatchers.IO) {
        val cleanRepo = repo.trim().removePrefix("https://github.com/").removeSuffix("/")
        if (cleanRepo.isBlank() || !cleanRepo.contains("/")) {
            return@withContext UpdateCheckResult(
                isUpdateAvailable = false,
                currentVersion = BuildConfig.VERSION_NAME,
                latestVersion = "N/A",
                errorMessage = "Formato repository non valido. Usa 'utente/repo'."
            )
        }

        try {
            // 1. Richiedi l'ultima release da GitHub
            val releaseUrl = "https://api.github.com/repos/$cleanRepo/releases/latest"
            val releaseRequestBuilder = Request.Builder()
                .url(releaseUrl)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "ItalianFreeboxManager-OTA")

            if (!token.isNullOrBlank()) {
                releaseRequestBuilder.header("Authorization", "Bearer ${token.trim()}")
            }

            val releaseResponse = httpClient.newCall(releaseRequestBuilder.build()).execute()
            if (!releaseResponse.isSuccessful) {
                val errCode = releaseResponse.code
                val errBody = releaseResponse.body?.string() ?: ""
                val msg = when (errCode) {
                    404 -> "Nessuna release trovata nel repository '$cleanRepo'."
                    401, 403 -> "Accesso limitato da GitHub (Rate Limit superato o token non valido)."
                    else -> "Errore GitHub API: HTTP $errCode"
                }
                return@withContext UpdateCheckResult(
                    isUpdateAvailable = false,
                    currentVersion = BuildConfig.VERSION_NAME,
                    latestVersion = "N/A",
                    errorMessage = "$msg $errBody".trim()
                )
            }

            val releaseJson = JSONObject(releaseResponse.body?.string() ?: "{}")
            val tagName = releaseJson.optString("tag_name", "")
            val releaseName = releaseJson.optString("name", tagName)
            val body = releaseJson.optString("body", "Nessuna nota di rilascio fornita.")
            val publishedAt = releaseJson.optString("published_at", "")
            val htmlUrl = releaseJson.optString("html_url", "")

            var apkUrl: String? = null
            var apkSize: Long = 0L

            val assets = releaseJson.optJSONArray("assets") ?: JSONArray()
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val assetName = asset.optString("name", "")
                if (assetName.endsWith(".apk", ignoreCase = true)) {
                    apkUrl = asset.optString("browser_download_url", "")
                    apkSize = asset.optLong("size", 0L)
                    if (assetName.equals(APK_FILE_NAME, ignoreCase = true)) {
                        break
                    }
                }
            }

            // 2. Richiedi gli ultimi commit recenti
            val recentCommits = fetchRecentCommits(cleanRepo, token)

            // 3. Valutazione versione e tag
            val currentVersion = BuildConfig.VERSION_NAME
            val resolvedRemoteVersion = extractVersionNumber(releaseName, body, tagName)
            val displayVersion = resolvedRemoteVersion 
                ?: (if (tagName.equals("latest", ignoreCase = true)) "1.0.x" else tagName.removePrefix("v").removePrefix("V"))

            val isNewer = if (forceCheck) {
                true
            } else {
                isRemoteVersionNewer(remoteVersionStr = displayVersion, localVersionStr = currentVersion)
            }

            val release = GitHubRelease(
                tagName = if (tagName.equals("latest", ignoreCase = true) && !resolvedRemoteVersion.isNullOrBlank()) "v$resolvedRemoteVersion" else tagName,
                name = releaseName,
                body = body,
                apkDownloadUrl = apkUrl,
                apkSize = apkSize,
                publishedAt = formatDate(publishedAt),
                htmlUrl = htmlUrl
            )

            UpdateCheckResult(
                isUpdateAvailable = (isNewer || forceCheck) && !apkUrl.isNullOrBlank(),
                currentVersion = currentVersion,
                latestVersion = displayVersion,
                release = release,
                recentCommits = recentCommits
            )

        } catch (e: Exception) {
            UpdateCheckResult(
                isUpdateAvailable = false,
                currentVersion = BuildConfig.VERSION_NAME,
                latestVersion = "N/A",
                errorMessage = "Errore durante il controllo: ${e.localizedMessage ?: e.message}"
            )
        }
    }

    /**
     * Scarica l'elenco degli ultimi commit da GitHub per comporre il changelog interattivo.
     */
    private fun fetchRecentCommits(repo: String, token: String?): List<GitHubCommit> {
        return try {
            val commitsUrl = "https://api.github.com/repos/$repo/commits?per_page=10"
            val reqBuilder = Request.Builder()
                .url(commitsUrl)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "ItalianFreeboxManager-OTA")

            if (!token.isNullOrBlank()) {
                reqBuilder.header("Authorization", "Bearer ${token.trim()}")
            }

            val response = httpClient.newCall(reqBuilder.build()).execute()
            if (!response.isSuccessful) return emptyList()

            val jsonArray = JSONArray(response.body?.string() ?: "[]")
            val list = mutableListOf<GitHubCommit>()
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                val sha = item.optString("sha", "")
                val commitObj = item.optJSONObject("commit")
                val message = commitObj?.optString("message", "")?.lines()?.firstOrNull() ?: ""
                val authorObj = commitObj?.optJSONObject("author")
                val authorName = authorObj?.optString("name", "GitHub") ?: "GitHub"
                val rawDate = authorObj?.optString("date", "") ?: ""
                val htmlUrl = item.optString("html_url", "")

                list.add(
                    GitHubCommit(
                        sha = sha,
                        message = message,
                        authorName = authorName,
                        date = formatDate(rawDate),
                        htmlUrl = htmlUrl
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Download streaming dell'APK con callback di progresso percentuale.
     */
    suspend fun downloadApk(
        downloadUrl: String,
        token: String? = null,
        onProgress: (Float, Long, Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val reqBuilder = Request.Builder()
                .url(downloadUrl)
                .header("User-Agent", "ItalianFreeboxManager-OTA")

            if (!token.isNullOrBlank()) {
                reqBuilder.header("Authorization", "Bearer ${token.trim()}")
            }

            val response = httpClient.newCall(reqBuilder.build()).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Impossibile scaricare l'APK: HTTP ${response.code}"))
            }

            val body = response.body ?: return@withContext Result.failure(Exception("Risposta vuota durante il download"))
            val totalBytes = body.contentLength()

            val targetDir = File(context.cacheDir, "apk_updates")
            if (!targetDir.exists()) targetDir.mkdirs()
            val apkFile = File(targetDir, APK_FILE_NAME)
            if (apkFile.exists()) apkFile.delete()

            body.byteStream().use { input ->
                FileOutputStream(apkFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int
                    var totalDownloaded = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalDownloaded += bytesRead
                        val progress = if (totalBytes > 0) totalDownloaded.toFloat() / totalBytes else 0f
                        onProgress(progress, totalDownloaded, totalBytes)
                    }
                    output.flush()
                }
            }

            Result.success(apkFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Avvia l'installazione nativa dell'APK scaricato tramite FileProvider e Intent ACTION_VIEW.
     */
    fun installApk(apkFile: File): Result<Unit> {
        return try {
            if (!apkFile.exists() || apkFile.length() == 0L) {
                return Result.failure(Exception("Il file APK non esiste o non è stato scaricato correttamente."))
            }

            // Verifica autorizzazione per installare pacchetti su Android 8.0+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val manageIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(manageIntent)
                    return Result.failure(Exception("Autorizzazione di installazione richiesta. Abilita l'opzione 'Consenti da questa origine' nelle impostazioni e tocca nuovamente 'Installa'."))
                }
            }

            val authority = "${context.packageName}.fileprovider"
            val apkUri = FileProvider.getUriForFile(context, authority, apkFile)

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            }

            // Concedi esplicitamente permessi di lettura alle activity delegate all'installazione
            val resInfoList = context.packageManager.queryIntentActivities(installIntent, 0)
            for (resolveInfo in resInfoList) {
                val pkgName = resolveInfo.activityInfo.packageName
                context.grantUriPermission(pkgName, apkUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(installIntent)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Estrae una versione semantica (es. 1.0.5) da stringhe di release o note.
     */
    private fun extractVersionNumber(vararg texts: String?): String? {
        val versionRegex = Regex("""(?i)\b(?:v|version|versione|build\s*#)?\s*(\d+(?:\.\d+)+)\b""")
        for (text in texts) {
            if (text.isNullOrBlank()) continue
            val match = versionRegex.find(text)
            if (match != null) {
                return match.groupValues[1]
            }
        }
        return null
    }

    /**
     * Confronto semantico della versione o tag.
     */
    private fun isRemoteVersionNewer(remoteVersionStr: String, localVersionStr: String): Boolean {
        val cleanRemote = remoteVersionStr.trim().removePrefix("v").removePrefix("V")
        val cleanLocal = localVersionStr.trim().removePrefix("v").removePrefix("V")

        if (cleanRemote == cleanLocal) return false

        val remoteParts = cleanRemote.split(".").mapNotNull { it.takeWhile { c -> c.isDigit() }.toIntOrNull() }
        val localParts = cleanLocal.split(".").mapNotNull { it.takeWhile { c -> c.isDigit() }.toIntOrNull() }

        if (remoteParts.isEmpty() && localParts.isNotEmpty()) {
            return cleanRemote != cleanLocal && cleanRemote.isNotBlank()
        }

        val maxLen = maxOf(remoteParts.size, localParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val l = localParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }

        return cleanRemote != cleanLocal
    }

    private fun formatDate(rawDate: String): String {
        return try {
            if (rawDate.isBlank()) return ""
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            val date = inputFormat.parse(rawDate) ?: return rawDate
            val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            outputFormat.format(date)
        } catch (_: Exception) {
            rawDate.take(10)
        }
    }
}
