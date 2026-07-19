package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.LanHost
import com.example.data.repository.FreeboxRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppScreen {
    HOME, DEVICES, FILES, DOWNLOADS, SETTINGS
}

enum class ConnectionState {
    DISCONNECTED,
    REGISTERING,
    PENDING_BOX_AUTH,
    LOGGING_IN,
    CONNECTED,
    ERROR
}

data class FreeboxUiState(
    val currentScreen: AppScreen = AppScreen.HOME,
    val boxUrl: String = "http://myiliadbox.iliad.it/",
    val isSimulated: Boolean = true,
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val error: String? = null,
    val feedback: String? = null,
    val isBusy: Boolean = false,
    val isRebooting: Boolean = false,

    // Auth & Registration Info
    val trackId: Int = -1,
    val authStatus: String = "",
    val appToken: String = "",
    val sessionToken: String = "",
    val connectionTesting: Boolean = false,
    val testedApiVersion: String? = null,

    // Connection statistics
    val mediaType: String = "Fiber 5G",
    val downloadSpeedMbps: Float = 842f,
    val uploadSpeedMbps: Float = 294f,

    // Wi-Fi Configuration
    val isWifiEnabled: Boolean = true,
    val wifiActiveBand: String = "2.4GHz + 5GHz",

    // Connected Lan Devices
    val devices: List<LanHost> = emptyList(),

    // Discovered API parameters
    val discoveredApiBaseUrl: String = "/api/",
    val discoveredApiVersionMajor: String = "3",
    val discoveredDeviceName: String = "Freebox Server",
    val discoveredBoxModelName: String = "Freebox Server",

    // Files
    val files: List<com.example.data.api.FileInfo> = emptyList(),
    val currentPath: String = "",
    val isLoadingFiles: Boolean = false,
    val downloadProgress: Map<String, Float> = emptyMap(),
    val downloadTasks: List<com.example.data.api.DownloadTask> = emptyList(),
    val partitions: List<com.example.data.api.DiskPartition> = emptyList(),
    val currentDownloadRate: Long = 0L,
    val currentUploadRate: Long = 0L,
    val screenHistory: List<AppScreen> = listOf(AppScreen.HOME),
    val taskFiles: Map<Int, List<com.example.data.api.DownloadFile>> = emptyMap(),
    val expandedTaskIds: Set<Int> = emptySet(),

    // System Metrics
    val systemUptime: String = "4 giorni, 12 ore",
    val internetConnectionTime: String = "21 giorni, 4 ore",
    val systemFirmwareVersion: String = "4.8.1",
    val systemCpuTemp: String = "48°C (Normale)",
    val systemFanSpeed: String = "1850 giri/min"
)

class FreeboxViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FreeboxRepository(application)

    private val _uiState = MutableStateFlow(FreeboxUiState())
    val uiState: StateFlow<FreeboxUiState> = _uiState.asStateFlow()

    private var refreshJob: Job? = null
    private var authPollingJob: Job? = null

    init {
        // Load initial settings
        _uiState.update {
            it.copy(
                boxUrl = repository.boxUrl.value,
                isSimulated = repository.isSimulated.value,
                trackId = repository.trackId.value,
                discoveredApiBaseUrl = repository.discoveredApiBaseUrl.value,
                discoveredApiVersionMajor = repository.discoveredApiVersionMajor.value,
                discoveredDeviceName = repository.discoveredDeviceName.value,
                discoveredBoxModelName = repository.discoveredBoxModelName.value
            )
        }

        // Keep tokens in sync
        viewModelScope.launch {
            repository.appToken.collect { token ->
                _uiState.update { it.copy(appToken = token) }
            }
        }
        viewModelScope.launch {
            repository.sessionToken.collect { token ->
                _uiState.update { it.copy(sessionToken = token) }
            }
        }
        viewModelScope.launch {
            repository.discoveredApiBaseUrl.collect { value ->
                _uiState.update { it.copy(discoveredApiBaseUrl = value) }
            }
        }
        viewModelScope.launch {
            repository.discoveredApiVersionMajor.collect { value ->
                _uiState.update { it.copy(discoveredApiVersionMajor = value) }
            }
        }
        viewModelScope.launch {
            repository.discoveredDeviceName.collect { value ->
                _uiState.update { it.copy(discoveredDeviceName = value) }
            }
        }
        viewModelScope.launch {
            repository.discoveredBoxModelName.collect { value ->
                _uiState.update { it.copy(discoveredBoxModelName = value) }
            }
        }

        // Accesso automatico o avvio
        autoConnect()
    }

    fun setScreen(screen: AppScreen) {
        _uiState.update { state ->
            if (state.currentScreen == screen) {
                state
            } else if (screen == AppScreen.HOME) {
                state.copy(
                    currentScreen = screen,
                    screenHistory = listOf(AppScreen.HOME)
                )
            } else {
                state.copy(
                    currentScreen = screen,
                    screenHistory = state.screenHistory + screen
                )
            }
        }
    }

    fun navigateBack(): Boolean {
        var handled = false
        _uiState.update { state ->
            if (state.screenHistory.size > 1) {
                val newHistory = state.screenHistory.dropLast(1)
                val prevScreen = newHistory.last()
                handled = true
                state.copy(
                    screenHistory = newHistory,
                    currentScreen = prevScreen
                )
            } else {
                state
            }
        }
        return handled
    }

    fun toggleTaskExpanded(taskId: Int) {
        _uiState.update { state ->
            val alreadyExpanded = state.expandedTaskIds.contains(taskId)
            val newExpanded = if (alreadyExpanded) {
                state.expandedTaskIds - taskId
            } else {
                state.expandedTaskIds + taskId
            }
            state.copy(expandedTaskIds = newExpanded)
        }
        
        // If expanding, fetch files
        val state = _uiState.value
        if (state.expandedTaskIds.contains(taskId)) {
            loadDownloadFiles(taskId)
        }
    }

    fun loadDownloadFiles(taskId: Int) {
        viewModelScope.launch {
            val result = repository.getDownloadFiles(taskId)
            result.onSuccess { files ->
                _uiState.update { state ->
                    val updatedMap = state.taskFiles.toMutableMap()
                    updatedMap[taskId] = files
                    state.copy(taskFiles = updatedMap)
                }
            }.onFailure { ex ->
                Log.e("FreeboxViewModel", "Failed to load files for download task $taskId", ex)
            }
        }
    }

    fun setBoxUrl(url: String) {
        _uiState.update { it.copy(boxUrl = url) }
    }

    fun discoverBox() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, error = null, feedback = "Ricerca del Box nella rete locale...") }
            val result = repository.autoDiscover()
            result.onSuccess { url ->
                _uiState.update { 
                    it.copy(
                        boxUrl = url,
                        isBusy = false, 
                        feedback = "Box trovato automaticamente all'indirizzo $url!"
                    )
                }
                testApiConnection() // Procedi al test automatico
            }.onFailure { ex ->
                _uiState.update { 
                    it.copy(
                        isBusy = false, 
                        error = "Nessun box trovato automaticamente. Inserisci l'URL manualmente."
                    ) 
                }
            }
        }
    }

    fun testApiConnection() {
        viewModelScope.launch {
            _uiState.update { it.copy(connectionTesting = true, error = null, feedback = "Verifica della connessione alle API di Freebox...") }
            repository.setBoxUrl(_uiState.value.boxUrl)
            val result = repository.testConnection()
            result.onSuccess { apiVersion ->
                val info = "Connesso a ${apiVersion.boxModelName ?: "ItalianFreebox"} (API v${apiVersion.apiVersion ?: "3"})"
                _uiState.update {
                    it.copy(
                        connectionTesting = false,
                        testedApiVersion = info,
                        feedback = "Connesso con successo! $info"
                    )
                }
            }.onFailure { ex ->
                _uiState.update {
                    it.copy(
                        connectionTesting = false,
                        testedApiVersion = null,
                        error = "Test di connessione fallito: ${ex.message}. Controlla l'URL o il Wi-Fi."
                    )
                }
            }
        }
    }

    fun toggleSimulation(enabled: Boolean) {
        repository.setIsSimulated(enabled)
        _uiState.update { it.copy(isSimulated = enabled) }
        clearErrorAndFeedback()
        disconnectCurrentSession()
        autoConnect()
    }

    fun loadFiles(path: String = "") {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingFiles = true) }
            val result = repository.getFiles(path)
            _uiState.update { state ->
                if (result.isSuccess) {
                    state.copy(
                        isLoadingFiles = false,
                        currentPath = path,
                        files = result.getOrNull() ?: emptyList(),
                        error = null
                    )
                } else {
                    state.copy(
                        isLoadingFiles = false,
                        error = result.exceptionOrNull()?.message ?: "Impossibile recuperare i file"
                    )
                }
            }
        }
    }


    fun removeFile(path: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val result = repository.removeFile(path)
            _uiState.update { state ->
                state.copy(
                    isBusy = false,
                    error = result.exceptionOrNull()?.message,
                    feedback = if (result.isSuccess) "File rimosso" else null
                )
            }
            if (result.isSuccess) loadFiles(_uiState.value.currentPath)
        }
    }

    fun createDirectory(dirName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val result = repository.createDirectory(_uiState.value.currentPath, dirName)
            _uiState.update { state ->
                state.copy(
                    isBusy = false,
                    error = result.exceptionOrNull()?.message,
                    feedback = if (result.isSuccess) "Cartella creata" else null
                )
            }
            if (result.isSuccess) loadFiles(_uiState.value.currentPath)
        }
    }

    fun renameFile(srcPath: String, newName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val result = repository.renameFile(srcPath, newName)
            _uiState.update { state ->
                state.copy(
                    isBusy = false,
                    error = result.exceptionOrNull()?.message,
                    feedback = if (result.isSuccess) "File rinominato" else null
                )
            }
            if (result.isSuccess) loadFiles(_uiState.value.currentPath)
        }
    }

    fun uploadFileToFsTask(uri: android.net.Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, feedback = "Caricamento in corso...") }
            val result = repository.uploadFileToFs(_uiState.value.currentPath, uri)
            _uiState.update { state ->
                state.copy(
                    isBusy = false,
                    error = result.exceptionOrNull()?.message,
                    feedback = if (result.isSuccess) "File caricato con successo" else "Errore nel caricamento"
                )
            }
            if (result.isSuccess) loadFiles(_uiState.value.currentPath)
        }
    }

    fun getDownloadUrl(path: String): String? {
        return repository.getDownloadUrl(path)
    }
    

    fun downloadFileToDisk(path: String, destFile: java.io.File) {
        viewModelScope.launch {
            _uiState.update { it.copy(downloadProgress = it.downloadProgress + (path to 0f)) }
            val result = repository.downloadFileToDisk(path, destFile) { progress ->
                _uiState.update { it.copy(downloadProgress = it.downloadProgress + (path to progress)) }
            }
            _uiState.update { state ->
                state.copy(
                    error = result.exceptionOrNull()?.message,
                    feedback = if (result.isSuccess) "Download completato in Download/" else "Errore download",
                    downloadProgress = state.downloadProgress - path
                )
            }
        }
    }

    fun downloadToCacheAndOpenFile(path: String, destFile: java.io.File, mimetype: String, context: android.content.Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(downloadProgress = it.downloadProgress + (path to 0f)) }
            val result = repository.downloadFileToDisk(path, destFile) { progress ->
                _uiState.update { it.copy(downloadProgress = it.downloadProgress + (path to progress)) }
            }
            _uiState.update { state ->
                state.copy(
                    error = result.exceptionOrNull()?.message,
                    feedback = if (result.isSuccess) "File pronto per l'apertura" else "Errore apertura",
                    downloadProgress = state.downloadProgress - path
                )
            }
            if (result.isSuccess) {
                val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", destFile)
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
                intent.setDataAndType(uri, mimetype)
                intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                context.startActivity(intent)
            }
        }
    }
    
    fun getDownloadTasks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val result = repository.getDownloads()
            val partitionsResult = repository.getPartitions()
            val downloads = result.getOrNull() ?: emptyList()
            val dlRate = downloads.filter { it.status == "downloading" }.sumOf { it.rxRate ?: 0L }
            val ulRate = downloads.filter { it.status == "downloading" }.sumOf { it.txRate ?: 0L }
            _uiState.update { state ->
                state.copy(
                    isBusy = false,
                    error = result.exceptionOrNull()?.message ?: partitionsResult.exceptionOrNull()?.message,
                    downloadTasks = downloads,
                    partitions = partitionsResult.getOrNull() ?: state.partitions,
                    currentDownloadRate = dlRate,
                    currentUploadRate = ulRate
                )
            }
            // Refresh files for expanded tasks
            _uiState.value.expandedTaskIds.forEach { taskId ->
                loadDownloadFiles(taskId)
            }
        }
    }

    fun addDownloadTask(url: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val result = repository.addDownload(url)
            _uiState.update { state ->
                state.copy(
                    isBusy = false,
                    error = result.exceptionOrNull()?.message,
                    feedback = if (result.isSuccess) "Download aggiunto" else "Errore aggiunta"
                )
            }
            if (result.isSuccess) getDownloadTasks()
        }
    }

    fun addDownloadByFileTask(uri: android.net.Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val result = repository.addDownloadByFile(uri)
            _uiState.update { state ->
                state.copy(
                    isBusy = false,
                    error = result.exceptionOrNull()?.message,
                    feedback = if (result.isSuccess) "Torrent caricato con successo" else "Errore caricamento torrent"
                )
            }
            if (result.isSuccess) getDownloadTasks()
        }
    }

    fun controlDownloadTask(id: Int, status: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val result = repository.controlDownload(id, status)
            _uiState.update { state ->
                state.copy(
                    isBusy = false,
                    error = result.exceptionOrNull()?.message
                )
            }
            if (result.isSuccess) getDownloadTasks()
        }
    }

    fun removeDownloadTask(id: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val result = repository.removeDownload(id)
            _uiState.update { state ->
                state.copy(
                    isBusy = false,
                    error = result.exceptionOrNull()?.message,
                    feedback = if (result.isSuccess) "Download rimosso" else "Errore rimozione"
                )
            }
            if (result.isSuccess) getDownloadTasks()
        }
    }
    
    fun getSessionToken(): String {
        return repository.sessionToken.value
    }


    fun registerApp() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, error = null, feedback = "Avvio della registrazione dell'applicazione...") }
            // Applica l'URL corrente al repository
            repository.setBoxUrl(_uiState.value.boxUrl)

            val result = repository.registerApplication()
            result.onSuccess { authResult ->
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        connectionState = ConnectionState.PENDING_BOX_AUTH,
                        trackId = authResult.trackId,
                        feedback = "Guarda lo schermo della tua ItalianFreebox/Freebox e premi SÌ per autorizzare questa app!"
                    )
                }
                startAuthPolling()
            }.onFailure { ex ->
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        connectionState = ConnectionState.ERROR,
                        error = "Registrazione fallita: ${ex.message}"
                    )
                }
            }
        }
    }

    fun checkAuthStatus() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, error = null) }
            val result = repository.checkAuthorizationStatus()
            result.onSuccess { status ->
                _uiState.update { it.copy(isBusy = false, authStatus = status) }
                when (status) {
                    "granted" -> {
                        _uiState.update { it.copy(feedback = "App autorizzata con successo! Accesso in corso...") }
                        authPollingJob?.cancel()
                        repository.switchToDiscoveredUrl()
                        login()
                    }
                    "pending" -> {
                        _uiState.update { it.copy(feedback = "Ancora in attesa. Autorizza l'applicazione sullo schermo touch del Box.") }
                    }
                    "timeout" -> {
                        _uiState.update { it.copy(connectionState = ConnectionState.ERROR, error = "Tempo scaduto per l'autorizzazione. Riprova.") }
                        authPollingJob?.cancel()
                    }
                    else -> {
                        _uiState.update { it.copy(connectionState = ConnectionState.ERROR, error = "Autorizzazione negata o scaduta ($status).") }
                        authPollingJob?.cancel()
                    }
                }
            }.onFailure { ex ->
                _uiState.update {
                    it.copy(isBusy = false, error = "Impossibile verificare lo stato del box: ${ex.message}")
                }
            }
        }
    }

    private fun startAuthPolling() {
        authPollingJob?.cancel()
        authPollingJob = viewModelScope.launch {
            var attempts = 0
            while (attempts < 60) { // Sondaggio per un massimo di 5 minuti
                delay(5000)
                attempts++
                val result = repository.checkAuthorizationStatus()
                if (result.isSuccess) {
                    val status = result.getOrNull()
                    _uiState.update { it.copy(authStatus = status ?: "") }
                    if (status == "granted") {
                        _uiState.update { it.copy(feedback = "App autorizzata con successo! Accesso in corso...") }
                        repository.switchToDiscoveredUrl()
                        login()
                        break
                    } else if (status != "pending") {
                        _uiState.update { it.copy(connectionState = ConnectionState.ERROR, error = "Registrazione interrotta: lo stato è $status") }
                        break
                    }
                }
            }
        }
    }

    fun login() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, error = null, connectionState = ConnectionState.LOGGING_IN) }
            val result = repository.loginAndOpenSession()
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        connectionState = ConnectionState.CONNECTED,
                        feedback = "Accesso a ItalianFreebox effettuato con successo!"
                    )
                }
                startDataRefreshLoop()
            }.onFailure { ex ->
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        connectionState = ConnectionState.ERROR,
                        error = "Accesso fallito: ${ex.message}. Assicurati che l'app sia autorizzata sul box."
                    )
                }
            }
        }
    }

    fun toggleWifi(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val result = repository.toggleWifi(enabled)
            result.onSuccess { successEnabled ->
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        isWifiEnabled = successEnabled,
                        feedback = if (successEnabled) "Wi-Fi ATTIVATO" else "Wi-Fi DISATTIVATO"
                    )
                }
            }.onFailure { ex ->
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        error = "Impossibile attivare/disattivare il Wi-Fi: ${ex.message}"
                    )
                }
            }
        }
    }

    fun rebootBox() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRebooting = true, feedback = "Invio del comando di riavvio...") }
            val result = repository.rebootBox()
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        isRebooting = false,
                        feedback = "Il Box si sta riavviando! La connessione si interromperà momentaneamente."
                    )
                }
                disconnectCurrentSession()
            }.onFailure { ex ->
                _uiState.update {
                    it.copy(
                        isRebooting = false,
                        error = "Riavvio del box fallito: ${ex.message}"
                    )
                }
            }
        }
    }

    fun refreshAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, error = null) }
            val statsResult = repository.getConnectionStatus()
            val wifiResult = repository.getWifiConfig()
            val devicesResult = repository.getConnectedDevices()
            val downloadsResult = repository.getDownloads()
            val systemConfigResult = repository.getSystemConfig()
            val partitionsResult = repository.getPartitions()
            
            // Also load root files on refresh
            loadFiles(_uiState.value.currentPath)

            statsResult.onSuccess { stats ->
                val downMbps = (stats.bandwidthDown ?: 0L) / 1_000_000f
                val upMbps = (stats.bandwidthUp ?: 0L) / 1_000_000f
                _uiState.update {
                    it.copy(
                        mediaType = when (val m = stats.media?.lowercase() ?: "") {
                            "fiber", "ftth", "gpon", "epon", "ethernet" -> "Fibra FTTH"
                            "xdsl", "adsl", "vdsl" -> "ADSL/VDSL"
                            else -> if (m.isNotEmpty()) m.uppercase() else "Fibra FTTH"
                        },
                        downloadSpeedMbps = if (downMbps > 0) downMbps else 842f,
                        uploadSpeedMbps = if (upMbps > 0) upMbps else 294f
                    )
                }
            }

            wifiResult.onSuccess { wifi ->
                _uiState.update {
                    it.copy(
                        isWifiEnabled = wifi.enabled,
                        wifiActiveBand = wifi.activeBand ?: "2.4GHz + 5GHz"
                    )
                }
            }

            devicesResult.onSuccess { devicesList ->
                _uiState.update {
                    it.copy(devices = devicesList)
                }
            }

            downloadsResult.onSuccess { downloads ->
                val dlRate = downloads.filter { it.status == "downloading" }.sumOf { it.rxRate ?: 0L }
                val ulRate = downloads.filter { it.status == "downloading" }.sumOf { it.txRate ?: 0L }
                _uiState.update {
                    it.copy(
                        downloadTasks = downloads,
                        currentDownloadRate = dlRate,
                        currentUploadRate = ulRate
                    )
                }
                _uiState.value.expandedTaskIds.forEach { taskId ->
                    loadDownloadFiles(taskId)
                }
            }

            partitionsResult.onSuccess { parts ->
                _uiState.update {
                    it.copy(partitions = parts)
                }
            }

            systemConfigResult.onSuccess { sys ->
                val uptimeStr = sys.uptime ?: "4 giorni, 12 ore"
                val firmwareStr = sys.firmwareVersion ?: "4.8.1"
                
                // Find CPU temperature
                val cpuTemp = sys.sensors?.firstOrNull { 
                    it.id.lowercase().contains("cpu") || it.name.lowercase().contains("cpu")
                }?.value ?: sys.sensors?.firstOrNull()?.value ?: 48
                val cpuTempStr = "$cpuTemp°C (${if (cpuTemp > 75) "Alta" else "Normale"})"

                // Find Fan speed
                val fanSpeed = sys.fans?.firstOrNull {
                    it.id.lowercase().contains("fan") || it.id.lowercase().contains("main") || it.name.lowercase().contains("fan") || it.name.lowercase().contains("ventilateur")
                }?.value ?: sys.fans?.firstOrNull()?.value ?: 1850
                val fanSpeedStr = "$fanSpeed giri/min"

                _uiState.update {
                    it.copy(
                        systemUptime = uptimeStr,
                        systemFirmwareVersion = firmwareStr,
                        systemCpuTemp = cpuTempStr,
                        systemFanSpeed = fanSpeedStr
                    )
                }
            }

            _uiState.update { it.copy(isBusy = false, feedback = null) }
        }
    }

    fun disconnectCurrentSession() {
        refreshJob?.cancel()
        authPollingJob?.cancel()
        _uiState.update {
            it.copy(
                connectionState = ConnectionState.DISCONNECTED,
                devices = emptyList()
            )
        }
    }

    fun resetAppAuthorization() {
        disconnectCurrentSession()
        repository.clearCredentials()
        _uiState.update {
            it.copy(
                trackId = -1,
                authStatus = "",
                feedback = "Credenziali dell'app rimosse."
            )
        }
    }

    private fun autoConnect() {
        if (repository.isAuthorized.value && repository.appToken.value.isNotEmpty()) {
            login()
        } else if (repository.trackId.value != -1) {
            _uiState.update { it.copy(connectionState = ConnectionState.PENDING_BOX_AUTH) }
            startAuthPolling()
        } else {
            _uiState.update { it.copy(connectionState = ConnectionState.DISCONNECTED) }
            if (!_uiState.value.isSimulated) {
                discoverBox()
            }
        }
    }

    private fun startDataRefreshLoop() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            while (true) {
                try {
                    refreshAll()
                } catch (e: Exception) {
                    Log.e("FreeboxViewModel", "Auto refresh failed", e)
                }
                delay(12000) // aggiorna ogni 12 secondi
            }
        }
    }

    fun clearErrorAndFeedback() {
        _uiState.update { it.copy(error = null, feedback = null) }
    }

    override fun onCleared() {
        super.onCleared()
        refreshJob?.cancel()
        authPollingJob?.cancel()
    }
}
