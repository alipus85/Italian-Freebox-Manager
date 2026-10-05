package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.LanHost
import com.example.ui.AppScreen
import com.example.ui.ConnectionState
import com.example.ui.FreeboxUiState
import com.example.ui.FreeboxViewModel
import com.example.ui.components.DeviceRow
import com.example.ui.theme.*

@Composable
fun HomeDashboardScreen(
    uiState: FreeboxUiState,
    viewModel: FreeboxViewModel,
    onRebootRequest: () -> Unit,
    onOpenWifiQr: () -> Unit
) {
    var selectedDevice by remember { mutableStateOf<LanHost?>(null) }
    var showGuestWifiDialog by remember { mutableStateOf(false) }

    // Dialog for device detail inspection
    if (selectedDevice != null) {
        AlertDialog(
            onDismissRequest = { selectedDevice = null },
            title = {
                Text(
                    text = selectedDevice?.primaryName ?: "Dispositivo di Rete",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailRow(label = "Indirizzo IP", value = selectedDevice?.ipAddress ?: "Non assegnato")
                    DetailRow(label = "Indirizzo MAC", value = selectedDevice?.macAddress ?: "N/A")
                    DetailRow(label = "Tipo Dispositivo", value = selectedDevice?.hostType ?: "Generico")
                    DetailRow(label = "Connettività", value = selectedDevice?.connectivityType ?: "Wi-Fi")
                    DetailRow(label = "Stato", value = if (selectedDevice?.active == true) "Attivo (Connesso)" else "Offline")
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedDevice = null },
                    colors = ButtonDefaults.buttonColors(containerColor = IliadRed)
                ) {
                    Text("Chiudi")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Dialog for Guest Wi-Fi
    if (showGuestWifiDialog) {
        AlertDialog(
            onDismissRequest = { showGuestWifiDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("👥", fontSize = 20.sp, modifier = Modifier.padding(end = 8.dp))
                    Text("Rete Wi-Fi Ospiti", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "La rete ospiti crea un punto d'accesso Wi-Fi isolato e protetto per consentire la navigazione a parenti e amici senza esporre i dispositivi della tua rete privata.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Nome Rete: ${uiState.discoveredDeviceName.ifEmpty { "Iliadbox" }}_Ospiti", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Accesso LAN: Disabilitato (Solo Internet)", fontSize = 11.sp, color = EmeraldGreen, fontWeight = FontWeight.Medium)
                            Text("Stato Wi-Fi Principale: " + if (uiState.isWifiEnabled) "Attivo" else "Disattivato", fontSize = 11.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showGuestWifiDialog = false
                        viewModel.refreshAll()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IliadRed)
                ) {
                    Text("Abilita Rete Ospiti")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGuestWifiDialog = false }) {
                    Text("Chiudi")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // DEMO / SIMULATION BANNER
        if (uiState.isSimulated) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("💡", fontSize = 22.sp, modifier = Modifier.padding(end = 12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Modalità Demo Attiva",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Per configurare una Iliadbox reale, vai alla scheda Impostazioni (⚙️).",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        // 1. HERO ILIADBOX ROUTER CARD (Iliadbox Connect style)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 2.dp, shape = RoundedCornerShape(28.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top status row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = if (uiState.connectionState == ConnectionState.CONNECTED) EmeraldGreen.copy(alpha = 0.12f) else IliadRed.copy(alpha = 0.12f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (uiState.connectionState == ConnectionState.CONNECTED) EmeraldGreen else IliadRed)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (uiState.connectionState == ConnectionState.CONNECTED) "Iliadbox Online" else "Disconnesso",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.connectionState == ConnectionState.CONNECTED) EmeraldGreen else IliadRed
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = uiState.mediaType,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Stylized Iliadbox hardware visual (Round router with status display)
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF2C2C32),
                                    Color(0xFF18181C)
                                )
                            )
                        )
                        .border(3.dp, if (uiState.connectionState == ConnectionState.CONNECTED) EmeraldGreen.copy(alpha = 0.8f) else IliadRed.copy(alpha = 0.8f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "iliad",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = IliadRed,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = if (uiState.isWifiEnabled) "Wi-Fi ON" else "Wi-Fi OFF",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = uiState.discoveredBoxModelName.ifEmpty { "Iliadbox Wi-Fi 6" },
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Rete: ${uiState.discoveredDeviceName.ifEmpty { "Iliadbox-WiFi" }} • ${uiState.wifiActiveBand}",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Share Wi-Fi button
                Button(
                    onClick = onOpenWifiQr,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IliadRed,
                        contentColor = Color.White
                    )
                ) {
                    Text("📶", fontSize = 16.sp, modifier = Modifier.padding(end = 8.dp))
                    Text(
                        text = "Condividi Wi-Fi con QR Code",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // 2. VELOCITÀ DI CONNESSIONE (Download / Upload)
        Text(
            text = "VELOCITÀ DI CONNESSIONE",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Download Speed
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⬇️", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "DOWNLOAD",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "%.0f".format(uiState.downloadSpeedMbps),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = IliadRed
                        )
                        Text(
                            text = " Mbps",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }
                }
            }

            // Upload Speed
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⬆️", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "UPLOAD",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "%.0f".format(uiState.uploadSpeedMbps),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = " Mbps",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }
                }
            }
        }

        // 3. AZIONI RAPIDE
        Text(
            text = "AZIONI RAPIDE",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickActionTile(
                icon = if (uiState.isWifiEnabled) "📶" else "📵",
                title = if (uiState.isWifiEnabled) "Wi-Fi Attivo" else "Wi-Fi Spento",
                subtitle = uiState.wifiActiveBand,
                isActive = uiState.isWifiEnabled,
                modifier = Modifier.weight(1f),
                onClick = { viewModel.toggleWifi(!uiState.isWifiEnabled) }
            )

            QuickActionTile(
                icon = "👥",
                title = "Rete Ospiti",
                subtitle = "Accesso sicuro",
                isActive = false,
                modifier = Modifier.weight(1f),
                onClick = { showGuestWifiDialog = true }
            )

            QuickActionTile(
                icon = "🔄",
                title = "Riavvia Box",
                subtitle = "Iliadbox",
                isActive = false,
                modifier = Modifier.weight(1f),
                onClick = onRebootRequest
            )
        }

        // 4. TELEMETRIA BOX & HARDWARE
        Text(
            text = "TELEMETRIA BOX",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("🌡️", fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Temp CPU", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = uiState.systemCpuTemp,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = if (uiState.systemCpuTemp.contains("Alta", ignoreCase = true)) IliadRed else EmeraldGreen
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("🌀", fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Ventola Box", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = uiState.systemFanSpeed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("⏱️", fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Attività", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = uiState.systemUptime.substringBefore(","),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // 5. ANTEPRIMA DEI DISPOSITIVI CONNESSI
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "DISPOSITIVI CONNESSI",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                val activeCount = uiState.devices.count { it.active == true }
                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "$activeCount",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            TextButton(onClick = { viewModel.setScreen(AppScreen.DEVICES) }) {
                Text(
                    text = "Gestisci tutti",
                    fontSize = 12.sp,
                    color = IliadRed,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 1.dp, shape = RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (uiState.devices.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📱", fontSize = 32.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Nessun dispositivo rilevato sulla rete",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    val previewList = uiState.devices.take(3)
                    for ((index, host) in previewList.withIndex()) {
                        DeviceRow(
                            host = host,
                            modifier = Modifier.clickable { selectedDevice = host }
                        )
                        if (index < previewList.size - 1) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // 6. SCORCIATOIE SCHEDE FILE & DOWNLOAD
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { viewModel.setScreen(AppScreen.FILES) },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("📁", fontSize = 24.sp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text("File Manager", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Dischi & Storage", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { viewModel.setScreen(AppScreen.DOWNLOADS) },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("🧲", fontSize = 24.sp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Torrent", fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                        Text("${uiState.downloadTasks.size} task attivi", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun QuickActionTile(
    icon: String,
    title: String,
    subtitle: String?,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (isActive) IliadRed.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(icon, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 9.5.sp,
                    color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}
