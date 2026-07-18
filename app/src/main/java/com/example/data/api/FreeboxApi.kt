package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.*

@JsonClass(generateAdapter = true)
data class ApiVersion(
    @Json(name = "box_model_name") val boxModelName: String?,
    @Json(name = "api_base_url") val apiBaseUrl: String?,
    @Json(name = "api_version") val apiVersion: String?,
    @Json(name = "device_name") val deviceName: String?,
    @Json(name = "api_domain") val apiDomain: String?,
    @Json(name = "https_port") val httpsPort: Int?,
    @Json(name = "https_available") val httpsAvailable: Boolean?
)

@JsonClass(generateAdapter = true)
data class AuthorizeRequest(
    @Json(name = "app_id") val appId: String,
    @Json(name = "app_name") val appName: String,
    @Json(name = "app_version") val appVersion: String,
    @Json(name = "device_name") val deviceName: String
)

@JsonClass(generateAdapter = true)
data class AuthorizeResult(
    @Json(name = "app_token") val appToken: String,
    @Json(name = "track_id") val trackId: Int
)

@JsonClass(generateAdapter = true)
data class AuthorizeResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "result") val result: AuthorizeResult?,
    @Json(name = "msg") val msg: String?,
    @Json(name = "error_code") val errorCode: String?
)

@JsonClass(generateAdapter = true)
data class TrackResult(
    @Json(name = "status") val status: String, // pending, granted, denied, timeout
    @Json(name = "challenge") val challenge: String?
)

@JsonClass(generateAdapter = true)
data class TrackResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "result") val result: TrackResult?
)

@JsonClass(generateAdapter = true)
data class LoginResult(
    @Json(name = "logged_in") val loggedIn: Boolean,
    @Json(name = "challenge") val challenge: String?
)

@JsonClass(generateAdapter = true)
data class LoginResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "result") val result: LoginResult?
)

@JsonClass(generateAdapter = true)
data class SessionRequest(
    @Json(name = "app_id") val appId: String,
    @Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class SessionResult(
    @Json(name = "session_token") val sessionToken: String?,
    @Json(name = "logged_in") val loggedIn: Boolean?
)

@JsonClass(generateAdapter = true)
data class SessionResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "result") val result: SessionResult?,
    @Json(name = "msg") val msg: String?
)

@JsonClass(generateAdapter = true)
data class ConnectionResult(
    @Json(name = "state") val state: String?, // up, down
    @Json(name = "bandwidth_down") val bandwidthDown: Long?, // in bps
    @Json(name = "bandwidth_up") val bandwidthUp: Long?, // in bps
    @Json(name = "media") val media: String? // fiber, xdsl
)

@JsonClass(generateAdapter = true)
data class ConnectionResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "result") val result: ConnectionResult?
)

@JsonClass(generateAdapter = true)
data class WifiConfigResult(
    @Json(name = "enabled") val enabled: Boolean,
    @Json(name = "active_band") val activeBand: String?
)

@JsonClass(generateAdapter = true)
data class WifiConfigResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "result") val result: WifiConfigResult?
)

@JsonClass(generateAdapter = true)
data class WifiConfigUpdateRequest(
    @Json(name = "enabled") val enabled: Boolean
)

@JsonClass(generateAdapter = true)
data class L2Ident(
    @Json(name = "id") val id: String?,
    @Json(name = "type") val type: String?
)

@JsonClass(generateAdapter = true)
data class LanHost(
    @Json(name = "id") val id: String,
    @Json(name = "primary_name") val primaryName: String?,
    @Json(name = "active") val active: Boolean?,
    @Json(name = "host_type") val hostType: String?, // smartphone, workstation, printer, etc.
    @Json(name = "connectivity_type") val connectivityType: String?, // wifi, ethernet
    @Json(name = "last_activity") val lastActivity: Long?
)

@JsonClass(generateAdapter = true)
data class LanHostsResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "result") val result: List<LanHost>?
)

@JsonClass(generateAdapter = true)
data class CommonResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "msg") val msg: String?,
    @Json(name = "error_code") val errorCode: String?
)

