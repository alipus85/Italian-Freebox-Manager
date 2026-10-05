package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.*
import com.example.ui.components.IliadBottomBar
import com.example.ui.components.IliadHeader
import com.example.ui.components.WifiQrCodeDialog
import com.example.ui.screens.*
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.IliadRed
import com.example.ui.theme.MyApplicationTheme
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
    var showWifiQrDialog by remember { mutableStateOf(false) }
    var showInitialAuthDialog by remember { mutableStateOf(false) }
    var showInitialAuthConfirmDialog by remember { mutableStateOf(false) }
    var showAddDownloadDialog by remember { mutableStateOf(false) }

    // Auto-prompt initial pairing if not paired and not simulated
    LaunchedEffect(Unit) {
        delay(1200)
        if (viewModel.uiState.value.appToken.isEmpty() && !viewModel.uiState.value.isSimulated) {
            showInitialAuthDialog = true
        }
    }

    // Auto-dismiss errors/feedbacks
    LaunchedEffect(uiState.feedback, uiState.error) {
        if (uiState.feedback != null || uiState.error != null) {
            delay(4500)
            viewModel.clearErrorAndFeedback()
        }
    }

    // Wi-Fi QR Code Modal Dialog
    if (showWifiQrDialog || uiState.showWifiQrDialog) {
        WifiQrCodeDialog(
            ssid = uiState.wifiSsid.ifEmpty { uiState.discoveredDeviceName.ifEmpty { "Iliadbox-WiFi" } },
            password = uiState.wifiPassword,
            onDismissRequest = {
                showWifiQrDialog = false
                viewModel.toggleWifiQrDialog(false)
            }
        )
    }

    // Initial Auth First-Time Dialog
    if (showInitialAuthDialog) {
        AlertDialog(
            onDismissRequest = { showInitialAuthDialog = false },
            title = { Text("Configurazione Iniziale Router", fontWeight = FontWeight.Bold) },
            text = { Text("Nessuna associazione trovata. Vuoi eseguire l'associazione con la tua Iliadbox/Freebox adesso?") },
            confirmButton = {
                Button(
                    onClick = {
                        showInitialAuthDialog = false
                        showInitialAuthConfirmDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IliadRed)
                ) {
                    Text("Sì, associa")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInitialAuthDialog = false }) {
                    Text("Più tardi")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showInitialAuthConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showInitialAuthConfirmDialog = false },
            title = { Text("Conferma Associazione", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Assicurati di essere collegato alla rete Wi-Fi della tua Iliadbox.\n\n" +
                    "Durante la procedura comparirà una richiesta sul display del router: tocca la freccia verde o 'SÌ' per autorizzare questa app.\n\n" +
                    "Vuoi procedere?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showInitialAuthConfirmDialog = false
                        viewModel.setScreen(AppScreen.SETTINGS)
                        viewModel.registerApp()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IliadRed)
                ) {
                    Text("Procedi")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInitialAuthConfirmDialog = false }) {
                    Text("Annulla")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Pairing Progress Dialog
    if (uiState.pairingDialogState != PairingDialogState.HIDDEN) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissPairingDialog() },
            icon = {
                when (uiState.pairingDialogState) {
                    PairingDialogState.WAITING -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(36.dp),
                            color = IliadRed,
                            strokeWidth = 3.dp
                        )
                    }
                    PairingDialogState.SUCCESS -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Successo",
                            tint = EmeraldGreen,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    PairingDialogState.FAILURE -> {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Errore",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    else -> {}
                }
            },
            title = {
                Text(
                    text = when (uiState.pairingDialogState) {
                        PairingDialogState.WAITING -> "In attesa di conferma..."
                        PairingDialogState.SUCCESS -> "Associazione Completata!"
                        PairingDialogState.FAILURE -> "Associazione Fallita"
                        else -> ""
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = uiState.pairingDialogMessage ?: when (uiState.pairingDialogState) {
                            PairingDialogState.WAITING -> "Guarda il display touch della tua Iliadbox e tocca 'SÌ' o la freccia verde per autorizzare l'applicazione."
                            PairingDialogState.SUCCESS -> "L'applicazione è stata autorizzata con successo dalla tua Iliadbox!"
                            PairingDialogState.FAILURE -> "Impossibile completare l'associazione. Verifica di essere connesso al Wi-Fi e riprova."
                            else -> ""
                        },
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                when (uiState.pairingDialogState) {
                    PairingDialogState.WAITING -> {
                        TextButton(onClick = { viewModel.dismissPairingDialog() }) {
                            Text("Annulla")
                        }
                    }
                    PairingDialogState.SUCCESS -> {
                        Button(
                            onClick = { viewModel.dismissPairingDialog() },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                        ) {
                            Text("OK, Continua")
                        }
                    }
                    PairingDialogState.FAILURE -> {
                        Button(
                            onClick = { viewModel.dismissPairingDialog() },
                            colors = ButtonDefaults.buttonColors(containerColor = IliadRed)
                        ) {
                            Text("Chiudi")
                        }
                    }
                    else -> {}
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Reboot Confirmation Dialog
    if (showRebootDialog) {
        AlertDialog(
            onDismissRequest = { showRebootDialog = false },
            title = { Text(text = "Riavviare il Router?", fontWeight = FontWeight.Bold) },
            text = { Text(text = "Sei sicuro di voler riavviare la tua Iliadbox? La connessione internet si interromperà momentaneamente.") },
            confirmButton = {
                Button(
                    onClick = {
                        showRebootDialog = false
                        viewModel.rebootBox()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IliadRed)
                ) {
                    Text("Riavvia")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRebootDialog = false }) {
                    Text("Annulla")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Main App Scaffold with 5 tabs
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
                IliadHeader(
                    isSimulated = uiState.isSimulated,
                    connectionState = uiState.connectionState,
                    boxModelName = uiState.discoveredBoxModelName,
                    currentScreen = uiState.currentScreen,
                    isBusy = uiState.isBusy,
                    onActionClick = {
                        if (uiState.currentScreen == AppScreen.DOWNLOADS) {
                            showAddDownloadDialog = true
                        } else {
                            viewModel.refreshAll()
                        }
                    }
                )

                // Feedback / Error notifications
                AnimatedVisibility(visible = uiState.error != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("⚠️", fontSize = 16.sp, modifier = Modifier.padding(end = 8.dp))
                            Text(
                                text = uiState.error ?: "",
                                color = MaterialTheme.colorScheme.onErrorContainer,
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
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = EmeraldGreen.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("ℹ️", fontSize = 16.sp, modifier = Modifier.padding(end = 8.dp))
                            Text(
                                text = uiState.feedback ?: "",
                                color = EmeraldGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Adaptive Split Pane for Wide screens (Tablets)
                if (isWideScreen) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(1.1f)
                        ) {
                            RenderActiveScreen(
                                screen = uiState.currentScreen,
                                uiState = uiState,
                                viewModel = viewModel,
                                onRebootRequest = { showRebootDialog = true },
                                onOpenWifiQr = { showWifiQrDialog = true },
                                openAddDownloadDialog = showAddDownloadDialog,
                                onAddDownloadDialogClosed = { showAddDownloadDialog = false }
                            )
                        }

                        // Right supporting pane on tablet
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(0.9f)
                                .border(
                                    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    shape = RoundedCornerShape(topStart = 24.dp)
                                )
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(16.dp)
                        ) {
                            DevicesScreen(uiState = uiState, viewModel = viewModel)
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    ) {
                        RenderActiveScreen(
                            screen = uiState.currentScreen,
                            uiState = uiState,
                            viewModel = viewModel,
                            onRebootRequest = { showRebootDialog = true },
                            onOpenWifiQr = { showWifiQrDialog = true },
                            openAddDownloadDialog = showAddDownloadDialog,
                            onAddDownloadDialogClosed = { showAddDownloadDialog = false }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RenderActiveScreen(
    screen: AppScreen,
    uiState: FreeboxUiState,
    viewModel: FreeboxViewModel,
    onRebootRequest: () -> Unit,
    onOpenWifiQr: () -> Unit,
    openAddDownloadDialog: Boolean = false,
    onAddDownloadDialogClosed: () -> Unit = {}
) {
    when (screen) {
        AppScreen.HOME -> HomeDashboardScreen(
            uiState = uiState,
            viewModel = viewModel,
            onRebootRequest = onRebootRequest,
            onOpenWifiQr = onOpenWifiQr
        )
        AppScreen.DEVICES -> DevicesScreen(
            uiState = uiState,
            viewModel = viewModel
        )
        AppScreen.FILES -> FilesScreen(
            uiState = uiState,
            viewModel = viewModel
        )
        AppScreen.DOWNLOADS -> DownloadsScreen(
            uiState = uiState,
            viewModel = viewModel,
            openAddDialog = openAddDownloadDialog,
            onAddDialogClosed = onAddDownloadDialogClosed
        )
        AppScreen.SETTINGS -> SettingsScreen(
            uiState = uiState,
            viewModel = viewModel,
            onRebootRequest = onRebootRequest
        )
    }
}
