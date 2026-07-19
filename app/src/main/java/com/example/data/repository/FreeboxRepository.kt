package com.example.data.repository
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okio.ByteString.Companion.toByteString
import kotlin.coroutines.resume

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.api.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class FreeboxRepository(private val context: Context) {

    private val sharedPrefs = context.getSharedPreferences("freebox_prefs", Context.MODE_PRIVATE)

    private val _boxUrl = MutableStateFlow(sharedPrefs.getString("box_url", "http://myiliadbox.iliad.it/") ?: "http://myiliadbox.iliad.it/")
    val boxUrl: StateFlow<String> = _boxUrl.asStateFlow()

    private val _appToken = MutableStateFlow(sharedPrefs.getString("app_token", "") ?: "")
    val appToken: StateFlow<String> = _appToken.asStateFlow()

    private val _sessionToken = MutableStateFlow("")
    val sessionToken: StateFlow<String> = _sessionToken.asStateFlow()

    private val _trackId = MutableStateFlow(sharedPrefs.getInt("track_id", -1))
    val trackId: StateFlow<Int> = _trackId.asStateFlow()

    private val _isAuthorized = MutableStateFlow(sharedPrefs.getBoolean("is_authorized", false))
    val isAuthorized: StateFlow<Boolean> = _isAuthorized.asStateFlow()

    private val _isSimulated = MutableStateFlow(sharedPrefs.getBoolean("is_simulated", false)) // Enabled by default to make testing easy
    val isSimulated: StateFlow<Boolean> = _isSimulated.asStateFlow()

    private val _discoveredApiBaseUrl = MutableStateFlow(sharedPrefs.getString("api_base_url", "/api/") ?: "/api/")
    val discoveredApiBaseUrl: StateFlow<String> = _discoveredApiBaseUrl.asStateFlow()

    private val _discoveredApiVersionMajor = MutableStateFlow(sharedPrefs.getString("api_version_major", "3") ?: "3")
    val discoveredApiVersionMajor: StateFlow<String> = _discoveredApiVersionMajor.asStateFlow()

    private val _discoveredDeviceName = MutableStateFlow(sharedPrefs.getString("discovered_device_name", "Freebox Server") ?: "Freebox Server")
    val discoveredDeviceName: StateFlow<String> = _discoveredDeviceName.asStateFlow()

    private val _discoveredBoxModelName = MutableStateFlow(sharedPrefs.getString("discovered_box_model_name", "Freebox Server") ?: "Freebox Server")
    val discoveredBoxModelName: StateFlow<String> = _discoveredBoxModelName.asStateFlow()

    private val _discoveredApiDomain = MutableStateFlow(sharedPrefs.getString("api_domain", "") ?: "")
    val discoveredApiDomain: StateFlow<String> = _discoveredApiDomain.asStateFlow()

    private val _discoveredHttpsPort = MutableStateFlow(sharedPrefs.getInt("https_port", 0))
    val discoveredHttpsPort: StateFlow<Int> = _discoveredHttpsPort.asStateFlow()

    private var apiService: FreeboxApi? = null
    private var currentSessionToken: String? = null

    // Simulated states for Demo Mode
    private val simulatedWifiEnabled = MutableStateFlow(true)
    private val simulatedConnectedDevices = MutableStateFlow(
        listOf(
            LanHost(
                "device_1", "iPhone 15 Pro", true, "smartphone", "wifi", System.currentTimeMillis(),
                l2ident = L2Ident("1A:2B:3C:4D:5E:6F", "mac"),
                l3connectivities = listOf(L3Connectivity("192.168.1.12", true, "ipv4"))
            ),
            LanHost(
                "device_2", "MacBook Pro 14", true, "workstation", "wifi", System.currentTimeMillis() - 120000,
                l2ident = L2Ident("AA:BB:CC:DD:EE:FF", "mac"),
                l3connectivities = listOf(L3Connectivity("192.168.1.15", true, "ipv4"))
            ),
            LanHost(
                "device_3", "Sony Smart TV", true, "tv", "ethernet", System.currentTimeMillis() - 600000,
                l2ident = L2Ident("11:22:33:44:55:66", "mac"),
                l3connectivities = listOf(L3Connectivity("192.168.1.50", true, "ipv4"))
            ),
            LanHost(
                "device_4", "iPad Air", false, "tablet", "wifi", System.currentTimeMillis() - 86400000,
                l2ident = L2Ident("00:11:22:33:44:55", "mac"),
                l3connectivities = listOf(L3Connectivity("192.168.1.20", false, "ipv4"))
            ),
            LanHost(
                "device_5", "HP LaserJet Printer", true, "printer", "ethernet", System.currentTimeMillis() - 1000,
                l2ident = L2Ident("77:88:99:AA:BB:CC", "mac"),
                l3connectivities = listOf(L3Connectivity("192.168.1.100", true, "ipv4"))
            )
        )
    )

    private val simulatedDownloadTasks = MutableStateFlow(
        listOf(
            DownloadTask(id = 1, name = "Ubuntu-24.04-Desktop-amd64.iso", status = "downloading", size = 4100000000L, downloadedSize = 1200000000L, queuePosition = 1),
            DownloadTask(id = 2, name = "Debian-12.5.0-amd64-netinst.iso", status = "stopped", size = 620000000L, downloadedSize = 310000000L, queuePosition = 2),
            DownloadTask(id = 3, name = "Big_Buck_Bunny_1080p.mp4", status = "done", size = 276000000L, downloadedSize = 276000000L, queuePosition = 3)
        )
    )

    init {
        rebuildApiService()
    }

    fun setBoxUrl(url: String) {
        val sanitized = if (!url.startsWith("http://") && !url.startsWith("https://")) {
            "http://$url"
        } else {
            url
        }
        val finalUrl = if (!sanitized.endsWith("/")) "$sanitized/" else sanitized
        sharedPrefs.edit().putString("box_url", finalUrl).apply()
        _boxUrl.value = finalUrl
        rebuildApiService()
    }

    fun setAppToken(token: String) {
        sharedPrefs.edit().putString("app_token", token).apply()
        _appToken.value = token
    }

    fun setTrackId(id: Int) {
        sharedPrefs.edit().putInt("track_id", id).apply()
        _trackId.value = id
    }

    fun setIsAuthorized(authorized: Boolean) {
        sharedPrefs.edit().putBoolean("is_authorized", authorized).apply()
        _isAuthorized.value = authorized
    }

    fun setIsSimulated(simulated: Boolean) {
        sharedPrefs.edit().putBoolean("is_simulated", simulated).apply()
        _isSimulated.value = simulated
    }

    fun clearCredentials() {
        sharedPrefs.edit()
            .remove("app_token")
            .remove("track_id")
            .remove("is_authorized")
            .remove("api_base_url")
            .remove("api_version_major")
            .remove("discovered_device_name")
            .remove("discovered_box_model_name")
            .remove("api_domain")
            .remove("https_port")
            .apply()
        _appToken.value = ""
        _trackId.value = -1
        _isAuthorized.value = false
        currentSessionToken = null
        _sessionToken.value = ""
        _discoveredApiBaseUrl.value = "/api/"
        _discoveredApiVersionMajor.value = "3"
        _discoveredDeviceName.value = "Freebox Server"
        _discoveredBoxModelName.value = "Freebox Server"
        _discoveredApiDomain.value = ""
        _discoveredHttpsPort.value = 0
    }

    fun setDiscoveredParams(baseUrl: String, majorVersion: String, deviceName: String, boxModelName: String, apiDomain: String?, httpsPort: Int?) {
        sharedPrefs.edit()
            .putString("api_base_url", baseUrl)
            .putString("api_version_major", majorVersion)
            .putString("discovered_device_name", deviceName)
            .putString("discovered_box_model_name", boxModelName)
            .putString("api_domain", apiDomain ?: "")
            .putInt("https_port", httpsPort ?: 0)
            .apply()
        _discoveredApiBaseUrl.value = baseUrl
        _discoveredApiVersionMajor.value = majorVersion
        _discoveredDeviceName.value = deviceName
        _discoveredBoxModelName.value = boxModelName
        _discoveredApiDomain.value = apiDomain ?: ""
        _discoveredHttpsPort.value = httpsPort ?: 0
    }

    fun switchToDiscoveredUrl() {
        val domain = _discoveredApiDomain.value
        val port = _discoveredHttpsPort.value
        if (domain.isNotEmpty() && port > 0) {
            val newUrl = "https://$domain:$port/"
            setBoxUrl(newUrl)
        }
    }

    private fun getUnsafeOkHttpClientBuilder(): OkHttpClient.Builder {
        return try {
            val trustAllCerts = arrayOf<TrustManager>(
                object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                }
            )
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, SecureRandom())
            val sslSocketFactory = sslContext.socketFactory

            OkHttpClient.Builder()
                .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    private fun rebuildApiService() {
        try {
            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            val versionInterceptor = Interceptor { chain ->
                val originalRequest = chain.request()
                val originalUrl = originalRequest.url
                
                val actualBaseUrl = _discoveredApiBaseUrl.value
                val actualMajorVersion = _discoveredApiVersionMajor.value
                
                val originalPath = originalUrl.encodedPath
                val prefixToReplace = "/api/v3/"
                
                val rawReplacement = if (actualBaseUrl.endsWith("/")) {
                    "${actualBaseUrl}v${actualMajorVersion}/"
                } else {
                    "${actualBaseUrl}/v${actualMajorVersion}/"
                }
                val cleanReplacement = rawReplacement.replace("//", "/")
                
                if (originalPath.startsWith(prefixToReplace)) {
                    val newPath = originalPath.replaceFirst(prefixToReplace, cleanReplacement)
                    val newUrl = originalUrl.newBuilder()
                        .encodedPath(newPath)
                        .build()
                    val newRequest = originalRequest.newBuilder()
                        .url(newUrl)
                        .build()
                    chain.proceed(newRequest)
                } else {
                    chain.proceed(originalRequest)
                }
            }

            val client = getUnsafeOkHttpClientBuilder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
                .addInterceptor(versionInterceptor)
                .addInterceptor(logging)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(_boxUrl.value)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()

            apiService = retrofit.create(FreeboxApi::class.java)
        } catch (e: Exception) {
            Log.e("FreeboxRepository", "Failed to build ApiService", e)
        }
    }

    suspend fun autoDiscover(): Result<String> = withContext(Dispatchers.IO) {
        if (_isSimulated.value) {
            delay(1000)
            return@withContext Result.success("http://myiliadbox.iliad.it/")
        }
        try {
            val tempClient = getUnsafeOkHttpClientBuilder()
                .connectTimeout(3, TimeUnit.SECONDS)
                .readTimeout(3, TimeUnit.SECONDS)
                .build()
            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
            val tempRetrofit = Retrofit.Builder()
                .baseUrl("http://myiliadbox.iliad.it/")
                .client(tempClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
            val tempApi = tempRetrofit.create(FreeboxApi::class.java)
            
            val response = tempApi.getApiVersion()
            if (response.isSuccessful && response.body() != null) {
                return@withContext Result.success("http://myiliadbox.iliad.it/")
            }
            Result.failure(Exception("Discovery failed, no response from local box"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun testConnection(): Result<ApiVersion> = withContext(Dispatchers.IO) {
        if (_isSimulated.value) {
            delay(1000)
            return@withContext Result.success(ApiVersion("italianfreebox Server r1", "/api/", "v3", "italianfreebox", "simulated.freebox.fr", 3615, true))
        }
        val service = apiService ?: return@withContext Result.failure(Exception("Service not initialized"))
        try {
            val response = service.getApiVersion()
            if (response.isSuccessful && response.body() != null) {
                val apiVer = response.body()!!
                val baseUrl = apiVer.apiBaseUrl ?: "/api/"
                val rawVersion = apiVer.apiVersion ?: "3"
                val majorVersion = rawVersion.split(".").firstOrNull() ?: "3"
                val deviceName = apiVer.deviceName ?: "Freebox Server"
                val boxModelName = apiVer.boxModelName ?: "Freebox Server"
                
                setDiscoveredParams(
                    baseUrl = baseUrl,
                    majorVersion = majorVersion,
                    deviceName = deviceName,
                    boxModelName = boxModelName,
                    apiDomain = apiVer.apiDomain,
                    httpsPort = apiVer.httpsPort
                )
                Result.success(apiVer)
            } else {
                Result.failure(Exception("Connection test failed (HTTP ${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun computeHmacSha1(key: String, data: String): String {
        return try {
            val mac = Mac.getInstance("HmacSHA1")
            val secretKey = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "HmacSHA1")
            mac.init(secretKey)
            val bytes = mac.doFinal(data.toByteArray(Charsets.UTF_8))
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e("FreeboxRepository", "HMAC computation failed", e)
            ""
        }
    }

    suspend fun registerApplication(): Result<AuthorizeResult> = withContext(Dispatchers.IO) {
        if (_isSimulated.value) {
            delay(1500)
            val fakeResult = AuthorizeResult(appToken = "simulated_app_token_123456789", trackId = 9999)
            setAppToken(fakeResult.appToken)
            setTrackId(fakeResult.trackId)
            return@withContext Result.success(fakeResult)
        }

        val service = apiService ?: return@withContext Result.failure(Exception("Service not initialized"))
        try {
            val req = AuthorizeRequest(
                appId = "it.adrix.italianfreebox.manager",
                appName = "ItalianFreebox Manager",
                appVersion = "1.0",
                deviceName = android.os.Build.MODEL
            )
            val response = service.authorizeApp(req)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.success && body.result != null) {
                    setAppToken(body.result.appToken)
                    setTrackId(body.result.trackId)
                    setIsAuthorized(false)
                    Result.success(body.result)
                } else {
                    Result.failure(Exception(body?.msg ?: "Authorization request returned success = false"))
                }
            } else {
                Result.failure(Exception("HTTP Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkAuthorizationStatus(): Result<String> = withContext(Dispatchers.IO) {
        val tId = _trackId.value
        if (tId == -1) return@withContext Result.failure(Exception("No track ID found. Please register first."))

        if (_isSimulated.value) {
            delay(1500)
            setIsAuthorized(true)
            return@withContext Result.success("granted")
        }

        val service = apiService ?: return@withContext Result.failure(Exception("Service not initialized"))
        try {
            val response = service.getAuthTrackStatus(tId)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.success && body.result != null) {
                    val status = body.result.status
                    if (status == "granted") {
                        setIsAuthorized(true)
                    }
                    Result.success(status)
                } else {
                    Result.failure(Exception("Status check failed"))
                }
            } else {
                Result.failure(Exception("HTTP Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginAndOpenSession(): Result<String> = withContext(Dispatchers.IO) {
        if (_isSimulated.value) {
            delay(1000)
            currentSessionToken = "simulated_session_token_abc123"
            _sessionToken.value = "simulated_session_token_abc123"
            return@withContext Result.success(currentSessionToken!!)
        }

        val service = apiService ?: return@withContext Result.failure(Exception("Service not initialized"))
        val token = _appToken.value
        if (token.isEmpty()) return@withContext Result.failure(Exception("App token missing. Please register first."))

        try {
            val challengeResponse = service.getLoginChallenge()
            if (!challengeResponse.isSuccessful || challengeResponse.body()?.success != true) {
                return@withContext Result.failure(Exception("Failed to get login challenge"))
            }

            val challenge = challengeResponse.body()?.result?.challenge
                ?: return@withContext Result.failure(Exception("Challenge string is null"))

            val password = computeHmacSha1(token, challenge)
            val sessionReq = SessionRequest(appId = "it.adrix.italianfreebox.manager", password = password)

            val sessionResponse = service.login(sessionReq)
            if (sessionResponse.isSuccessful) {
                val body = sessionResponse.body()
                if (body != null && body.success && body.result?.sessionToken != null) {
                    currentSessionToken = body.result.sessionToken
                    _sessionToken.value = body.result.sessionToken!!
                    Result.success(body.result.sessionToken)
                } else {
                    Result.failure(Exception(body?.msg ?: "Login failed: empty token"))
                }
            } else {
                Result.failure(Exception("HTTP Login error: ${sessionResponse.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getWifiConfig(): Result<WifiConfigResult> = withContext(Dispatchers.IO) {
        if (_isSimulated.value) {
            return@withContext Result.success(WifiConfigResult(simulatedWifiEnabled.value, "2.4GHz + 5GHz"))
        }

        val token = currentSessionToken ?: return@withContext Result.failure(Exception("No active session"))
        val service = apiService ?: return@withContext Result.failure(Exception("Service not initialized"))

        try {
            val response = service.getWifiConfig(token)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.result!!)
            } else {
                Result.failure(Exception("Failed to fetch Wi-Fi configuration"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleWifi(enabled: Boolean): Result<Boolean> = withContext(Dispatchers.IO) {
        if (_isSimulated.value) {
            delay(800)
            simulatedWifiEnabled.value = enabled
            return@withContext Result.success(enabled)
        }

        val token = currentSessionToken ?: return@withContext Result.failure(Exception("No active session"))
        val service = apiService ?: return@withContext Result.failure(Exception("Service not initialized"))

        try {
            val req = WifiConfigUpdateRequest(enabled = enabled)
            val response = service.updateWifiConfig(token, req)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(enabled)
            } else {
                Result.failure(Exception("Failed to update Wi-Fi state: ${response.body()?.msg}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rebootBox(): Result<Boolean> = withContext(Dispatchers.IO) {
        if (_isSimulated.value) {
            delay(1500)
            return@withContext Result.success(true)
        }

        val token = currentSessionToken ?: return@withContext Result.failure(Exception("No active session"))
        val service = apiService ?: return@withContext Result.failure(Exception("Service not initialized"))

        try {
            val response = service.rebootSystem(token)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to reboot box: ${response.body()?.msg}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSystemConfig(): Result<SystemConfigResult> = withContext(Dispatchers.IO) {
        if (_isSimulated.value) {
            return@withContext Result.success(
                SystemConfigResult(
                    uptime = "4 giorni, 12 ore",
                    uptimeVal = 388800L,
                    firmwareVersion = "4.8.1",
                    sensors = listOf(
                        SystemSensor(id = "cpu_ap", name = "Température CPU", value = 48),
                        SystemSensor(id = "t1", name = "Température 1", value = 45)
                    ),
                    fans = listOf(
                        SystemFan(id = "main", name = "Ventilateur 1", value = 1850)
                    ),
                    modelInfo = SystemModelInfo(prettyName = "Iliadbox Server 5G", name = "iliadbox")
                )
            )
        }

        val token = currentSessionToken ?: return@withContext Result.failure(Exception("No active session"))
        val service = apiService ?: return@withContext Result.failure(Exception("Service not initialized"))

        try {
            val response = service.getSystemConfig(token)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.result!!)
            } else {
                Result.failure(Exception("Failed to fetch system config"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getConnectedDevices(): Result<List<LanHost>> = withContext(Dispatchers.IO) {
        if (_isSimulated.value) {
            return@withContext Result.success(simulatedConnectedDevices.value)
        }

        val token = currentSessionToken ?: return@withContext Result.failure(Exception("No active session"))
        val service = apiService ?: return@withContext Result.failure(Exception("Service not initialized"))

        try {
            val response = service.getLanHosts(token)
            if (response.isSuccessful && response.body()?.success == true) {
                val hosts = response.body()!!.result ?: emptyList()
                Result.success(hosts)
            } else {
                Result.failure(Exception("Failed to fetch connected devices"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getConnectionStatus(): Result<ConnectionResult> = withContext(Dispatchers.IO) {
        if (_isSimulated.value) {
            // Return realistic fiber speed test values
            return@withContext Result.success(
                ConnectionResult(
                    state = "up",
                    bandwidthDown = 842_000_000L, // 842 Mbps
                    bandwidthUp = 294_000_000L,   // 294 Mbps
                    media = "fiber"
                )
            )
        }

        val token = currentSessionToken ?: return@withContext Result.failure(Exception("No active session"))
        val service = apiService ?: return@withContext Result.failure(Exception("Service not initialized"))

        try {
            val response = service.getConnectionStatus(token)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.result!!)
            } else {
                Result.failure(Exception("Failed to fetch connection status"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFiles(path: String = ""): Result<List<FileInfo>> = withContext(Dispatchers.IO) {
        if (_isSimulated.value) {
            return@withContext Result.success(
                listOf(
                    FileInfo(name = "Disque dur", type = "dir", size = 0L, path = "L0Rpc3F1ZSBkdXI=", mimetype = null),
                    FileInfo(name = "Downloads", type = "dir", size = 0L, path = "L0Rvd25sb2Fkcw==", mimetype = null),
                    FileInfo(name = "Photos", type = "dir", size = 0L, path = "L1Bob3Rvcw==", mimetype = null),
                    FileInfo(name = "document.pdf", type = "file", size = 2400000L, path = "L2RvY3VtZW50LnBkZg==", mimetype = "application/pdf"),
                    FileInfo(name = "movie.mp4", type = "file", size = 1200000000L, path = "L21vdmllLm1wNA==", mimetype = "video/mp4")
                )
            )
        }
        val token = currentSessionToken ?: return@withContext Result.failure(Exception("No active session"))
        val service = apiService ?: return@withContext Result.failure(Exception("Service not initialized"))
        try {
            val base64Path = android.util.Base64.encodeToString(path.toByteArray(), android.util.Base64.NO_WRAP)
            val response = service.getFiles(token, base64Path)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.result ?: emptyList())
            } else {
                Result.failure(Exception("Failed to fetch files"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeFile(path: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val token = currentSessionToken ?: return@withContext Result.failure(Exception("No active session"))
        val service = apiService ?: return@withContext Result.failure(Exception("Service not initialized"))
        try {
            val base64Path = android.util.Base64.encodeToString(path.toByteArray(), android.util.Base64.NO_WRAP)
            val response = service.removeFiles(token, RmRequest(listOf(base64Path)))
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to remove file"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createDirectory(parentPath: String, dirName: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val token = currentSessionToken ?: return@withContext Result.failure(Exception("No active session"))
        val service = apiService ?: return@withContext Result.failure(Exception("Service not initialized"))
        try {
            val base64Parent = android.util.Base64.encodeToString(parentPath.toByteArray(), android.util.Base64.NO_WRAP)
            val response = service.createDirectory(token, MkdirRequest(base64Parent, dirName))
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to create directory"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun renameFile(srcPath: String, newName: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val token = currentSessionToken ?: return@withContext Result.failure(Exception("No active session"))
        val service = apiService ?: return@withContext Result.failure(Exception("Service not initialized"))
        try {
            val base64Src = android.util.Base64.encodeToString(srcPath.toByteArray(), android.util.Base64.NO_WRAP)
            val response = service.renameFile(token, RenameRequest(base64Src, newName))
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to rename file"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    fun getDownloadUrl(path: String): String? {
        val token = currentSessionToken ?: return null
        val baseUrl = _boxUrl.value
        val base64Path = android.util.Base64.encodeToString(path.toByteArray(), android.util.Base64.NO_WRAP)
        // Adjust for API version
        val actualBaseUrl = _discoveredApiBaseUrl.value
        val actualMajorVersion = _discoveredApiVersionMajor.value
        val rawPrefix = if (actualBaseUrl.endsWith("/")) "${actualBaseUrl}v${actualMajorVersion}/" else "${actualBaseUrl}/v${actualMajorVersion}/"
        val cleanPrefix = rawPrefix.replace("//", "/")
        return "${baseUrl.removeSuffix("/")}${cleanPrefix}dl/$base64Path"
    }

    suspend fun downloadFileToDisk(path: String, destFile: java.io.File, onProgress: (Float) -> Unit): Result<Boolean> = withContext(Dispatchers.IO) {
        val token = currentSessionToken ?: return@withContext Result.failure(Exception("No active session"))
        val service = apiService ?: return@withContext Result.failure(Exception("Service not initialized"))
        try {
            val base64Path = android.util.Base64.encodeToString(path.toByteArray(), android.util.Base64.NO_WRAP)
            Log.d("FreeboxDownload", "Downloading from path: $path, base64: $base64Path")
            val response = service.downloadFile(token, base64Path)
            if (response.isSuccessful && response.body() != null) {
                val totalBytes = response.body()!!.contentLength()
                var bytesRead = 0L
                response.body()!!.byteStream().use { input ->
                    java.io.FileOutputStream(destFile).use { output ->
                        val buffer = ByteArray(8192)
                        var read = input.read(buffer)
                        while (read != -1) {
                            output.write(buffer, 0, read)
                            bytesRead += read
                            if (totalBytes > 0) {
                                onProgress(bytesRead.toFloat() / totalBytes)
                            }
                            read = input.read(buffer)
                        }
                    }
                }
                Result.success(true)
            } else {
                Log.e("FreeboxDownload", "Download failed: ${response.code()} ${response.message()}")
                Result.failure(Exception("Failed to download file: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("FreeboxDownload", "Download exception", e)
            Result.failure(e)
        }
    }

    suspend fun getDownloads(): Result<List<DownloadTask>> = withContext(Dispatchers.IO) {
        if (_isSimulated.value) {
            val updated = simulatedDownloadTasks.value.map { task ->
                if (task.status == "downloading") {
                    val addSpeed = (3_000_000..12_000_000).random().toLong() // 3-12 MB/s
                    val newDownloaded = (task.downloadedSize + addSpeed).coerceAtMost(task.size)
                    val newStatus = if (newDownloaded >= task.size) "done" else "downloading"
                    val rx = if (newStatus == "downloading") addSpeed else 0L
                    val tx = if (newStatus == "downloading") (50_000..300_000).random().toLong() else 0L
                    task.copy(
                        downloadedSize = newDownloaded,
                        status = newStatus,
                        rxRate = rx,
                        txRate = tx
                    )
                } else {
                    task.copy(rxRate = 0L, txRate = 0L)
                }
            }
            simulatedDownloadTasks.value = updated
            return@withContext Result.success(updated)
        }

        val token = currentSessionToken ?: return@withContext Result.failure(Exception("No active session"))
        val service = apiService ?: return@withContext Result.failure(Exception("Service not initialized"))
        try {
            val response = service.getDownloads(token)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.result ?: emptyList())
            } else {
                Result.failure(Exception("Failed to fetch downloads"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDownloadFiles(taskId: Int): Result<List<com.example.data.api.DownloadFile>> = withContext(Dispatchers.IO) {
        if (_isSimulated.value) {
            val task = simulatedDownloadTasks.value.find { it.id == taskId }
            val taskName = task?.name ?: "download_task"
            val totalSize = task?.size ?: 1_500_000_000L
            val downloaded = task?.downloadedSize ?: 0L
            val ratio = if (totalSize > 0) downloaded.toDouble() / totalSize else 1.0

            val ext = taskName.substringAfterLast(".", "bin")
            val baseName = taskName.substringBeforeLast(".")

            val file1Name = "$baseName.$ext"
            val file2Name = "$baseName-metadata.json"
            val file3Name = "$baseName-checksum.sha256"

            val file1Size = (totalSize * 0.95).toLong()
            val file2Size = (totalSize * 0.04).toLong().coerceAtLeast(1024L)
            val file3Size = (totalSize * 0.01).toLong().coerceAtLeast(512L)

            val file1Rx = (downloaded * 0.95).toLong().coerceAtMost(file1Size)
            val file2Rx = if (ratio > 0.05) file2Size else 0L
            val file3Rx = if (ratio > 0.1) file3Size else 0L

            val files = listOf(
                com.example.data.api.DownloadFile(
                    id = "$taskId-1",
                    taskId = taskId.toString(),
                    name = file1Name,
                    path = "/Disque dur/Téléchargements/$file1Name",
                    size = file1Size,
                    rx = file1Rx,
                    status = if (file1Rx >= file1Size) "done" else "downloading",
                    priority = "normal",
                    mimetype = "application/octet-stream"
                ),
                com.example.data.api.DownloadFile(
                    id = "$taskId-2",
                    taskId = taskId.toString(),
                    name = file2Name,
                    path = "/Disque dur/Téléchargements/$file2Name",
                    size = file2Size,
                    rx = file2Rx,
                    status = if (file2Rx >= file2Size) "done" else "downloading",
                    priority = "normal",
                    mimetype = "application/json"
                ),
                com.example.data.api.DownloadFile(
                    id = "$taskId-3",
                    taskId = taskId.toString(),
                    name = file3Name,
                    path = "/Disque dur/Téléchargements/$file3Name",
                    size = file3Size,
                    rx = file3Rx,
                    status = if (file3Rx >= file3Size) "done" else "downloading",
                    priority = "normal",
                    mimetype = "text/plain"
                )
            )
            return@withContext Result.success(files)
        }

        val token = currentSessionToken ?: return@withContext Result.failure(Exception("No active session"))
        val service = apiService ?: return@withContext Result.failure(Exception("Service not initialized"))
        try {
            val response = service.getDownloadFiles(token, taskId)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.result ?: emptyList())
            } else {
                Result.failure(Exception("Failed to fetch download files"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPartitions(): Result<List<DiskPartition>> = withContext(Dispatchers.IO) {
        if (_isSimulated.value) {
            return@withContext Result.success(
                listOf(
                    DiskPartition(
                        id = 1,
                        label = "Hard Disk Interno",
                        totalBytes = 1_000_200_000_000L, // ~1 TB
                        usedBytes = 420_500_000_000L,   // 420.5 GB
                        freeBytes = 579_700_000_000L,   // 579.7 GB
                        state = "mounted"
                    ),
                    DiskPartition(
                        id = 2,
                        label = "Chiavetta USB Backup",
                        totalBytes = 64_000_000_000L,    // 64 GB
                        usedBytes = 12_000_000_000L,    // 12 GB
                        freeBytes = 52_000_000_000L,    // 52 GB
                        state = "mounted"
                    )
                )
            )
        }

        val token = currentSessionToken ?: return@withContext Result.failure(Exception("No active session"))
        val service = apiService ?: return@withContext Result.failure(Exception("Service not initialized"))
        try {
            val response = service.getPartitions(token)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.result ?: emptyList())
            } else {
                Result.failure(Exception("Failed to fetch partitions"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addDownload(url: String): Result<DownloadTask> = withContext(Dispatchers.IO) {
        if (_isSimulated.value) {
            val fileName = url.substringAfterLast("/").substringBefore("?").ifEmpty { "download_task_${System.currentTimeMillis() % 10000}" }
            val newTask = DownloadTask(
                id = (simulatedDownloadTasks.value.map { it.id }.maxOrNull() ?: 0) + 1,
                name = fileName,
                status = "downloading",
                size = 150_000_000L + (10_000_000L..1_000_000_000L).random(),
                downloadedSize = 0L,
                queuePosition = simulatedDownloadTasks.value.size + 1
            )
            simulatedDownloadTasks.value = simulatedDownloadTasks.value + newTask
            return@withContext Result.success(newTask)
        }

        val token = currentSessionToken ?: return@withContext Result.failure(Exception("No active session"))
        val service = apiService ?: return@withContext Result.failure(Exception("Service not initialized"))
        try {
            val response = service.addDownload(token, url)
            if (response.isSuccessful && response.body()?.success == true) {
                val addResult = response.body()!!.result!!
                val dummyTask = DownloadTask(
                    id = addResult.id,
                    name = url.substringAfterLast("/").substringBefore("?"),
                    status = "downloading",
                    size = 0L,
                    downloadedSize = 0L,
                    queuePosition = 0
                )
                Result.success(dummyTask)
            } else {
                Result.failure(Exception("Failed to add download"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun controlDownload(id: Int, status: String): Result<Boolean> = withContext(Dispatchers.IO) {
        if (_isSimulated.value) {
            val updated = simulatedDownloadTasks.value.map { task ->
                if (task.id == id) {
                    val newStatus = when (status) {
                        "stopped" -> "stopped"
                        "downloading" -> "downloading"
                        else -> status
                    }
                    task.copy(status = newStatus)
                } else {
                    task
                }
            }
            simulatedDownloadTasks.value = updated
            return@withContext Result.success(true)
        }

        val token = currentSessionToken ?: return@withContext Result.failure(Exception("No active session"))
        val service = apiService ?: return@withContext Result.failure(Exception("Service not initialized"))
        try {
            val response = service.controlDownload(token, id, com.example.data.api.ControlDownloadBody(status))
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to control download"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeDownload(id: Int): Result<Boolean> = withContext(Dispatchers.IO) {
        if (_isSimulated.value) {
            simulatedDownloadTasks.value = simulatedDownloadTasks.value.filter { it.id != id }
            return@withContext Result.success(true)
        }

        val token = currentSessionToken ?: return@withContext Result.failure(Exception("No active session"))
        val service = apiService ?: return@withContext Result.failure(Exception("Service not initialized"))
        try {
            val response = service.removeDownload(token, id)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to remove download"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addDownloadByFile(uri: Uri): Result<DownloadTask> = withContext(Dispatchers.IO) {
        if (_isSimulated.value) {
            val fileName = "uploaded_torrent_${System.currentTimeMillis() % 10000}.torrent"
            val newTask = DownloadTask(
                id = (simulatedDownloadTasks.value.map { it.id }.maxOrNull() ?: 0) + 1,
                name = fileName,
                status = "downloading",
                size = 150_000_000L + (10_000_000L..1_000_000_000L).random(),
                downloadedSize = 0L,
                queuePosition = simulatedDownloadTasks.value.size + 1
            )
            simulatedDownloadTasks.value = simulatedDownloadTasks.value + newTask
            return@withContext Result.success(newTask)
        }

        val token = currentSessionToken ?: return@withContext Result.failure(Exception("No active session"))
        val service = apiService ?: return@withContext Result.failure(Exception("Service not initialized"))
        try {
            val resolver = context.contentResolver
            val inputStream = resolver.openInputStream(uri) ?: return@withContext Result.failure(Exception("Failed to open file"))
            val bytes = inputStream.readBytes()
            inputStream.close()

            var fileName = "download.torrent"
            resolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIdx != -1) fileName = cursor.getString(nameIdx)
                }
            }

            val requestBody = bytes.toRequestBody("application/octet-stream".toMediaTypeOrNull())
            val part = okhttp3.MultipartBody.Part.createFormData("download_file", fileName, requestBody)

            val response = service.addDownloadFile(token, part)
            if (response.isSuccessful && response.body()?.success == true) {
                val addResult = response.body()!!.result!!
                val dummyTask = DownloadTask(
                    id = addResult.id,
                    name = fileName,
                    status = "downloading",
                    size = 0L,
                    downloadedSize = 0L,
                    queuePosition = 0
                )
                Result.success(dummyTask)
            } else {
                Result.failure(Exception("Failed to add download by file"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadFileToFs(destinationDir: String, uri: Uri): Result<Boolean> = suspendCancellableCoroutine { continuation ->
        if (_isSimulated.value) {
            continuation.resume(Result.success(true))
            return@suspendCancellableCoroutine
        }

        val token = _sessionToken.value
        if (token.isEmpty()) {
            continuation.resume(Result.failure(Exception("No active session")))
            return@suspendCancellableCoroutine
        }

        val resolver = context.contentResolver
        var fileName = "uploaded_file"
        var fileSize = 0L
        resolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIdx != -1) fileName = cursor.getString(nameIdx)
                val sizeIdx = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (sizeIdx != -1) fileSize = cursor.getLong(sizeIdx)
            }
        }

        val urlStr = _boxUrl.value.removeSuffix("/") + "/api/v8/ws/upload"
        val wsUrl = urlStr.replace("http://", "ws://").replace("https://", "wss://")
        
        val request = okhttp3.Request.Builder()
            .url(wsUrl)
            .addHeader("X-Fbx-App-Auth", token)
            .build()
            
        val okHttpClient = OkHttpClient.Builder().build()
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val startAdapter = moshi.adapter(Map::class.java)

        val webSocket = okHttpClient.newWebSocket(request, object : okhttp3.WebSocketListener() {
            override fun onOpen(webSocket: okhttp3.WebSocket, response: okhttp3.Response) {
                val startAction = mapOf(
                    "action" to "upload_start",
                    "request_id" to 1,
                    "dirname" to destinationDir,
                    "filename" to fileName,
                    "size" to fileSize,
                    "force" to "overwrite"
                )
                webSocket.send(startAdapter.toJson(startAction))
            }

            override fun onMessage(webSocket: okhttp3.WebSocket, text: String) {
                try {
                    val map = startAdapter.fromJson(text) as? Map<*, *> ?: return
                    val action = map["action"] as? String
                    val success = map["success"] as? Boolean ?: false
                    
                    if (action == "upload_start") {
                        if (success) {
                            GlobalScope.launch(Dispatchers.IO) {
                                try {
                                    val inputStream = resolver.openInputStream(uri)
                                    val buffer = ByteArray(64 * 1024)
                                    var bytesRead: Int
                                    while (inputStream?.read(buffer).also { bytesRead = it ?: -1 } != -1) {
                                        webSocket.send(buffer.toByteString(0, bytesRead))
                                    }
                                    inputStream?.close()
                                    
                                    val finalizeAction = mapOf(
                                        "action" to "upload_finalize",
                                        "request_id" to 1
                                    )
                                    webSocket.send(startAdapter.toJson(finalizeAction))
                                } catch (e: Exception) {
                                    if (continuation.isActive) continuation.resume(Result.failure(e))
                                    webSocket.close(1000, null)
                                }
                            }
                        } else {
                            if (continuation.isActive) continuation.resume(Result.failure(Exception("Failed to start upload: $text")))
                            webSocket.close(1000, null)
                        }
                    } else if (action == "upload_finalize") {
                        if (success) {
                            if (continuation.isActive) continuation.resume(Result.success(true))
                        } else {
                            if (continuation.isActive) continuation.resume(Result.failure(Exception("Failed to finalize upload: $text")))
                        }
                        webSocket.close(1000, null)
                    }
                } catch (e: Exception) {
                    if (continuation.isActive) continuation.resume(Result.failure(e))
                    webSocket.close(1000, null)
                }
            }

            override fun onFailure(webSocket: okhttp3.WebSocket, t: Throwable, response: okhttp3.Response?) {
                if (continuation.isActive) continuation.resume(Result.failure(t))
            }
        })

        continuation.invokeOnCancellation {
            webSocket.close(1000, null)
        }
    }
}