@JsonClass(generateAdapter = true)
data class FileInfo(
    @Json(name = "name") val name: String,
    @Json(name = "type") val type: String, // "dir" or "file"
    @Json(name = "size") val size: Long?,
    @Json(name = "path") val path: String?,
    @Json(name = "mimetype") val mimetype: String?
)

@JsonClass(generateAdapter = true)
data class FileListResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "result") val result: List<FileInfo>?
)

@JsonClass(generateAdapter = true)
data class RmRequest(
    @Json(name = "files") val files: List<String>
)

@JsonClass(generateAdapter = true)
data class MkdirRequest(
    @Json(name = "parent") val parent: String,
    @Json(name = "dirname") val dirname: String
)

@JsonClass(generateAdapter = true)
data class RenameRequest(
    @Json(name = "src") val src: String,
    @Json(name = "dst") val dst: String
)

@JsonClass(generateAdapter = true)
data class SystemSensor(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "value") val value: Int
)

@JsonClass(generateAdapter = true)
data class SystemFan(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "value") val value: Int
)

@JsonClass(generateAdapter = true)
data class SystemModelInfo(
    @Json(name = "pretty_name") val prettyName: String?,
    @Json(name = "name") val name: String?
)

@JsonClass(generateAdapter = true)
data class SystemConfigResult(
    @Json(name = "uptime") val uptime: String?,
    @Json(name = "uptime_val") val uptimeVal: Long?,
    @Json(name = "firmware_version") val firmwareVersion: String?,
    @Json(name = "sensors") val sensors: List<SystemSensor>?,
    @Json(name = "fans") val fans: List<SystemFan>?,
    @Json(name = "model_info") val modelInfo: SystemModelInfo?
)

@JsonClass(generateAdapter = true)
data class SystemConfigResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "result") val result: SystemConfigResult?
)

@JsonClass(generateAdapter = true)
data class ControlDownloadBody(
    @Json(name = "status") val status: String
)

@JsonClass(generateAdapter = true)
data class DownloadTask(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String,
    @Json(name = "status") val status: String, // "stopped", "downloading", "done", "error"
    @Json(name = "size") val size: Long,
    @Json(name = "rx_bytes") val downloadedSize: Long,
    @Json(name = "queue_pos") val queuePosition: Int,
    @Json(name = "rx_rate") val rxRate: Long? = 0L,
    @Json(name = "tx_rate") val txRate: Long? = 0L
)

@JsonClass(generateAdapter = true)
data class DiskPartition(
    @Json(name = "id") val id: Int,
    @Json(name = "label") val label: String?,
    @Json(name = "total_bytes") val totalBytes: Long,
    @Json(name = "used_bytes") val usedBytes: Long,
    @Json(name = "free_bytes") val freeBytes: Long,
    @Json(name = "state") val state: String?
)

@JsonClass(generateAdapter = true)
data class PartitionsResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "result") val result: List<DiskPartition>?
)

@JsonClass(generateAdapter = true)
data class DownloadListResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "result") val result: List<DownloadTask>?
)

@JsonClass(generateAdapter = true)
data class DownloadFile(
    @Json(name = "id") val id: String,
    @Json(name = "task_id") val taskId: String,
    @Json(name = "name") val name: String,
    @Json(name = "path") val path: String,
    @Json(name = "size") val size: Long,
    @Json(name = "rx") val rx: Long,
    @Json(name = "status") val status: String,
    @Json(name = "priority") val priority: String,
    @Json(name = "mimetype") val mimetype: String? = null
)

@JsonClass(generateAdapter = true)
data class DownloadFileListResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "result") val result: List<DownloadFile>?
)

@JsonClass(generateAdapter = true)
data class AddDownloadResult(
    @Json(name = "id") val id: Int
)

@JsonClass(generateAdapter = true)
data class AddDownloadResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "result") val result: AddDownloadResult?
)

@JsonClass(generateAdapter = true)
data class BooleanResponse(
    @Json(name = "success") val success: Boolean
)

interface FreeboxApi {
    @GET("api_version")
    suspend fun getApiVersion(): Response<ApiVersion>

    @POST("api/v3/login/authorize/")
    suspend fun authorizeApp(@Body request: AuthorizeRequest): Response<AuthorizeResponse>

    @GET("api/v3/login/authorize/{track_id}")
    suspend fun getAuthTrackStatus(@Path("track_id") trackId: Int): Response<TrackResponse>

