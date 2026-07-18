package com.example

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*

import android.app.DownloadManager
import android.net.Uri
import android.widget.Toast
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit

import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.api.LanHost
import com.example.ui.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppContainer()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContainer() {
    val viewModel: FreeboxViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    BackHandler(enabled = uiState.screenHistory.size > 1) {
        viewModel.navigateBack()
    }

    var showRebootDialog by remember { mutableStateOf(false) }
    var showAddDownloadDialog by remember { mutableStateOf(false) }
    var newDownloadUrl by remember { mutableStateOf("") }
    
    var showInitialAuthDialog by remember { mutableStateOf(false) }
    var showInitialAuthConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(1000) // Aspetta che il caricamento dello stato sia completo
        if (viewModel.uiState.value.appToken.isEmpty() && !viewModel.uiState.value.isSimulated) {
            showInitialAuthDialog = true
        }
    }

    val torrentUploadLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            viewModel.addDownloadByFileTask(uri)
            showAddDownloadDialog = false
        }
    }

    // Cancella i messaggi di avviso e feedback con una snackbar o una dissolvenza automatica
    LaunchedEffect(uiState.feedback, uiState.error) {
        if (uiState.feedback != null || uiState.error != null) {
            delay(4000)
            viewModel.clearErrorAndFeedback()
        }
    }

    if (showInitialAuthDialog) {
        AlertDialog(
            onDismissRequest = { showInitialAuthDialog = false },
            title = { Text("Associazione necessaria") },
            text = { Text("Nessuna associazione trovata. Vuoi eseguire l'associazione con il router Iliadbox/Freebox adesso?") },
            confirmButton = {
                Button(onClick = {
                    showInitialAuthDialog = false
                    showInitialAuthConfirmDialog = true
                }) {
                    Text("Sì")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInitialAuthDialog = false }) {
                    Text("Non ora")
                }
            }
        )
    }

    if (showInitialAuthConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showInitialAuthConfirmDialog = false },
            title = { Text("Conferma associazione") },
            text = { Text("Attenzione: per completare l'associazione, assicurati di essere collegato alla rete Wi-Fi del router Iliadbox/Freebox.\n\nDurante la procedura ti verrà richiesto di accettare la connessione toccando la freccia verde o 'SÌ' sul display del router.\n\nVuoi procedere?") },
            confirmButton = {
                Button(onClick = {
                    showInitialAuthConfirmDialog = false
                    viewModel.setScreen(AppScreen.SETTINGS)
                    viewModel.registerApp()
                }) {
                    Text("Procedi")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInitialAuthConfirmDialog = false }) {
                    Text("Annulla")
                }
            }
        )
    }

    if (showAddDownloadDialog) {
        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
        AlertDialog(
            onDismissRequest = { showAddDownloadDialog = false },
            title = {
                Text(
                    text = "Aggiungi nuovo download",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Inserisci l'URL di un file, file Torrent o Magnet link:",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    OutlinedTextField(
                        value = newDownloadUrl,
                        onValueChange = { newDownloadUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("https://... o magnet:...") },
                        singleLine = false,
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    Button(
                        onClick = {
                            val clipText = clipboardManager.getText()?.text
                            if (!clipText.isNullOrBlank()) {
                                newDownloadUrl = clipText
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("📋 Incolla dagli appunti")
                    }

                    Button(
                        onClick = { torrentUploadLauncher.launch("*/*") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("📁 Carica file .torrent")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newDownloadUrl.isNotBlank()) {
                            viewModel.addDownloadTask(newDownloadUrl)
                            newDownloadUrl = ""
                            showAddDownloadDialog = false
                        }
                    },
                    enabled = newDownloadUrl.isNotBlank(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Avvia download")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        newDownloadUrl = ""
                        showAddDownloadDialog = false
                    }
                ) {
                    Text("Annulla")
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            IliadBottomBar(
                currentScreen = uiState.currentScreen,
                onSelectScreen = { viewModel.setScreen(it) }
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isWideScreen = maxWidth > 600.dp

            Column(modifier = Modifier.fillMaxSize()) {
                // Intestazione dell'applicazione
                IliadHeader(
                    isSimulated = uiState.isSimulated,
                    currentScreen = uiState.currentScreen,
                    onActionClick = {
                        if (uiState.currentScreen == AppScreen.DOWNLOADS) {
                            showAddDownloadDialog = true
                        } else {
                            viewModel.refreshAll()
                        }
                    }
                )

                // Notifiche di feedback / errore
                AnimatedVisibility(visible = uiState.error != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFDAD6)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⚠️",
                                fontSize = 18.sp,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = uiState.error ?: "",
                                color = Color(0xFF410002),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = uiState.feedback != null && uiState.error == null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ℹ️",
                                fontSize = 18.sp,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = uiState.feedback ?: "",
                                color = Color(0xFF1B5E20),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Area del corpo principale basata sulla larghezza dello schermo (Design Adattivo)
                if (isWideScreen) {
                    // Schermo diviso per tablet / modalità orizzontale
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    ) {
                        // Riquadro sinistro (Controlli della schermata corrente)
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(1.1f)
                        ) {
                            RenderScreenContent(
                                screen = uiState.currentScreen,
                                uiState = uiState,
                                viewModel = viewModel,
                                onRebootRequest = { showRebootDialog = true }
                            )
                        }

                        // Riquadro destro (Mostra sempre l'elenco dei dispositivi come riquadro di supporto per una visualizzazione rapida)
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(0.9f)
                                .border(
                                    BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    shape = RoundedCornerShape(topStart = 24.dp)
                                )
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(16.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "DISPOSITIVI CONNESSI",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary,
                                        letterSpacing = 1.sp
                                    )
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    ) {
                                        Text(
                                            text = uiState.devices.count { it.active == true }.toString(),
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                DevicesList(devices = uiState.devices)
                            }
                        }
                    }
                } else {
                    // Viewport standard verticale per dispositivi mobili
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    ) {
                        RenderScreenContent(
                            screen = uiState.currentScreen,
                            uiState = uiState,
                            viewModel = viewModel,
                            onRebootRequest = { showRebootDialog = true }
                        )
                    }
                }
            }
        }
    }

    // Dialoghi / Popup
    if (showRebootDialog) {
        AlertDialog(
            onDismissRequest = { showRebootDialog = false },
            title = { Text(text = "Riavviare il Box?") },
            text = { Text(text = "Sei sicuro di voler riavviare l'ItalianFreebox? La tua connessione internet si disconnetterà temporaneamente.") },
            confirmButton = {
                Button(
                    onClick = {
                        showRebootDialog = false
                        viewModel.rebootBox()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Riavvia", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRebootDialog = false }) {
                    Text("Annulla", color = MaterialTheme.colorScheme.secondary)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
fun RenderScreenContent(
    screen: AppScreen,
    uiState: FreeboxUiState,
    viewModel: FreeboxViewModel,
    onRebootRequest: () -> Unit
) {
    when (screen) {
        AppScreen.HOME -> HomeScreen(
            uiState = uiState,
            viewModel = viewModel
        )
        AppScreen.DEVICES -> DevicesScreen(
            uiState = uiState,
            viewModel = viewModel
        )
        AppScreen.FILES -> FilesScreen(uiState, viewModel)
        AppScreen.DOWNLOADS -> DownloadsScreen(uiState, viewModel)
        AppScreen.SETTINGS -> SettingsScreen(
            uiState = uiState,
            viewModel = viewModel
        )
    }
}

// ==========================================
// SCHERMATA 1: SCHERMATA HOME
// ==========================================
@Composable
fun HomeScreen(
    uiState: FreeboxUiState,
    viewModel: FreeboxViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // BANNER DEMO
        if (uiState.isSimulated) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("💡", fontSize = 24.sp, modifier = Modifier.padding(end = 12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Modalità Simulazione Attiva",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Per configurare un box fisico, vai alla scheda Impostazioni (⚙️) e registra la tua ItalianFreebox.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // SCHEDA DI CONNETTIVITÀ
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 2.dp, shape = RoundedCornerShape(32.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shape = RoundedCornerShape(32.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // Intestazione dello Stato
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Stato Corrente",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (uiState.discoveredBoxModelName.isNotEmpty()) {
                                Text(
                                    text = " • ${uiState.discoveredBoxModelName}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            val statusColor = when (uiState.connectionState) {
                                ConnectionState.CONNECTED -> Color(0xFF4CAF50)
                                ConnectionState.PENDING_BOX_AUTH -> Color(0xFFFF9800)
                                ConnectionState.LOGGING_IN, ConnectionState.REGISTERING -> Color(0xFF2196F3)
                                else -> Color(0xFFE62C2E)
                            }
                            val statusText = when (uiState.connectionState) {
                                ConnectionState.CONNECTED -> stringResource(R.string.status_connected)
                                ConnectionState.PENDING_BOX_AUTH -> stringResource(R.string.status_pending)
                                ConnectionState.LOGGING_IN -> stringResource(R.string.status_connecting)
                                ConnectionState.REGISTERING -> "Registrazione..."
                                else -> stringResource(R.string.status_disconnected)
                            }

                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(statusColor)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = statusText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Badge Tecnologico
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = uiState.mediaType,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Griglia delle Velocità
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Scaricamento
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "DOWNLOAD",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "%.0f".format(uiState.downloadSpeedMbps),
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = " Mbps",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 3.dp)
                                )
                            }
                        }
                    }

                    // Caricamento
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "UPLOAD",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "%.0f".format(uiState.uploadSpeedMbps),
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = " Mbps",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // AZIONI PRINCIPALI (MANDATORY)
        Text(
            text = "AZIONI PRINCIPALI",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 4.dp, top = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Gestione File Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(110.dp)
                    .clickable { viewModel.setScreen(AppScreen.FILES) },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("📁", fontSize = 32.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Gestione File",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Download Manager Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(110.dp)
                    .clickable { viewModel.setScreen(AppScreen.DOWNLOADS) },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("📥", fontSize = 32.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Download",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        // ANTEPRIMA DEI DISPOSITIVI CONNESSI
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "DISPOSITIVI CONNESSI",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
            TextButton(onClick = { viewModel.setScreen(AppScreen.DEVICES) }) {
                Text("Vedi tutti", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 1.dp, shape = RoundedCornerShape(28.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (uiState.devices.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📱", fontSize = 32.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Nessun dispositivo attivo rilevato",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    // Limita l'anteprima a 3 dispositivi
                    val activeDevices = uiState.devices.take(3)
                    for ((index, host) in activeDevices.withIndex()) {
                        DeviceRow(host = host)
                        if (index < activeDevices.size - 1) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ==========================================
// SCHERMATA 2: SCHERMATA DETTAGLIATA DISPOSITIVI
// ==========================================
@Composable
fun DevicesScreen(
    uiState: FreeboxUiState,
    viewModel: FreeboxViewModel
) {
    var searchQuery by remember { mutableStateOf("") }
    var showOnlyActive by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Dispositivi di rete",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = { viewModel.refreshAll() }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Aggiorna elenco",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Barra di Ricerca
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Cerca dispositivi per nome...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Icona Cerca") },
            shape = RoundedCornerShape(100.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Chip di Filtro
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = !showOnlyActive,
                onClick = { showOnlyActive = false },
                label = { Text("Tutti (${uiState.devices.size})") }
            )
            FilterChip(
                selected = showOnlyActive,
                onClick = { showOnlyActive = true },
                label = { Text("Attivi (${uiState.devices.count { it.active == true }})") }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Elenco dei dispositivi
        val filtered = uiState.devices.filter {
            val matchesSearch = it.primaryName?.contains(searchQuery, ignoreCase = true) ?: false
            val matchesActive = !showOnlyActive || (it.active == true)
            matchesSearch && matchesActive
        }

        Box(modifier = Modifier.weight(1f)) {
            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔍", fontSize = 40.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Nessun dispositivo corrispondente trovato",
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                DevicesList(devices = filtered)
            }
        }
    }
}

@Composable
fun DevicesList(devices: List<LanHost>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(devices, key = { it.id }) { host ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(modifier = Modifier.padding(14.dp)) {
                    DeviceRow(host = host)
                }
            }
        }
    }
}

// ==========================================
// SCHERMATA 3: SCHERMATA DI SISTEMA
// ==========================================
// ==========================================
// SCHERMATA FILES: GESTIONE FILE
// ==========================================

@Composable
fun FilesScreen(uiState: FreeboxUiState, viewModel: FreeboxViewModel) {
    var showCreateDirDialog by remember { mutableStateOf(false) }
    var dirNameInput by remember { mutableStateOf("") }
    
    val context = LocalContext.current
    val uploadLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            viewModel.uploadFileToFsTask(uri)
        }
    }

    if (showCreateDirDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDirDialog = false },
            title = { Text("Nuova Cartella") },
            text = {
                OutlinedTextField(
                    value = dirNameInput,
                    onValueChange = { dirNameInput = it },
                    label = { Text("Nome cartella") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (dirNameInput.isNotBlank()) {
                        viewModel.createDirectory(dirNameInput)
                    }
                    showCreateDirDialog = false
                    dirNameInput = ""
                }) { Text("Crea") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDirDialog = false }) { Text("Annulla") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (uiState.currentPath.isNotEmpty()) {
                    IconButton(onClick = {
                        val parentPath = uiState.currentPath.substringBeforeLast("/", "")
                        viewModel.loadFiles(parentPath)
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Indietro")
                    }
                }
                Text(
                    text = if (uiState.currentPath.isEmpty()) "Files" else uiState.currentPath.substringAfterLast("/"),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Row {
                IconButton(onClick = { uploadLauncher.launch("*/*") }) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Upload File", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { showCreateDirDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Nuova Cartella", tint = MaterialTheme.colorScheme.primary)
                }
                if (uiState.isLoadingFiles) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(4.dp))
                }
            }
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (uiState.files.isEmpty() && !uiState.isLoadingFiles) {
                    Text("Cartella vuota.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    for (file in uiState.files) {
                        val icon = if (file.type == "dir") "📁" else {
                            if (file.mimetype?.startsWith("video") == true) "🎬"
                            else if (file.mimetype?.startsWith("audio") == true) "🎵"
                            else if (file.mimetype?.startsWith("image") == true) "🖼️"
                            else "📄"
                        }
                        val info = if (file.type == "dir") "Cartella" else {
                            val bytes = file.size ?: 0
                            when {
                                bytes < 1024 -> "$bytes B"
                                bytes < 1024 * 1024 -> "${bytes / 1024} KB"
                                else -> String.format("%.1f MB", bytes.toDouble() / 1024 / 1024)
                            }
                        }
                        FileRow(
                            icon = icon, 
                            name = file.name, 
                            info = info,
                            isDir = file.type == "dir",
                            fullPath = if (uiState.currentPath.isEmpty()) file.name else "${uiState.currentPath}/${file.name}",
                            size = file.size ?: 0,
                            mimetype = file.mimetype ?: "",
                            viewModel = viewModel,
                            uiState = uiState,
                            onClick = {
                                if (file.type == "dir") {
                                    val newPath = if (uiState.currentPath.isEmpty()) file.name else "${uiState.currentPath}/${file.name}"
                                    viewModel.loadFiles(newPath)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FileRow(
    icon: String, 
    name: String, 
    info: String, 
    isDir: Boolean,
    fullPath: String,
    size: Long,
    mimetype: String,
    viewModel: FreeboxViewModel,
    uiState: FreeboxUiState,
    onClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameInput by remember { mutableStateOf(name) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showDownloadDialog by remember { mutableStateOf(false) }
    
    val context = LocalContext.current

    if (showDownloadDialog) {
        AlertDialog(
            onDismissRequest = { showDownloadDialog = false },
            title = { Text("Download File Grande") },
            text = { Text("Il file è di circa ${size / 1024 / 1024} MB. Vuoi procedere con il download?") },
            confirmButton = {
                TextButton(onClick = {
                    val destDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val destFile = java.io.File(destDir, name)
                    viewModel.downloadFileToDisk(fullPath, destFile)
                    Toast.makeText(context, "Download avviato...", Toast.LENGTH_SHORT).show()
                    showDownloadDialog = false
                }) { Text("Download") }
            },
            dismissButton = {
                TextButton(onClick = { showDownloadDialog = false }) { Text("Annulla") }
            }
        )
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rinomina") },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text("Nuovo nome") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (renameInput.isNotBlank() && renameInput != name) {
                        viewModel.renameFile(fullPath, renameInput)
                    }
                    showRenameDialog = false
                }) { Text("Rinomina") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Annulla") }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Conferma eliminazione") },
            text = { Text("Vuoi davvero eliminare $name?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeFile(fullPath)
                    showDeleteConfirm = false
                }) { Text("Elimina", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Annulla") }
            }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (isDir) {
                    onClick()
                } else {
                    if (size < 50 * 1024 * 1024) {
                        // Open directly
                        val destFile = java.io.File(context.cacheDir, name)
                        viewModel.downloadToCacheAndOpenFile(fullPath, destFile, mimetype, context)
                    } else {
                        showDownloadDialog = true
                    }
                }
            }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.surface, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = icon, fontSize = 24.sp)
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = info,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val progress = uiState.downloadProgress[fullPath]
            if (progress != null) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )
            }
        }

        Box {
            IconButton(onClick = { expanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Opzioni")
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                if (!isDir) {
                    DropdownMenuItem(
                        text = { Text("Scarica") },
                        leadingIcon = { Icon(Icons.Default.KeyboardArrowDown, contentDescription = null) },
                        onClick = {
                            expanded = false
                            val destDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                            val destFile = java.io.File(destDir, name)
                            viewModel.downloadFileToDisk(fullPath, destFile)
                            Toast.makeText(context, "Download avviato...", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Rinomina") },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                    onClick = {
                        expanded = false
                        renameInput = name
                        showRenameDialog = true
                    }
                )
                DropdownMenuItem(
                    text = { Text("Elimina") },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    onClick = {
                        expanded = false
                        showDeleteConfirm = true
                    }
                )
            }
        }
    }
}


// ==========================================
// SCHERMATA 4: SCHERMATA IMPOSTAZIONI (API & DNS)
// ==========================================
@Composable
fun SettingsScreen(
    uiState: FreeboxUiState,
    viewModel: FreeboxViewModel
) {
    var boxUrlInput by remember { mutableStateOf(uiState.boxUrl) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Impostazioni di connessione del router",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Scheda di configurazione DNS e API
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "ENDPOINT DI CONNESSIONE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )

                Text(
                    text = "Puoi accedere al tuo Box in locale o da remoto fuori dalla rete domestica utilizzando il tuo indirizzo dnsApi personalizzato.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = boxUrlInput,
                    onValueChange = {
                        boxUrlInput = it
                        viewModel.setBoxUrl(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("URL API / DNS dell'ItalianFreebox") },
                    placeholder = { Text("http://myiliadbox.iliad.it/") },
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done
                    ),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.discoverBox() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(100.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        ),
                        enabled = !uiState.connectionTesting && !uiState.isBusy
                    ) {
                        Text("Scopri in Rete Locale", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                    }

                    Button(
                        onClick = { viewModel.testApiConnection() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(100.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        enabled = !uiState.connectionTesting && !uiState.isBusy
                    ) {
                        if (uiState.connectionTesting) {
                            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp).padding(end = 4.dp))
                            Text("Test...", fontSize = 12.sp)
                        } else {
                            Text("Testa Connessione API", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                        }
                    }
                }

                // Informazioni sul risultato del test
                uiState.testedApiVersion?.let { info ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE8F5E9))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text("🟢", fontSize = 12.sp, modifier = Modifier.padding(end = 8.dp))
                        Text(info, color = Color(0xFF1B5E20), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }

                // Interruttore per la modalità simulata
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Abilita modalità demo / simulazione",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Visualizza in anteprima e testa istantaneamente le funzionalità dell'app senza collegare un router reale.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.isSimulated,
                        onCheckedChange = { viewModel.toggleSimulation(it) }
                    )
                }
            }
        }

        if (uiState.discoveredBoxModelName.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "DETTAGLI ROUTER RILEVATO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Modello", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(uiState.discoveredBoxModelName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Nome Dispositivo", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(uiState.discoveredDeviceName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Percorso Base API", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(uiState.discoveredApiBaseUrl, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Versione Principale API", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("v${uiState.discoveredApiVersionMajor}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // HANDSHAKE TIMELINE CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "HANDSHAKE DI AUTENTICAZIONE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                val step1Completed = uiState.testedApiVersion != null || uiState.trackId != -1
                val step2Completed = uiState.trackId != -1
                val step3Completed = uiState.authStatus == "granted" || uiState.connectionState == ConnectionState.CONNECTED
                val step4Completed = uiState.connectionState == ConnectionState.CONNECTED

                // PASSO 1: VERIFICA URL
                HandshakeStep(
                    stepNumber = 1,
                    title = "Rileva URL del Router",
                    description = "Verifica l'accessibilità dell'endpoint API dell'ItalianFreebox.",
                    statusText = if (step1Completed) "VERIFICATO" else "IN ATTESA",
                    statusColor = if (step1Completed) Color(0xFF4CAF50) else Color(0xFFFF9800),
                    isActive = !step1Completed,
                    isCompleted = step1Completed,
                    showLine = true
                ) {
                    // Breve indicatore dello stato del passo 1
                    Text(
                        text = "Endpoint: " + (if (uiState.isSimulated) "Local Host Simulato" else uiState.boxUrl),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // PASSO 2: REGISTRAZIONE APP
                HandshakeStep(
                    stepNumber = 2,
                    title = "Registra Applicazione",
                    description = "Richiedi le credenziali del token dell'app e un ID di tracciamento dal router.",
                    statusText = if (step2Completed) "REGISTRATO" else if (step1Completed) "PRONTO" else "BLOCCATO",
                    statusColor = if (step2Completed) Color(0xFF4CAF50) else if (step1Completed) Color(0xFF2196F3) else MaterialTheme.colorScheme.onSurfaceVariant,
                    isActive = step1Completed && !step2Completed,
                    isCompleted = step2Completed,
                    showLine = true
                ) {
                    if (step2Completed) {
                        Column {
                            Text("ID Tracciamento: ${uiState.trackId}", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            Text("Token App: Cache di Sicurezza Mascherata", fontSize = 11.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Medium)
                        }
                    } else {
                        Button(
                            onClick = { viewModel.registerApp() },
                            modifier = Modifier.fillMaxWidth().testTag("submit_button"),
                            shape = RoundedCornerShape(100.dp),
                            enabled = step1Completed && !uiState.isBusy,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            if (uiState.isBusy && uiState.connectionState == ConnectionState.REGISTERING) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                            } else {
                                Text("Registra e Richiedi Token App", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // PASSO 3: APPROVAZIONE LCD
                val isStep3Active = uiState.connectionState == ConnectionState.PENDING_BOX_AUTH || (step2Completed && !step3Completed)
                val liveStatusColor = when (uiState.authStatus) {
                    "granted" -> Color(0xFF4CAF50)
                    "pending" -> Color(0xFFFF9800)
                    "denied" -> Color(0xFFE62C2E)
                    "timeout" -> Color(0xFF757575)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                HandshakeStep(
                    stepNumber = 3,
                    title = "Approvazione LCD Touchscreen",
                    description = "Guarda lo schermo della tua ItalianFreebox/Freebox e premi SÌ per autorizzare questa app di gestione.",
                    statusText = if (step3Completed) "APPROVATO" else if (uiState.authStatus.isNotEmpty()) {
                        when (uiState.authStatus) {
                            "granted" -> "APPROVATO"
                            "pending" -> "IN ATTESA"
                            "denied" -> "NEGATO"
                            "timeout" -> "TEMPO SCADUTO"
                            else -> uiState.authStatus.uppercase()
                        }
                    } else if (isStep3Active) "IN ATTESA..." else "BLOCCATO",
                    statusColor = if (step3Completed) Color(0xFF4CAF50) else liveStatusColor,
                    isActive = isStep3Active,
                    isCompleted = step3Completed,
                    showLine = true
                ) {
                    if (isStep3Active && !step3Completed) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Aggiornamento dello stato
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(liveStatusColor.copy(alpha = 0.12f))
                                    .padding(12.dp)
                            ) {
                                val pollingStatusText = when (uiState.authStatus.ifEmpty { "pending" }) {
                                    "granted" -> "APPROVATO"
                                    "pending" -> "IN ATTESA"
                                    "denied" -> "NEGATO"
                                    "timeout" -> "TEMPO SCADUTO"
                                    else -> uiState.authStatus.uppercase()
                                }
                                Text(
                                    text = "Stato Corrente del Sondaggio: $pollingStatusText",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = liveStatusColor
                                )
                            }
                            
                            Button(
                                onClick = { viewModel.checkAuthStatus() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(100.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                enabled = !uiState.isBusy
                            ) {
                                Text("Verifica Progresso Ora", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    } else if (step3Completed) {
                        Text("Autorizzato con successo!", color = Color(0xFF2E7D32), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Text("In attesa della registrazione dell'applicazione.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // PASSO 4: LOGIN / AVVIA SESSIONE
                HandshakeStep(
                    stepNumber = 4,
                    title = "Autenticazione Sessione",
                    description = "Stabilisci un token di sessione temporaneo dopo l'accesso utilizzando la firma HMAC-SHA1.",
                    statusText = if (step4Completed) "CONNESSO" else if (step3Completed) "PRONTO" else "BLOCCATO",
                    statusColor = if (step4Completed) Color(0xFF4CAF50) else if (step3Completed) Color(0xFF2196F3) else MaterialTheme.colorScheme.onSurfaceVariant,
                    isActive = step3Completed && !step4Completed,
                    isCompleted = step4Completed,
                    showLine = false
                ) {
                    if (step4Completed) {
                        Column {
                            Text("Sessione Attiva", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Token: Sessione di Sicurezza Mascherata", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    } else {
                        Button(
                            onClick = { viewModel.login() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(100.dp),
                            enabled = step3Completed && !uiState.isBusy,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            if (uiState.isBusy && uiState.connectionState == ConnectionState.LOGGING_IN) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                            } else {
                                Text("Accedi e Apri Sessione", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // SCHEDA DI GESTIONE CACHE TOKEN
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "GESTIONE CACHE TOKEN",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )

                // Riga Token App
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Token App (Permanente)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(
                            text = if (uiState.appToken.isNotEmpty()) {
                                "Salvato: " + uiState.appToken.take(8) + "..." + uiState.appToken.takeLast(6)
                            } else {
                                "Nessuno"
                            },
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(if (uiState.appToken.isNotEmpty()) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (uiState.appToken.isNotEmpty()) "PROTETTO" else "MANCANTE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.appToken.isNotEmpty()) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                // Riga Token Sessione
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Token Sessione (Temporaneo)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(
                            text = if (uiState.sessionToken.isNotEmpty()) {
                                "Attivo: " + uiState.sessionToken.take(8) + "..." + uiState.sessionToken.takeLast(6)
                            } else {
                                "Nessuno"
                            },
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(if (uiState.sessionToken.isNotEmpty()) Color(0xFFE3F2FD) else MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (uiState.sessionToken.isNotEmpty()) "ATTIVO" else "INATTIVO",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.sessionToken.isNotEmpty()) Color(0xFF1565C0) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Login di sessione a attivazione manuale
                    Button(
                        onClick = { viewModel.login() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(100.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                        enabled = !uiState.isBusy && uiState.appToken.isNotEmpty()
                    ) {
                        Text("Rinnova Sessione", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Reimposta credenziali
                    Button(
                        onClick = { viewModel.resetAppAuthorization() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(100.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFDAD6), contentColor = Color(0xFF410002)),
                        enabled = !uiState.isBusy
                    ) {
                        Text("Reimposta Token", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Statistiche hardware
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "PRESTAZIONI HARDWARE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Temp CPU", fontSize = 13.sp)
                    Text(
                        text = uiState.systemCpuTemp,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (uiState.systemCpuTemp.lowercase().contains("alta")) Color(0xFFC62828) else Color(0xFF1B5E20)
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Velocità Ventola", fontSize = 13.sp)
                    Text(uiState.systemFanSpeed, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Tempo di attività", fontSize = 13.sp)
                    Text(uiState.systemUptime, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        // Impostazioni di Sistema
        Button(
            onClick = { viewModel.rebootBox() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
            shape = RoundedCornerShape(100.dp)
        ) {
            Text("🔄 Riavvia ItalianFreebox", fontWeight = FontWeight.Bold)
        }
    }
}

// Reusable stepper timeline step
@Composable
fun HandshakeStep(
    stepNumber: Int,
    title: String,
    description: String,
    statusText: String,
    statusColor: Color,
    isActive: Boolean,
    isCompleted: Boolean,
    showLine: Boolean = true,
    content: @Composable () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Step indicator circle/line
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                             isCompleted -> Color(0xFF4CAF50)
                             isActive -> MaterialTheme.colorScheme.primary
                             else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(Icons.Default.Check, contentDescription = "Completato", tint = Color.White, modifier = Modifier.size(16.dp))
                } else {
                    Text(
                        text = stepNumber.toString(),
                        color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (showLine) {
                // Vertical timeline connector line
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(80.dp)
                        .background(
                            if (isCompleted) Color(0xFF4CAF50).copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
                        )
                )
            }
        }

        // Content
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (isActive || isCompleted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (statusText.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(statusColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = statusText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }
            }

            Text(
                text = description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
            )

            content()
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

// ==========================================
// REUSABLE UI SUB-COMPONENTS
// ==========================================
@Composable
fun IliadHeader(
    isSimulated: Boolean,
    currentScreen: AppScreen,
    onActionClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App logo & name
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE62C2E)),
                contentAlignment = Alignment.Center
            ) {
                // Curved TouchScreen representation inside logo
                Box(
                    modifier = Modifier
                        .size(24.dp, 8.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .background(Color.White)
                )
            }
            Text(
                text = "ItalianFreebox",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Action button (+ or Refresh)
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFDAD6))
                .border(2.dp, Color.White, CircleShape)
                .clickable { onActionClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (currentScreen == AppScreen.DOWNLOADS) Icons.Default.Add else Icons.Default.Refresh,
                contentDescription = if (currentScreen == AppScreen.DOWNLOADS) "Aggiungi download" else "Aggiorna",
                tint = Color(0xFF410002),
                modifier = Modifier.size(if (currentScreen == AppScreen.DOWNLOADS) 24.dp else 24.dp)
            )
        }
    }
}

@Composable
fun DeviceRow(host: LanHost) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Icon based on type
        val deviceEmoji = when (host.hostType?.lowercase()) {
            "smartphone" -> "📱"
            "workstation", "laptop" -> "💻"
            "tv" -> "📺"
            "printer" -> "🖨️"
            "tablet" -> "📶"
            else -> "🔌"
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(deviceEmoji, fontSize = 20.sp)
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = host.primaryName ?: "Host Sconosciuto",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val activeText = if (host.active == true) "Attivo ora" else "Offline"
            val connType = host.connectivityType ?: "wifi"
            Text(
                text = "$activeText • $connType",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Ping badge / Active indicator
        if (host.active == true) {
            Text(
                text = "12ms",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B5E20)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            )
        }
    }
}

@Composable
fun IliadBottomBar(
    currentScreen: AppScreen,
    onSelectScreen: (AppScreen) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        modifier = Modifier.border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline))
    ) {
        val items = listOf(
            Triple(AppScreen.HOME, stringResource(R.string.title_home), "🏠"),
            Triple(AppScreen.DEVICES, stringResource(R.string.title_devices), "💻"),
            Triple(AppScreen.FILES, stringResource(R.string.title_files), "📁"),
            Triple(AppScreen.DOWNLOADS, stringResource(R.string.title_downloads), "📥"),
            Triple(AppScreen.SETTINGS, stringResource(R.string.title_settings), "⚙️")
        )

        for (item in items) {
            val selected = currentScreen == item.first
            NavigationBarItem(
                selected = selected,
                onClick = { onSelectScreen(item.first) },
                icon = {
                    Box(
                        modifier = Modifier
                            .width(64.dp)
                            .height(32.dp)
                            .clip(RoundedCornerShape(100.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.third,
                            fontSize = 18.sp,
                            modifier = if (!selected) Modifier.background(Color.Transparent) else Modifier
                        )
                    }
                },
                label = {
                    Text(
                        text = item.second,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}

@Composable
fun DownloadsScreen(
    uiState: FreeboxUiState,
    viewModel: FreeboxViewModel
) {
    // Automatically load downloads when entering the screen
    LaunchedEffect(Unit) {
        viewModel.getDownloadTasks()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Card displaying current transfer statistics (compact actual speeds)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Download Speed
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⬇️", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${formatBytes(uiState.currentDownloadRate)}/s",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Upload Speed
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⬆️", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${formatBytes(uiState.currentUploadRate)}/s",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                // Count active/total
                val downloadingCount = uiState.downloadTasks.count { it.status == "downloading" }
                val totalCount = uiState.downloadTasks.size
                Text(
                    text = "$downloadingCount attivi di $totalCount",
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Card displaying disk space
        if (uiState.partitions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Text(
                        text = "SPAZIO DI ARCHIVIAZIONE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    uiState.partitions.forEachIndexed { index, partition ->
                        if (index > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        val totalGB = partition.totalBytes / 1_000_000_000f
                        val freeGB = partition.freeBytes / 1_000_000_000f
                        val usedGB = partition.usedBytes / 1_000_000_000f
                        val percentage = if (partition.totalBytes > 0) {
                            partition.usedBytes.toFloat() / partition.totalBytes.toFloat()
                        } else {
                            0f
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("💽", fontSize = 18.sp, modifier = Modifier.padding(end = 8.dp))
                                Column {
                                    Text(
                                        text = partition.label ?: "Disque dur",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Stato: ${partition.state ?: "mounted"}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "%.1f GB liberi di %.1f GB".format(freeGB, totalGB),
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "%.1f GB usati".format(usedGB),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Progress bar for used space
                        LinearProgressIndicator(
                            progress = { percentage },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (percentage > 0.9f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // List of Download Tasks
        Text(
            text = "CODA DEI TASK",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Box(modifier = Modifier.weight(1f)) {
            if (uiState.downloadTasks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📥", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Nessun download in coda",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Premi il tasto '+' in alto per iniziare",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.downloadTasks, key = { it.id }) { task ->
                        DownloadTaskCard(
                            task = task,
                            isExpanded = uiState.expandedTaskIds.contains(task.id),
                            onToggleExpand = {
                                viewModel.toggleTaskExpanded(task.id)
                            },
                            taskFiles = uiState.taskFiles[task.id],
                            onControl = { action ->
                                viewModel.controlDownloadTask(task.id, action)
                            },
                            onRemove = {
                                viewModel.removeDownloadTask(task.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadTaskCard(
    task: com.example.data.api.DownloadTask,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    taskFiles: List<com.example.data.api.DownloadFile>?,
    onControl: (String) -> Unit,
    onRemove: () -> Unit
) {
    val progress = if (task.size > 0L) task.downloadedSize.toFloat() / task.size else 0f
    val progressPercent = (progress * 100).toInt().coerceIn(0, 100)

    val isDone = task.status == "done"
    val isStopped = task.status == "stopped" || task.status == "stopped_error"
    val isDownloading = task.status == "downloading"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 1.dp, shape = RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Task header (Name and Action buttons)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onToggleExpand() }
                ) {
                    Text(
                        text = task.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val (statusEmoji, statusText, statusColor) = when {
                            isDone -> Triple("✅", "Completato", Color(0xFF1B5E20))
                            isStopped -> Triple("⏸️", "In pausa", Color(0xFFE65100))
                            isDownloading -> {
                                val rateStr = if ((task.rxRate ?: 0L) > 0L) " (${formatBytes(task.rxRate!!)}/s)" else ""
                                Triple("⚡", "Download in corso$rateStr", MaterialTheme.colorScheme.primary)
                            }
                            else -> Triple("❌", "Errore: ${task.status}", MaterialTheme.colorScheme.error)
                        }

                        Text(
                            text = "$statusEmoji $statusText",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = statusColor
                        )

                        Text(
                            text = " • Pos: ${task.queuePosition}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleExpand,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Dettagli file",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Elimina download",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar and Percentage
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = if (isDone) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outlineVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Transfer stats (downloaded / total size) and Play/Pause controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${formatBytes(task.downloadedSize)} di ${formatBytes(task.size)} ($progressPercent%)",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace
                )

                // Control action buttons (Play/Pause)
                if (!isDone) {
                    if (isDownloading) {
                        IconButton(
                            onClick = { onControl("stopped") },
                            modifier = Modifier
                                .size(32.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                        ) {
                            Text("⏸️", fontSize = 12.sp)
                        }
                    } else if (isStopped) {
                        IconButton(
                            onClick = { onControl("downloading") },
                            modifier = Modifier
                                .size(32.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                        ) {
                            Text("▶️", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Expanded files list treeview section
            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "FILE CONTENUTI (${taskFiles?.size ?: "..."})",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                if (taskFiles == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else if (taskFiles.isEmpty()) {
                    Text(
                        text = "Nessun file trovato per questo download.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        taskFiles.forEach { file ->
                            DownloadFileRow(file = file)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadFileRow(file: com.example.data.api.DownloadFile) {
    val fileProgress = if (file.size > 0L) file.rx.toFloat() / file.size else 0f
    val filePercent = (fileProgress * 100).toInt().coerceIn(0, 100)
    val isFileDone = file.status == "done" || file.rx >= file.size

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(getFileIcon(file.name, file.mimetype), fontSize = 16.sp)
                Text(
                    text = file.name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "${formatBytes(file.rx)} / ${formatBytes(file.size)} ($filePercent%)",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = if (isFileDone) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { fileProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape),
            color = if (isFileDone) Color(0xFF4CAF50) else MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

fun getFileIcon(fileName: String, mimetype: String?): String {
    val ext = fileName.substringAfterLast(".", "").lowercase()
    return when {
        mimetype?.startsWith("image/") == true || listOf("jpg", "jpeg", "png", "gif", "webp").contains(ext) -> "🖼️"
        mimetype?.startsWith("video/") == true || listOf("mp4", "mkv", "avi", "mov").contains(ext) -> "🎬"
        mimetype?.startsWith("audio/") == true || listOf("mp3", "flac", "ogg", "wav", "m4a").contains(ext) -> "🎵"
        listOf("zip", "rar", "tar", "gz", "7z").contains(ext) -> "📦"
        listOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "html", "json").contains(ext) -> "📄"
        listOf("iso", "img", "dmg").contains(ext) -> "💿"
        else -> "📄"
    }
}

fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val exp = (Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt()
    val pre = "KMGTPE"[exp - 1].toString()
    return String.format("%.1f %sB", bytes / Math.pow(1024.0, exp.toDouble()), pre)
}

