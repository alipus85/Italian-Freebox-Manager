package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.FreeboxUiState
import com.example.ui.FreeboxViewModel
import com.example.ui.theme.IliadRed
import com.example.util.formatBytes
import java.io.File

@Composable
fun FilesScreen(
    uiState: FreeboxUiState,
    viewModel: FreeboxViewModel
) {
    var showCreateDirDialog by remember { mutableStateOf(false) }
    var dirNameInput by remember { mutableStateOf("") }

    // Intercept back navigation when inside a subfolder
    BackHandler(enabled = uiState.currentPath.isNotEmpty()) {
        val parentPath = uiState.currentPath.substringBeforeLast("/", "")
        viewModel.loadFiles(parentPath)
    }

    LaunchedEffect(Unit) {
        if (uiState.files.isEmpty()) {
            viewModel.loadFiles(uiState.currentPath)
        }
    }

    val uploadLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            viewModel.uploadFileToFsTask(uri)
        }
    }

    if (showCreateDirDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDirDialog = false },
            title = { Text("Nuova Cartella", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = dirNameInput,
                    onValueChange = { dirNameInput = it },
                    label = { Text("Nome cartella") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = IliadRed
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (dirNameInput.isNotBlank()) {
                            viewModel.createDirectory(dirNameInput)
                        }
                        showCreateDirDialog = false
                        dirNameInput = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IliadRed)
                ) {
                    Text("Crea")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDirDialog = false }) { Text("Annulla") }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Navigation header for current directory
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (uiState.currentPath.isNotEmpty()) {
                    IconButton(onClick = {
                        val parentPath = uiState.currentPath.substringBeforeLast("/", "")
                        viewModel.loadFiles(parentPath)
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Indietro",
                            tint = IliadRed
                        )
                    }
                }
                Column {
                    Text(
                        text = if (uiState.currentPath.isEmpty()) "File e Cartelle" else uiState.currentPath.substringAfterLast("/"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (uiState.currentPath.isEmpty()) "Memoria interna e dischi USB router" else "/${uiState.currentPath}",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.loadFiles(uiState.currentPath) }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Aggiorna", tint = IliadRed)
                }
                IconButton(onClick = { uploadLauncher.launch("*/*") }) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Upload", tint = IliadRed)
                }
                IconButton(onClick = { showCreateDirDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Nuova Cartella", tint = IliadRed)
                }
                if (uiState.isLoadingFiles) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = IliadRed
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Files List Container
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            if (uiState.files.isEmpty() && !uiState.isLoadingFiles) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📁", fontSize = 42.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Nessun elemento presente",
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tocca '+' in alto per creare una cartella o caricare un file",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 11.5.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    items(uiState.files, key = { it.path ?: it.name }) { file ->
                        val icon = if (file.type == "dir") "📁" else {
                            if (file.mimetype?.startsWith("video") == true) "🎬"
                            else if (file.mimetype?.startsWith("audio") == true) "🎵"
                            else if (file.mimetype?.startsWith("image") == true) "🖼️"
                            else "📄"
                        }
                        val info = if (file.type == "dir") "Cartella" else {
                            val bytes = file.size ?: 0L
                            formatBytes(bytes)
                        }
                        FileRowItem(
                            icon = icon,
                            name = file.name,
                            info = info,
                            isDir = file.type == "dir",
                            fullPath = if (uiState.currentPath.isEmpty()) file.name else "${uiState.currentPath}/${file.name}",
                            size = file.size ?: 0L,
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
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FileRowItem(
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
            text = { Text("Il file è di circa ${formatBytes(size)}. Vuoi procedere con il download?") },
            confirmButton = {
                Button(
                    onClick = {
                        val destDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        val destFile = File(destDir, name)
                        viewModel.downloadFileToDisk(fullPath, destFile)
                        Toast.makeText(context, "Download avviato...", Toast.LENGTH_SHORT).show()
                        showDownloadDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IliadRed)
                ) { Text("Download") }
            },
            dismissButton = {
                TextButton(onClick = { showDownloadDialog = false }) { Text("Annulla") }
            },
            shape = RoundedCornerShape(20.dp)
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
                    label = { Text("Nuovo nome") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameInput.isNotBlank() && renameInput != name) {
                            viewModel.renameFile(fullPath, renameInput)
                        }
                        showRenameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IliadRed)
                ) { Text("Rinomina") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Annulla") }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Conferma eliminazione") },
            text = { Text("Vuoi davvero eliminare $name?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.removeFile(fullPath)
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Elimina") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Annulla") }
            },
            shape = RoundedCornerShape(20.dp)
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
                        val destFile = File(context.cacheDir, name)
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
                .size(42.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = icon, fontSize = 20.sp)
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    color = IliadRed
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
                            val destFile = File(destDir, name)
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
