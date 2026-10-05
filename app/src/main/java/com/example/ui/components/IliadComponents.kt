package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.api.LanHost
import com.example.ui.AppScreen
import com.example.ui.ConnectionState
import com.example.ui.theme.*

@Composable
fun IliadHeader(
    isSimulated: Boolean,
    connectionState: ConnectionState,
    boxModelName: String,
    currentScreen: AppScreen,
    isBusy: Boolean,
    onActionClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Iliadbox Connect logo & title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Stylized circular Iliadbox router shape
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(IliadRed),
                contentAlignment = Alignment.Center
            ) {
                // Curved TouchScreen representation inside logo
                Box(
                    modifier = Modifier
                        .size(22.dp, 8.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .background(Color.White)
                )
            }
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "iliad",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp,
                        color = IliadRed
                    )
                    Text(
                        text = "box",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusDotColor = when (connectionState) {
                        ConnectionState.CONNECTED -> EmeraldGreen
                        ConnectionState.PENDING_BOX_AUTH -> AmberWarning
                        ConnectionState.LOGGING_IN, ConnectionState.REGISTERING -> IliadBlue
                        ConnectionState.DISCONNECTED -> Color(0xFF9E9E9E)
                        ConnectionState.ERROR -> IliadRed
                    }
                    val statusLabel = when (connectionState) {
                        ConnectionState.CONNECTED -> "Connesso"
                        ConnectionState.PENDING_BOX_AUTH -> "In attesa"
                        ConnectionState.LOGGING_IN -> "Accesso in corso..."
                        ConnectionState.REGISTERING -> "Registrazione..."
                        else -> "Disconnesso"
                    }
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(statusDotColor)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (boxModelName.isNotEmpty()) "$statusLabel • $boxModelName" else statusLabel,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Action button (+ for downloads or Refresh)
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable { onActionClick() },
            contentAlignment = Alignment.Center
        ) {
            if (isBusy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                Icon(
                    imageVector = if (currentScreen == AppScreen.DOWNLOADS) Icons.Default.Add else Icons.Default.Refresh,
                    contentDescription = if (currentScreen == AppScreen.DOWNLOADS) "Aggiungi" else "Aggiorna",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }
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
        tonalElevation = 4.dp,
        modifier = Modifier.border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant))
    ) {
        val items = listOf(
            Triple(AppScreen.HOME, stringResource(R.string.title_home), "🏠"),
            Triple(AppScreen.DEVICES, stringResource(R.string.title_devices), "💻"),
            Triple(AppScreen.FILES, stringResource(R.string.title_files), "📁"),
            Triple(AppScreen.DOWNLOADS, stringResource(R.string.title_downloads), "🧲"),
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
                            .width(52.dp)
                            .height(28.dp)
                            .clip(RoundedCornerShape(100.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.third,
                            fontSize = 16.sp
                        )
                    }
                },
                label = {
                    Text(
                        text = item.second,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) IliadRed else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
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
fun DeviceRow(host: LanHost, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
                .size(42.dp)
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
                fontSize = 13.5.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val activeText = if (host.active == true) "Attivo ora" else "Disconnesso"
            val connType = host.connectivityType ?: "wifi"
            val ipText = host.ipAddress?.let { " • $it" } ?: ""
            Text(
                text = "$activeText • $connType$ipText",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (host.active == true) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = EmeraldGreen.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "Online",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldGreen,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
            )
        }
    }
}

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
            .height(IntrinsicSize.Min)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(32.dp)
                .fillMaxHeight()
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isCompleted -> EmeraldGreen
                            isActive -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Completato",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
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
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .padding(vertical = 4.dp)
                        .background(
                            if (isCompleted) EmeraldGreen.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
                        )
                )
            }
        }

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
                    color = if (isActive || isCompleted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )
                if (statusText.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(statusColor.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = statusText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            maxLines = 1,
                            softWrap = false
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
