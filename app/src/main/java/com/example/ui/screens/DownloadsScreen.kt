package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.DownloadFile
import com.example.data.api.DownloadTask
import com.example.ui.FreeboxUiState
import com.example.ui.FreeboxViewModel
import com.example.ui.theme.*
import com.example.util.formatBytes
import java.util.Locale

enum class TaskFilter {
    ALL, DOWNLOADING, TORRENT_SEEDING, PAUSED, COMPLETED
}

@Composable
fun DownloadsScreen(
    uiState: FreeboxUiState,
    viewModel: FreeboxViewModel,
    openAddDialog: Boolean = false,
    onAddDialogClosed: () -> Unit = {}
) {
    var showInternalAddDialog by remember { mutableStateOf(false) }
    val showAddDialog = openAddDialog || showInternalAddDialog
    fun closeAddDialog() {
        showInternalAddDialog = false
        onAddDialogClosed()
    }

    var newDownloadUrl by remember { mutableStateOf("") }
    var taskToDelete by remember { mutableStateOf<DownloadTask?>(null) }
    var currentFilter by remember { mutableStateOf(TaskFilter.ALL) }
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(Unit) {
        viewModel.getDownloadTasks()
    }

    val torrentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            viewModel.addDownloadByFileTask(uri)
            closeAddDialog()
        }
    }

    // Modal dialog to add a new direct download or .torrent / Magnet
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { closeAddDialog() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🧲", fontSize = 20.sp, modifier = Modifier.padding(end = 8.dp))
                    Text(
                        text = "Nuovo Torrent",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Inserisci un Magnet link, URL di un torrent o link diretto (HTTP/FTP):",
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = newDownloadUrl,
                        onValueChange = { newDownloadUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("magnet:?xt=urn:btih:... o https://...") },
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IliadRed
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("📋 Incolla Link", fontSize = 12.sp, maxLines = 1)
                        }

                        Button(
                            onClick = { torrentLauncher.launch("*/*") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("📁 File .torrent", fontSize = 12.sp, maxLines = 1)
                        }
                    }

                    // Test preset links
                    Text(
                        text = "Esempi Rapidi Open-Source:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SuggestionChip(
                            onClick = {
                                newDownloadUrl = "https://releases.ubuntu.com/24.04/ubuntu-24.04-desktop-amd64.iso.torrent"
                            },
                            label = { Text("🐧 Ubuntu 24.04", fontSize = 10.5.sp) }
                        )
                        SuggestionChip(
                            onClick = {
                                newDownloadUrl = "https://cdimage.debian.org/debian-cd/current/amd64/bt-cd/debian-12.5.0-amd64-netinst.iso.torrent"
                            },
                            label = { Text("🔴 Debian 12", fontSize = 10.5.sp) }
                        )
                        SuggestionChip(
                            onClick = {
                                newDownloadUrl = "magnet:?xt=urn:btih:3b245504cf5f11bbdbe1201cea6a6bf45a004e35&dn=Big_Buck_Bunny_1080p.mp4"
                            },
                            label = { Text("🎬 Big Buck Bunny", fontSize = 10.5.sp) }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newDownloadUrl.isNotBlank()) {
                            viewModel.addDownloadTask(newDownloadUrl)
                            newDownloadUrl = ""
                            closeAddDialog()
                        }
                    },
                    enabled = newDownloadUrl.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = IliadRed),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Avvia Torrent", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    newDownloadUrl = ""
                    closeAddDialog()
                }) {
                    Text("Annulla")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (taskToDelete != null) {
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Elimina Torrent", fontWeight = FontWeight.Bold) },
            text = { Text("Sei sicuro di voler rimuovere '${taskToDelete?.name}' dalla coda della Iliadbox?") },
            confirmButton = {
                Button(
                    onClick = {
                        val id = taskToDelete?.id
                        taskToDelete = null
                        if (id != null) viewModel.removeDownloadTask(id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Elimina")
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToDelete = null }) { Text("Annulla") }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Torrent",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Client BitTorrent Iliadbox",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = { viewModel.getDownloadTasks() }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Aggiorna",
                    tint = IliadRed
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Transfer Speeds Compact Bar & Add Button
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⬇️", fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${formatBytes(uiState.currentDownloadRate)}/s",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = IliadRed
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⬆️", fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${formatBytes(uiState.currentUploadRate)}/s",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                Button(
                    onClick = { showInternalAddDialog = true },
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IliadRed),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Aggiungi", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Chips Row
        val downloadingCount = uiState.downloadTasks.count { it.status == "downloading" || it.status == "starting" }
        val seedingCount = uiState.downloadTasks.count { it.status.contains("seeding", ignoreCase = true) || it.status.contains("seed", ignoreCase = true) }
        val pausedCount = uiState.downloadTasks.count { it.status == "stopped" || it.status == "stopped_error" || it.status == "seeding_paused" }
        val completedCount = uiState.downloadTasks.count { it.status == "done" || it.status == "seeding_done" }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = currentFilter == TaskFilter.ALL,
                onClick = { currentFilter = TaskFilter.ALL },
                label = { Text("Tutti (${uiState.downloadTasks.size})", fontSize = 10.5.sp) }
            )
            FilterChip(
                selected = currentFilter == TaskFilter.DOWNLOADING,
                onClick = { currentFilter = TaskFilter.DOWNLOADING },
                label = { Text("In Download ($downloadingCount)", fontSize = 10.5.sp) }
            )
            FilterChip(
                selected = currentFilter == TaskFilter.TORRENT_SEEDING,
                onClick = { currentFilter = TaskFilter.TORRENT_SEEDING },
                label = { Text("Seed ($seedingCount)", fontSize = 10.5.sp) }
            )
            FilterChip(
                selected = currentFilter == TaskFilter.PAUSED,
                onClick = { currentFilter = TaskFilter.PAUSED },
                label = { Text("In Pausa ($pausedCount)", fontSize = 10.5.sp) }
            )
            FilterChip(
                selected = currentFilter == TaskFilter.COMPLETED,
                onClick = { currentFilter = TaskFilter.COMPLETED },
                label = { Text("Completati ($completedCount)", fontSize = 10.5.sp) }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Filtered tasks list
        val filteredTasks = uiState.downloadTasks.filter { task ->
            when (currentFilter) {
                TaskFilter.ALL -> true
                TaskFilter.DOWNLOADING -> task.status == "downloading" || task.status == "starting"
                TaskFilter.TORRENT_SEEDING -> task.status.contains("seeding", ignoreCase = true) || task.status.contains("seed", ignoreCase = true)
                TaskFilter.PAUSED -> task.status == "stopped" || task.status == "stopped_error" || task.status == "seeding_paused"
                TaskFilter.COMPLETED -> task.status == "done" || task.status == "seeding_done"
            }
        }

        // List of tasks
        Box(modifier = Modifier.weight(1f)) {
            if (filteredTasks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🧲", fontSize = 42.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (uiState.downloadTasks.isEmpty()) "Nessun torrent attivo" else "Nessun torrent per questo filtro",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Tocca '+ Aggiungi' per caricare un file Torrent o Magnet link.",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredTasks, key = { it.id }) { task ->
                        DownloadTaskCard(
                            task = task,
                            isExpanded = uiState.expandedTaskIds.contains(task.id),
                            onToggleExpand = { viewModel.toggleTaskExpanded(task.id) },
                            taskFiles = uiState.taskFiles[task.id],
                            onControl = { action -> viewModel.controlDownloadTask(task.id, action) },
                            onRemove = { taskToDelete = task }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadTaskCard(
    task: DownloadTask,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    taskFiles: List<DownloadFile>?,
    onControl: (String) -> Unit,
    onRemove: () -> Unit
) {
    val progress = if (task.size > 0L) task.downloadedSize.toFloat() / task.size else 0f
    val progressPercent = (progress * 100).toInt().coerceIn(0, 100)

    val isSeeding = task.status.contains("seeding", ignoreCase = true) || task.status.contains("seed", ignoreCase = true)
    val isDone = task.status == "done" || task.status == "seeding_done"
    val isStopped = task.status == "stopped" || task.status == "stopped_error" || task.status == "seeding_paused"
    val isDownloading = task.status == "downloading" || task.status == "starting"
    val isChecking = task.status == "checking" || task.status == "repairing" || task.status == "extracting"
    val isError = task.status.contains("error", ignoreCase = true)

    val cleanTaskName = task.name.trim().replace(Regex("[\\r\\n]+"), " ")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 1.dp, shape = RoundedCornerShape(18.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onToggleExpand() }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (task.isTorrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = if (task.isTorrent) "🧲 TORRENT" else "⬇️ HTTP",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (task.isTorrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = cleanTaskName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))
                    val (statusEmoji, statusText, statusColor) = when {
                        isSeeding -> Triple("🌱", "Seeding attivo", EmeraldGreen)
                        isDone -> Triple("✅", "Completato", EmeraldGreen)
                        isStopped -> Triple("⏸️", "In pausa", AmberWarning)
                        isDownloading -> {
                            val rateStr = if ((task.rxRate ?: 0L) > 0L) " • ${formatBytes(task.rxRate!!)}/s" else ""
                            Triple("⚡", "Download in corso$rateStr", IliadRed)
                        }
                        isChecking -> Triple("🔍", "Controllo integrità...", IliadBlue)
                        isError -> Triple("❌", "Errore", MaterialTheme.colorScheme.error)
                        else -> Triple("ℹ️", task.status, IliadBlue)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$statusEmoji $statusText",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = statusColor
                        )
                        task.formattedEta?.let { eta ->
                            if (isDownloading) {
                                Text(
                                    text = " • ⏱️ $eta",
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onToggleExpand, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = IliadRed
                        )
                    }
                    IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Elimina",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Torrent Seeds / Peers details
            val seedsConn = task.seedsConn
            val seedsTot = task.seedsTot
            val peersConn = task.peersConn
            val peersTot = task.peersTot

            if (task.isTorrent) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🌱 Seed: $seedsConn${if (seedsTot > 0 && seedsTot != seedsConn) "/$seedsTot" else ""}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "   👥 Peer: $peersConn${if (peersTot > 0 && peersTot != peersConn) "/$peersTot" else ""}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    val healthText = when {
                        seedsConn >= 5 -> "Torrent Ottimo"
                        seedsConn > 0 -> "Torrent Buono"
                        peersConn > 0 -> "Peer Connessi"
                        else -> "In ricerca Seed"
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (seedsConn > 0) EmeraldGreen else AmberWarning
                    ) {
                        Text(
                            text = healthText,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress Bar
            val barColor = when {
                isSeeding || isDone -> EmeraldGreen
                isStopped -> AmberWarning
                isError -> MaterialTheme.colorScheme.error
                else -> IliadRed
            }
            LinearProgressIndicator(
                progress = { if (isSeeding || isDone) 1f else progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = barColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Stats Row & Play/Pause/Retry
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val ratio = if (task.downloadedSize > 0) (task.uploadedSize ?: 0L).toDouble() / task.downloadedSize else 0.0
                Column {
                    Text(
                        text = "${formatBytes(task.downloadedSize)} di ${formatBytes(task.size)} ($progressPercent%)",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (task.isTorrent && (task.uploadedSize ?: 0L) > 0L) {
                        Text(
                            text = "⬆️ ${formatBytes(task.uploadedSize!!)} • Ratio: ${String.format(Locale.US, "%.2f", ratio)}",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (isError || isStopped) {
                        IconButton(
                            onClick = { onControl("retry") },
                            modifier = Modifier
                                .size(28.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Riprova",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    if (!isDone) {
                        if (isDownloading || isSeeding) {
                            IconButton(
                                onClick = { onControl("stopped") },
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                            ) {
                                Text("⏸️", fontSize = 11.sp)
                            }
                        } else if (isStopped) {
                            IconButton(
                                onClick = { onControl("downloading") },
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                            ) {
                                Text("▶️", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Expanded File List and Details
            if (isExpanded) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(6.dp))

                // Torrent technical specifications
                if (task.isTorrent) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Posizione in coda:", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("#${task.queuePosition}", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Velocità Upload corrente:", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${formatBytes(task.txRate ?: 0L)}/s", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Soglia Stop Ratio:", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${(task.stopRatio ?: 100) / 100f}x", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                Text(
                    text = "FILE CONTENUTI (${taskFiles?.size ?: "..."})",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )

                if (taskFiles == null) {
                    Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = IliadRed)
                    }
                } else if (taskFiles.isEmpty()) {
                    Text(
                        text = "Nessun file dettagliato.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                        taskFiles.forEach { f ->
                            DownloadFileItem(file = f)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadFileItem(file: DownloadFile) {
    val fileProgress = if (file.size > 0L) file.rx.toFloat() / file.size else 0f
    val filePercent = (fileProgress * 100).toInt().coerceIn(0, 100)
    val isFileDone = file.status == "done" || file.rx >= file.size
    val cleanName = file.name.trim().replace(Regex("[\\r\\n]+"), " ")

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = cleanName,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(end = 6.dp)
            )
            Text(
                text = "${formatBytes(file.rx)} / ${formatBytes(file.size)} ($filePercent%)",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = if (isFileDone) EmeraldGreen else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        LinearProgressIndicator(
            progress = { fileProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(CircleShape),
            color = if (isFileDone) EmeraldGreen else MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}