    @GET("api/v3/login/")
    suspend fun getLoginChallenge(): Response<LoginResponse>

    @POST("api/v3/login/session/")
    suspend fun login(@Body request: SessionRequest): Response<SessionResponse>

    @POST("api/v3/login/logout/")
    suspend fun logout(@Header("X-Fbx-App-Auth") sessionToken: String): Response<CommonResponse>

    @GET("api/v3/wifi/config/")
    suspend fun getWifiConfig(@Header("X-Fbx-App-Auth") sessionToken: String): Response<WifiConfigResponse>

    @PUT("api/v3/wifi/config/")
    suspend fun updateWifiConfig(
        @Header("X-Fbx-App-Auth") sessionToken: String,
        @Body request: WifiConfigUpdateRequest
    ): Response<CommonResponse>

    @POST("api/v3/system/reboot/")
    suspend fun rebootSystem(@Header("X-Fbx-App-Auth") sessionToken: String): Response<CommonResponse>

    @GET("api/v8/system/")
    suspend fun getSystemConfig(@Header("X-Fbx-App-Auth") sessionToken: String): Response<SystemConfigResponse>

    @GET("api/v3/lan/browser/pub/")
    suspend fun getLanHosts(@Header("X-Fbx-App-Auth") sessionToken: String): Response<LanHostsResponse>

    @GET("api/v3/connection/")
    suspend fun getConnectionStatus(@Header("X-Fbx-App-Auth") sessionToken: String): Response<ConnectionResponse>

    @GET("api/v8/fs/ls/{path}")
    suspend fun getFiles(
        @Header("X-Fbx-App-Auth") sessionToken: String,
        @Path("path") path: String
    ): Response<FileListResponse>

    @POST("api/v8/fs/rm/")
    suspend fun removeFiles(
        @Header("X-Fbx-App-Auth") sessionToken: String,
        @Body request: RmRequest
    ): Response<CommonResponse>

    @POST("api/v8/fs/mkdir/")
    suspend fun createDirectory(
        @Header("X-Fbx-App-Auth") sessionToken: String,
        @Body request: MkdirRequest
    ): Response<CommonResponse>

    @POST("api/v8/fs/rename/")
    suspend fun renameFile(
        @Header("X-Fbx-App-Auth") sessionToken: String,
        @Body request: RenameRequest
    ): Response<CommonResponse>

    @GET("api/v8/dl/{path}")
    @Streaming
    suspend fun downloadFile(
        @Header("X-Fbx-App-Auth") sessionToken: String,
        @Path("path") path: String
    ): Response<okhttp3.ResponseBody>

    @GET("api/v8/downloads/")
    suspend fun getDownloads(@Header("X-Fbx-App-Auth") sessionToken: String): Response<DownloadListResponse>

    @GET("api/v8/downloads/{task_id}/files")
    suspend fun getDownloadFiles(
        @Header("X-Fbx-App-Auth") sessionToken: String,
        @Path("task_id") taskId: Int
    ): Response<DownloadFileListResponse>

    @GET("api/v8/storage/partition/")
    suspend fun getPartitions(@Header("X-Fbx-App-Auth") sessionToken: String): Response<PartitionsResponse>

    @POST("api/v8/downloads/add")
    @FormUrlEncoded
    suspend fun addDownload(@Header("X-Fbx-App-Auth") sessionToken: String, @Field("download_url") url: String): Response<AddDownloadResponse>

    @Multipart
    @POST("api/v8/downloads/add")
    suspend fun addDownloadFile(
        @Header("X-Fbx-App-Auth") sessionToken: String,
        @Part downloadFile: okhttp3.MultipartBody.Part
    ): Response<AddDownloadResponse>

    @PUT("api/v8/downloads/{id}")
    suspend fun controlDownload(
        @Header("X-Fbx-App-Auth") sessionToken: String,
        @Path("id") id: Int,
        @Body request: ControlDownloadBody
    ): Response<CommonResponse>

    @DELETE("api/v8/downloads/{id}")
    suspend fun removeDownload(
        @Header("X-Fbx-App-Auth") sessionToken: String,
        @Path("id") id: Int
    ): Response<CommonResponse>
}
