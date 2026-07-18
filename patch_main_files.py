import sys

def modify_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    new_imports = """
import android.app.DownloadManager
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
"""
    content = content.replace("import androidx.compose.foundation.layout.*", "import androidx.compose.foundation.layout.*\n" + new_imports)

    files_screen_code = """
@Composable
fun FilesScreen(uiState: FreeboxUiState, viewModel: FreeboxViewModel) {
    var showCreateDirDialog by remember { mutableStateOf(false) }
    var dirNameInput by remember { mutableStateOf("") }
    
    val context = LocalContext.current
    val uploadLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            Toast.makeText(context, "Upload simulato per: ${uri.path}", Toast.LENGTH_SHORT).show()
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
                    Icon(Icons.Default.Upload, contentDescription = "Upload File", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { showCreateDirDialog = true }) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = "Nuova Cartella", tint = MaterialTheme.colorScheme.primary)
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
                        val info = if (file.type == "dir") "Cartella" else "${(file.size ?: 0) / 1024 / 1024} MB"
                        FileRow(
                            icon = icon, 
                            name = file.name, 
                            info = info,
                            isDir = file.type == "dir",
                            fullPath = if (uiState.currentPath.isEmpty()) file.name else "${uiState.currentPath}/${file.name}",
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
    viewModel: FreeboxViewModel,
    uiState: FreeboxUiState,
    onClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameInput by remember { mutableStateOf(name) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    val context = LocalContext.current

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
            .clickable(onClick = onClick)
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
                        leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) },
                        onClick = {
                            expanded = false
                            val url = viewModel.getDownloadUrl(fullPath)
                            if (url != null) {
                                val request = DownloadManager.Request(Uri.parse(url))
                                    .setTitle(name)
                                    .setDescription("Download from Freebox")
                                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name)
                                    .addRequestHeader("X-Fbx-App-Auth", viewModel.getSessionToken())
                                
                                val downloadManager = context.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as DownloadManager
                                downloadManager.enqueue(request)
                                Toast.makeText(context, "Download avviato", Toast.LENGTH_SHORT).show()
                            }
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
"""
    import re
    content = re.sub(r'@Composable\nfun FilesScreen.*?\}\n\}\n\n@Composable\nfun FileRow.*?\n\}', files_screen_code, content, flags=re.DOTALL)
    
    with open(filepath, 'w') as f:
        f.write(content)

if __name__ == "__main__":
    modify_file("app/src/main/java/com/example/MainActivity.kt")
