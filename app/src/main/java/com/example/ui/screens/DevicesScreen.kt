package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.LanHost
import com.example.ui.FreeboxUiState
import com.example.ui.FreeboxViewModel
import com.example.ui.components.DeviceRow
import com.example.ui.theme.IliadRed

enum class DeviceFilter {
    ALL, ACTIVE, WIFI, ETHERNET
}

@Composable
fun DevicesScreen(
    uiState: FreeboxUiState,
    viewModel: FreeboxViewModel
) {
    var searchQuery by remember { mutableStateOf("") }
    var currentFilter by remember { mutableStateOf(DeviceFilter.ALL) }
    var selectedDevice by remember { mutableStateOf<LanHost?>(null) }

    if (selectedDevice != null) {
        AlertDialog(
            onDismissRequest = { selectedDevice = null },
            title = {
                Text(
                    text = selectedDevice?.primaryName ?: "Dettaglio Dispositivo",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DetailRow(label = "Indirizzo IP", value = selectedDevice?.ipAddress ?: "Nessun IP IPv4")
                    DetailRow(label = "Indirizzo MAC", value = selectedDevice?.macAddress ?: "N/A")
                    DetailRow(label = "Tipo", value = selectedDevice?.hostType?.replaceFirstChar { it.uppercase() } ?: "Generico")
                    DetailRow(label = "Connettività", value = selectedDevice?.connectivityType ?: "Wi-Fi")
                    DetailRow(label = "Stato Rete", value = if (selectedDevice?.active == true) "Connesso ora" else "Disconnesso")
                    DetailRow(label = "ID Host", value = selectedDevice?.id ?: "")
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Screen Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Dispositivi di rete",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${uiState.devices.count { it.active == true }} attivi di ${uiState.devices.size} registrati",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = { viewModel.refreshAll() }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Aggiorna elenco",
                    tint = IliadRed
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Cerca per nome, IP o MAC...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Cerca") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancella")
                    }
                }
            },
            shape = RoundedCornerShape(100.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = IliadRed,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Chips Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = currentFilter == DeviceFilter.ALL,
                onClick = { currentFilter = DeviceFilter.ALL },
                label = { Text("Tutti (${uiState.devices.size})", fontSize = 11.sp) }
            )
            FilterChip(
                selected = currentFilter == DeviceFilter.ACTIVE,
                onClick = { currentFilter = DeviceFilter.ACTIVE },
                label = { Text("Attivi (${uiState.devices.count { it.active == true }})", fontSize = 11.sp) }
            )
            FilterChip(
                selected = currentFilter == DeviceFilter.WIFI,
                onClick = { currentFilter = DeviceFilter.WIFI },
                label = { Text("Wi-Fi", fontSize = 11.sp) }
            )
            FilterChip(
                selected = currentFilter == DeviceFilter.ETHERNET,
                onClick = { currentFilter = DeviceFilter.ETHERNET },
                label = { Text("Ethernet", fontSize = 11.sp) }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Filtered List
        val filteredList = uiState.devices.filter { host ->
            val nameMatch = host.primaryName?.contains(searchQuery, ignoreCase = true) == true ||
                    host.ipAddress?.contains(searchQuery, ignoreCase = true) == true ||
                    host.macAddress?.contains(searchQuery, ignoreCase = true) == true ||
                    searchQuery.isBlank()

            val filterMatch = when (currentFilter) {
                DeviceFilter.ALL -> true
                DeviceFilter.ACTIVE -> host.active == true
                DeviceFilter.WIFI -> host.connectivityType?.lowercase()?.contains("wifi") == true
                DeviceFilter.ETHERNET -> host.connectivityType?.lowercase()?.contains("ethernet") == true
            }

            nameMatch && filterMatch
        }

        Box(modifier = Modifier.weight(1f)) {
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔍", fontSize = 40.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Nessun dispositivo corrispondente",
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredList, key = { it.id }) { host ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedDevice = host },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Box(modifier = Modifier.padding(14.dp)) {
                                DeviceRow(host = host)
                            }
                        }
                    }
                }
            }
        }
    }
}
