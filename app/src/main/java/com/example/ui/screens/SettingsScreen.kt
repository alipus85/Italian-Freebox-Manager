package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.ui.ConnectionState
import com.example.ui.FreeboxUiState
import com.example.ui.FreeboxViewModel
import com.example.ui.components.HandshakeStep
import com.example.ui.theme.*
import com.example.util.DownloadState
import com.example.util.formatBytes

@Composable
fun SettingsScreen(
    uiState: FreeboxUiState,
    viewModel: FreeboxViewModel,
    onRebootRequest: () -> Unit
) {
    var boxUrlInput by remember(uiState.boxUrl) { mutableStateOf(uiState.boxUrl) }
    var domainInput by remember(uiState.discoveredApiDomain) { mutableStateOf(uiState.discoveredApiDomain) }
    var portInput by remember(uiState.discoveredHttpsPort) {
        mutableStateOf(if (uiState.discoveredHttpsPort > 0) uiState.discoveredHttpsPort.toString() else "297")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Impostazioni Router",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Banner per suggerire e applicare la connessione sicura HTTPS se rilevata
        if (uiState.discoveredHttpsAvailable && uiState.discoveredApiDomain.isNotBlank() && !uiState.boxUrl.contains(uiState.discoveredApiDomain)) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = EmeraldGreenLight),
                border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🔒", fontSize = 16.sp, modifier = Modifier.padding(end = 8.dp))
                        Text(
                            text = "Connessione Sicura Iliadbox Rilevata",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = EmeraldGreen
                        )
                    }
                    Text(
                        text = "Il router ha fornito l'endpoint HTTPS ufficiale: ${uiState.discoveredApiDomain}:${uiState.discoveredHttpsPort}.\nApplicando questo indirizzo potrai autenticarti ed eseguire la registrazione senza errori.",
                        fontSize = 11.5.sp,
                        color = Color(0xFF1B5E20)
                    )
                    Button(
                        onClick = { viewModel.applyDiscoveredHttps() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(100.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                    ) {
                        Text(
                            text = "Applica https://${uiState.discoveredApiDomain}:${uiState.discoveredHttpsPort}/",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Scheda di configurazione DNS e API
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(22.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "ENDPOINT DI CONNESSIONE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )

                Text(
                    text = "Accedi al tuo router in locale o da remoto tramite l'indirizzo DNS/IP della tua Iliadbox.",
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
                    label = { Text("URL API / DNS") },
                    placeholder = { Text("http://myiliadbox.iliad.it/") },
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done
                    ),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = IliadRed
                    )
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
                        Text("Scopri in Rete", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
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
                            Text("Testa Connessione", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                        }
                    }
                }

                // Risultato test API
                uiState.testedApiVersion?.let { info ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(EmeraldGreenLight)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text("🟢", fontSize = 12.sp, modifier = Modifier.padding(end = 8.dp))
                        Text(info, color = EmeraldGreen, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }

                // Switch Modalità Demo
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
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
                            text = "Visualizza le funzioni dell'app senza collegare un router reale.",
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

        // Scheda Connessione Sicura (Porta e Dominio)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(22.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "IMPOSTAZIONI CONNESSIONE SICURA ILIADBOX",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )

                Text(
                    text = "Personalizza o inserisci il nome di dominio e la porta HTTPS della tua Iliadbox (visibili nell'app ufficiale iliadbox o ottenuti con 'Testa Connessione').",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = domainInput,
                    onValueChange = { domainInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nome dominio Iliadbox") },
                    placeholder = { Text("es. 7rkyjjfc.ibxos.it") },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = IliadRed
                    )
                )

                OutlinedTextField(
                    value = portInput,
                    onValueChange = { portInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Porta HTTPS") },
                    placeholder = { Text("297") },
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = IliadRed
                    )
                )

                Button(
                    onClick = { viewModel.setCustomDomainAndPort(domainInput, portInput) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IliadRed),
                    enabled = domainInput.isNotBlank() && portInput.isNotBlank()
                ) {
                    Text("Salva Impostazioni HTTPS", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        // Handshake di Autenticazione Timeline
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(22.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "AUTENTICAZIONE & REGISTRAZIONE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 14.dp)
                )

                val step1Completed = uiState.testedApiVersion != null || uiState.trackId != -1
                val step2Completed = uiState.trackId != -1
                val step3Completed = uiState.authStatus == "granted" || uiState.connectionState == ConnectionState.CONNECTED
                val step4Completed = uiState.connectionState == ConnectionState.CONNECTED

                // PASSO 1
                HandshakeStep(
                    stepNumber = 1,
                    title = "Verifica URL Router",
                    description = "Verifica l'accessibilità dell'endpoint API dell'Iliadbox.",
                    statusText = if (step1Completed) "VERIFICATO" else "IN ATTESA",
                    statusColor = if (step1Completed) EmeraldGreen else AmberWarning,
                    isActive = !step1Completed,
                    isCompleted = step1Completed,
                    showLine = true
                ) {
                    Text(
                        text = "Endpoint: " + (if (uiState.isSimulated) "Local Host Simulato" else uiState.boxUrl),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // PASSO 2
                HandshakeStep(
                    stepNumber = 2,
                    title = "Registra Applicazione",
                    description = "Richiedi le credenziali del token dell'app e un ID di tracciamento dal router.",
                    statusText = if (step2Completed) "REGISTRATO" else if (uiState.connectionState == ConnectionState.REGISTERING) "REGISTRAZIONE..." else if (uiState.connectionState == ConnectionState.ERROR) "ERRORE" else if (step1Completed) "PRONTO" else "BLOCCATO",
                    statusColor = if (step2Completed) EmeraldGreen else if (uiState.connectionState == ConnectionState.REGISTERING) IliadBlue else if (uiState.connectionState == ConnectionState.ERROR) MaterialTheme.colorScheme.error else if (step1Completed) IliadBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                    isActive = step1Completed && !step2Completed,
                    isCompleted = step2Completed,
                    showLine = true
                ) {
                    if (step2Completed) {
                        Column {
                            Text("ID Tracciamento: ${uiState.trackId}", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            Text("Token App: Memorizzato in sicurezza", fontSize = 11.sp, color = EmeraldGreen, fontWeight = FontWeight.Medium)
                        }
                    } else {
                        Button(
                            onClick = { viewModel.registerApp() },
                            modifier = Modifier.fillMaxWidth().testTag("submit_button"),
                            shape = RoundedCornerShape(100.dp),
                            enabled = step1Completed && !uiState.isBusy,
                            colors = ButtonDefaults.buttonColors(containerColor = IliadRed)
                        ) {
                            if (uiState.isBusy && uiState.connectionState == ConnectionState.REGISTERING) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                            } else {
                                Text("Registra e Richiedi Token", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // PASSO 3
                val isStep3Active = uiState.connectionState == ConnectionState.PENDING_BOX_AUTH || (step2Completed && !step3Completed)
                val liveStatusColor = when (uiState.authStatus) {
                    "granted" -> EmeraldGreen
                    "pending" -> AmberWarning
                    "denied" -> IliadRed
                    "timeout" -> Color(0xFF757575)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                HandshakeStep(
                    stepNumber = 3,
                    title = "Approvazione Display Iliadbox",
                    description = "Guarda il display touch della tua Iliadbox e tocca 'SÌ' o la freccia verde per autorizzare l'app.",
                    statusText = if (step3Completed) "APPROVATO" else if (uiState.authStatus.isNotEmpty()) uiState.authStatus.uppercase() else if (isStep3Active) "IN ATTESA..." else "BLOCCATO",
                    statusColor = if (step3Completed) EmeraldGreen else liveStatusColor,
                    isActive = isStep3Active,
                    isCompleted = step3Completed,
                    showLine = true
                ) {
                    if (isStep3Active && !step3Completed) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(liveStatusColor.copy(alpha = 0.12f))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "Stato: ${uiState.authStatus.ifEmpty { "in attesa di conferma sul display..." }}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = liveStatusColor
                                )
                            }

                            Button(
                                onClick = { viewModel.checkAuthStatus() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(100.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                enabled = !uiState.isBusy
                            ) {
                                Text("Verifica Progresso Ora", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    } else if (step3Completed) {
                        Text("Autorizzato con successo!", color = EmeraldGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Text("In attesa della registrazione.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // PASSO 4
                HandshakeStep(
                    stepNumber = 4,
                    title = "Sessione di Controllo Cifrata",
                    description = "Apertura della sessione HMAC-SHA1 per il controllo completo del router.",
                    statusText = if (step4Completed) "CONNESSO" else if (step3Completed) "PRONTO" else "BLOCCATO",
                    statusColor = if (step4Completed) EmeraldGreen else if (step3Completed) IliadBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                    isActive = step3Completed && !step4Completed,
                    isCompleted = step4Completed,
                    showLine = false
                ) {
                    if (step4Completed) {
                        Column {
                            Text("Sessione Attiva", color = EmeraldGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Token di sessione valido e aggiornato", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        Button(
                            onClick = { viewModel.login() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(100.dp),
                            enabled = step3Completed && !uiState.isBusy,
                            colors = ButtonDefaults.buttonColors(containerColor = IliadRed)
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

        // Gestione Cache Token & Permessi
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(22.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "CREDENZIALI MEMORIZZATE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Token App Permanente", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(
                            text = if (uiState.appToken.isNotEmpty()) "Salvato (${uiState.appToken.take(8)}...)" else "Non presente",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = if (uiState.appToken.isNotEmpty()) EmeraldGreenLight else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = if (uiState.appToken.isNotEmpty()) "PROTETTO" else "MANCANTE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.appToken.isNotEmpty()) EmeraldGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Token Sessione", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(
                            text = if (uiState.sessionToken.isNotEmpty()) "Attivo (${uiState.sessionToken.take(8)}...)" else "Inattivo",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = if (uiState.sessionToken.isNotEmpty()) IliadBlueLight else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = if (uiState.sessionToken.isNotEmpty()) "ATTIVO" else "INATTIVO",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.sessionToken.isNotEmpty()) IliadBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.login() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(100.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                        enabled = !uiState.isBusy && uiState.appToken.isNotEmpty()
                    ) {
                        Text("Rinnova Sessione", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { viewModel.resetAppAuthorization() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(100.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                        enabled = !uiState.isBusy
                    ) {
                        Text("Reimposta Token", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Riavvio Router Box
        Button(
            onClick = onRebootRequest,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            Text("🔄 Riavvia Iliadbox", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        // Scheda Aggiornamenti OTA GitHub
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(22.dp)
        ) {
            val context = LocalContext.current
            var repoInput by remember(uiState.githubRepo) { mutableStateOf(uiState.githubRepo) }

            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "AGGIORNAMENTI OTA (GITHUB)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    val updateResult = uiState.updateCheckResult
                    val badgeText = when {
                        uiState.isCheckingUpdate -> "Verifica..."
                        updateResult?.isUpdateAvailable == true -> "Nuova v${updateResult.latestVersion}"
                        updateResult != null && updateResult.errorMessage == null -> "✓ Aggiornata"
                        updateResult?.errorMessage != null -> "Errore"
                        else -> "Disponibile"
                    }
                    val badgeColor = when {
                        updateResult?.isUpdateAvailable == true -> IliadRed
                        updateResult != null && updateResult.errorMessage == null -> EmeraldGreen
                        updateResult?.errorMessage != null -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }

                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = badgeColor
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = repoInput,
                    onValueChange = {
                        repoInput = it
                        viewModel.setGithubRepo(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Repository GitHub (owner/repo)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = IliadRed
                    )
                )

                Button(
                    onClick = { viewModel.checkForUpdates(forceCheck = false) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IliadRed),
                    enabled = !uiState.isCheckingUpdate
                ) {
                    if (uiState.isCheckingUpdate) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Controllo release in corso...")
                    } else {
                        Text("🚀 Verifica Aggiornamenti OTA", fontWeight = FontWeight.Bold)
                    }
                }

                // Risultato controllo OTA
                val result = uiState.updateCheckResult
                if (result != null && result.isUpdateAvailable) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Nuova release: ${result.latestVersion}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (result.release != null && result.release.apkSize > 0) {
                                    Text(
                                        text = formatBytes(result.release.apkSize),
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            when (val dlState = uiState.downloadState) {
                                is DownloadState.Downloading -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        LinearProgressIndicator(
                                            progress = { dlState.progress },
                                            modifier = Modifier.fillMaxWidth().height(6.dp),
                                            color = IliadRed
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Download APK in corso...",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "${(dlState.progress * 100).toInt()}%",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                is DownloadState.Success -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = EmeraldGreen.copy(alpha = 0.12f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("✅", fontSize = 16.sp, modifier = Modifier.padding(end = 6.dp))
                                                Text(
                                                    text = "APK scaricato con successo!",
                                                    color = EmeraldGreen,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }

                                        Button(
                                            onClick = { viewModel.installDownloadedApk(dlState.apkFile) },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(100.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                                        ) {
                                            Text("📲 Apri Installer e Aggiorna APK", fontWeight = FontWeight.Bold)
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = AmberWarning.copy(alpha = 0.12f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text(
                                                    text = "⚠️ Se compare 'Pacchetto in conflitto':",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.5.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "Android blocca l'aggiornamento quando l'app già installata ha una firma differente rispetto a questa release GitHub. Disinstalla prima l'app dal telefono e poi installa questo APK. I successivi aggiornamenti si installeranno normalmente.",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    lineHeight = 15.sp
                                                )
                                            }
                                        }
                                    }
                                }
                                is DownloadState.Error -> {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.errorContainer,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Errore: ${dlState.message}",
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            fontSize = 11.5.sp,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                    Button(
                                        onClick = { viewModel.downloadAndInstallUpdate(context) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(100.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = IliadRed)
                                    ) {
                                        Text("🔄 Riprova Download", fontWeight = FontWeight.Bold)
                                    }
                                }
                                else -> {
                                    Button(
                                        onClick = { viewModel.downloadAndInstallUpdate(context) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(100.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                                    ) {
                                        Text("📥 Scarica e Installa APK", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}
