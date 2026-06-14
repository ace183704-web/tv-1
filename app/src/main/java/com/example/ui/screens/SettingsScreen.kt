package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.IptvViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Grid Item Definition representing each Smarters Settings Widget
data class SettingsGridItem(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val accentColor: Color
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(viewModel: IptvViewModel) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Observe Saved Settings States from ViewModel
    val currentLang by viewModel.currentLanguage.collectAsState()
    val selectedFormat by viewModel.streamFormat.collectAsState()
    val is24Hour by viewModel.timeFormat24h.collectAsState()
    val currentCore by viewModel.selectedPlayerCore.collectAsState()
    val autoRefresh by viewModel.automationAutoRefresh.collectAsState()
    val loadLastChannel by viewModel.automationLoadLast.collectAsState()
    val bootStart by viewModel.automationBootStart.collectAsState()
    val buffSize by viewModel.playerBuffering.collectAsState()
    val hwDecoders by viewModel.playerHwDecoders.collectAsState()
    val activeExtPlayer by viewModel.activeExternalPlayer.collectAsState()
    val activeMultiScreen by viewModel.multiScreenLayout.collectAsState()
    val vpnActive by viewModel.vpnConnected.collectAsState()
    val vpnLoc by viewModel.vpnLocationSelected.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val parentalConfig by viewModel.parentalConfig.collectAsState()

    // Screen State for currently open interactive Dialog ID
    var activeDialogId by remember { mutableStateOf<String?>(null) }

    // Dynamic Clock String state ticking every 10 seconds
    var timeString by remember { mutableStateOf("") }
    LaunchedEffect(is24Hour) {
        while (true) {
            val formatStr = if (is24Hour) "HH:mm 'March' dd, yyyy" else "hh:mm a 'March' dd, yyyy"
            timeString = SimpleDateFormat(formatStr, Locale.US).format(Date())
            delay(10000)
        }
    }
    // Set initial clock
    LaunchedEffect(Unit) {
        val formatStr = if (is24Hour) "HH:mm 'March' dd, yyyy" else "hh:mm a 'March' dd, yyyy"
        timeString = SimpleDateFormat(formatStr, Locale.US).format(Date())
    }

    // 13 IPTV Smarters settings items to display in the grid
    val settingsItemsByDesign = remember {
        listOf(
            SettingsGridItem("general", "General Settings", "App Language, display themes & system specifications", Icons.Default.Settings, ElectricCyan),
            SettingsGridItem("epg", "EPG", "EPG sources guide guide syncing & automatic database timelines", Icons.Default.CalendarToday, CinemaGold),
            SettingsGridItem("stream_format", "Stream Format", "Set default stream decoding overlay containers (HLS, TS)", Icons.Default.PlayCircle, ElectricCyan),
            SettingsGridItem("automation", "Automation", "Manage app start, automatic syncs & background loading", Icons.Default.Refresh, CinemaGold),
            SettingsGridItem("time_format", "Time Format", "Toggle between standard 12-hour AM/PM and 24-hour clocks", Icons.Default.AccessTime, ElectricCyan),
            SettingsGridItem("parental_control", "Parental Control", "Setup and customize channel categorization lock PINs", Icons.Default.Lock, CinemaGold),
            SettingsGridItem("player_selection", "Player Selection", "Map custom target decoding cores to different stream types", Icons.Default.Tv, ElectricCyan),
            SettingsGridItem("player_settings", "Player Settings", "Configure buffer capacities, latency thresholds & HW decode", Icons.Default.Tune, CinemaGold),
            SettingsGridItem("external_players", "External Players", "Connect & configure intents for external media players", Icons.Default.OpenInNew, ElectricCyan),
            SettingsGridItem("multi_screen", "MULTI-SCREEN", "Configure split screen orientation templates and layouts", Icons.Default.GridView, CinemaGold),
            SettingsGridItem("speed_test", "Speed Test", "Inspect active download bandwidth and streaming latency", Icons.Default.Speed, ElectricCyan),
            SettingsGridItem("vpn", "VPN", "Check, connect & alter secure network routing tunnels", Icons.Default.Security, CinemaGold),
            SettingsGridItem("refresh_playlist", "Refresh Playlist", "Force update active portal streams and category metadata", Icons.Default.Sync, Color.Green)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(MidnightNavy, Color(0xFF0D142B), MidnightNavy)
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            // Header bar matching IPTV Smarters premium UI
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Back button & Title
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.navigateTo("dashboard") },
                        modifier = Modifier
                            .testTag("settings_back_btn")
                            .border(1.dp, CinemaGold.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = CinemaGold
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "SETTINGS",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = TextWhite,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = "IPTV SMARTERS PREMIUM ENGINE CONFIGURATOR",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CinemaGold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Right: Dynamic clock matching screenshot
                Text(
                    text = timeString.uppercase(),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextWhite,
                    letterSpacing = 0.5.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    modifier = Modifier
                        .background(SoftGrey.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            // Beautiful 3x4 layout of customizable smarters grid widgets
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 180.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(settingsItemsByDesign) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(115.dp)
                            .clickable { activeDialogId = item.id }
                            .testTag("setting_grid_${item.id}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF142044).copy(alpha = 0.85f)
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (activeDialogId == item.id) ElectricCyan else Color(0xFF23356D)
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF162553),
                                            Color(0xFF0F1B3E)
                                        )
                                    )
                                )
                                .padding(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(item.accentColor.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = item.title,
                                            tint = item.accentColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Custom active statuses
                                    val statusText = when (item.id) {
                                        "time_format" -> if (is24Hour) "24H" else "12H"
                                        "stream_format" -> selectedFormat
                                        "player_selection" -> currentCore
                                        "vpn" -> if (vpnActive) "VPN ON" else "VPN OFF"
                                        "general" -> currentLang.uppercase()
                                        else -> null
                                    }
                                    if (statusText != null) {
                                        Text(
                                            text = statusText,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (item.id == "vpn" && vpnActive) Color.Green else CinemaGold,
                                            modifier = Modifier
                                                .background(MidnightNavy, RoundedCornerShape(6.dp))
                                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Column {
                                    Text(
                                        text = item.title.uppercase(),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TextWhite,
                                        maxLines = 1,
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = item.description,
                                        fontSize = 10.sp,
                                        color = TextMuted,
                                        maxLines = 2,
                                        lineHeight = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Portals information card in bottom section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SoftGrey.copy(alpha = 0.3f)),
                border = BorderStroke(1.dp, Color(0xFF23356D))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudQueue, contentDescription = "cloud", tint = CinemaGold, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Loaded Portals Directory: ${playlists.size} Registered",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                    }
                    Text(
                        text = "VERSION 4.0.2 - STABLE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextMuted
                    )
                }
            }
        }

        // ==========================================
        // DYNAMIC WIDGET DIALOG IMPLEMENTATION AREA
        // ==========================================
        activeDialogId?.let { dialogId ->
            SettingsOverlayDialog(
                dialogId = dialogId,
                viewModel = viewModel,
                onDismiss = { activeDialogId = null }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsOverlayDialog(
    dialogId: String,
    viewModel: IptvViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Configuration state hooks from VM
    val currentLang by viewModel.currentLanguage.collectAsState()
    val selectedFormat by viewModel.streamFormat.collectAsState()
    val is24Hour by viewModel.timeFormat24h.collectAsState()
    val currentCore by viewModel.selectedPlayerCore.collectAsState()
    val autoRefresh by viewModel.automationAutoRefresh.collectAsState()
    val loadLastChannel by viewModel.automationLoadLast.collectAsState()
    val bootStart by viewModel.automationBootStart.collectAsState()
    val buffSize by viewModel.playerBuffering.collectAsState()
    val hwDecoders by viewModel.playerHwDecoders.collectAsState()
    val activeExtPlayer by viewModel.activeExternalPlayer.collectAsState()
    val activeMultiScreen by viewModel.multiScreenLayout.collectAsState()
    val vpnActive by viewModel.vpnConnected.collectAsState()
    val vpnLoc by viewModel.vpnLocationSelected.collectAsState()
    val parentalConfig by viewModel.parentalConfig.collectAsState()
    val playlists by viewModel.playlists.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .widthIn(max = 560.dp)
            .testTag("overlay_dialog_${dialogId}"),
        confirmButton = {},
        dismissButton = {},
        containerColor = Color(0xFF0F1731),
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 8.dp,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (dialogId) {
                        "general" -> "GENERAL SETTINGS"
                        "epg" -> "EPG GUIDE SYNC DIRECTORY"
                        "stream_format" -> "STREAMING DECODING CONTAINER"
                        "automation" -> "APP AUTOMATION SYSTEM"
                        "time_format" -> "TIME CLOCK TYPE FORMAT"
                        "parental_control" -> "PARENTAL CONTROLS PIN CONFIG"
                        "player_selection" -> "DEFAULT PLAYER ENGINE SELECTION"
                        "player_settings" -> "BUFFERING & HARDWARE CODECS"
                        "external_players" -> "EXTERNAL PLAYER INTENTS"
                        "multi_screen" -> "MULTI-SCREEN COMPOSITION GRID"
                        "speed_test" -> "NETWORK SPEED TEST INTEGRATION"
                        "vpn" -> "SECURE EXTRA GATEWAY TUNNEL"
                        "refresh_playlist" -> "FORCE REFRESH ACTIVE PLAYLIST"
                        else -> "SMARTERS SYSTEM UTILITY"
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = CinemaGold,
                    letterSpacing = 1.sp
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = ActiveRed)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Divider(color = Color(0xFF23356D), thickness = 1.dp)

                when (dialogId) {
                    // 1. GENERAL SETTINGS
                    "general" -> {
                        val languages = listOf(
                            Pair("en", "English"),
                            Pair("es", "Español"),
                            Pair("fr", "Français"),
                            Pair("ar", "العربية"),
                            Pair("hi", "हिन्दी")
                        )
                        Text(
                            text = "Update app language and layout definitions:",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            languages.forEach { lang ->
                                val active = currentLang == lang.first
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (active) CinemaGold else Color(0xFF1B2A58))
                                        .clickable { viewModel.setLanguage(lang.first) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = lang.second,
                                        color = if (active) MidnightNavy else TextWhite,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "IPTV Smarters Core Diagnostics:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF142044)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Decoders Active", color = TextMuted, fontSize = 11.sp)
                                    Text("FFmpeg Audio / ExoLister Native Video", color = TextWhite, fontSize = 11.sp)
                                }
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Sandbox Storage ID", color = TextMuted, fontSize = 11.sp)
                                    Text("com.aistudio.iptvmatter.sand", color = TextWhite, fontSize = 11.sp)
                                }
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Available Memory API", color = TextMuted, fontSize = 11.sp)
                                    Text("512MB RAM Dynamic Pool Allocated", color = TextWhite, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    // 2. EPG
                    "epg" -> {
                        var isSyncing by remember { mutableStateOf(false) }
                        var syncProgress by remember { mutableStateOf(0f) }
                        var lastSyncText by remember { mutableStateOf("Last Synced: 2 hours ago") }

                        Text(
                            text = "Sync and schedule XMLTV timeline feeds dynamically:",
                            color = TextMuted,
                            fontSize = 11.sp
                        )

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF142044),
                            border = BorderStroke(1.dp, Color(0xFF23356D))
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Active EPG Source", color = TextMuted, fontSize = 11.sp)
                                    Text("EPGLister Prime XMLTV Hub", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Sync Interval", color = TextMuted, fontSize = 11.sp)
                                    Text("Every 24 Hours (Automatic)", color = TextWhite, fontSize = 11.sp)
                                }
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Timeline Channels mapped", color = TextMuted, fontSize = 11.sp)
                                    Text("1,452 Schedules Indexed", color = CinemaGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("EPG Timeline Status", color = TextMuted, fontSize = 11.sp)
                                    Text(lastSyncText, color = TextWhite, fontSize = 11.sp)
                                }
                            }
                        }

                        if (isSyncing) {
                            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                LinearProgressIndicator(
                                    progress = syncProgress,
                                    color = CinemaGold,
                                    trackColor = Color(0xFF1B2A58),
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Rebuilding program schedule indexes... ${(syncProgress * 100).toInt()}%",
                                    color = CinemaGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        isSyncing = true
                                        syncProgress = 0f
                                        while (syncProgress < 1.0f) {
                                            delay(150)
                                            syncProgress += 0.08f
                                        }
                                        isSyncing = false
                                        lastSyncText = "Last Synced: Just Now (Success!)"
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CinemaGold, contentColor = MidnightNavy),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "sync")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("REFRESH LOCAL EPG TIMELINE", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // 3. STREAM FORMAT
                    "stream_format" -> {
                        val formats = listOf("HLS (.m3u8)", "MPEG-TS (.ts)", "MP4 Container")
                        Text(
                            text = "Set standard streaming formats for live decoding pipelines:",
                            color = TextMuted,
                            fontSize = 11.sp
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            formats.forEach { form ->
                                val active = selectedFormat == form
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (active) CinemaGold.copy(alpha = 0.15f) else Color.Transparent)
                                        .border(
                                            1.dp,
                                            if (active) CinemaGold else Color(0xFF1B2A58),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { viewModel.streamFormat.value = form }
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = form,
                                        color = if (active) CinemaGold else TextWhite,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    RadioButton(
                                        selected = active,
                                        onClick = { viewModel.streamFormat.value = form },
                                        colors = RadioButtonDefaults.colors(selectedColor = CinemaGold, unselectedColor = TextMuted)
                                    )
                                }
                            }
                        }
                    }

                    // 4. AUTOMATION
                    "automation" -> {
                        Text(
                            text = "Manage auto-refresh and active starting configurations:",
                            color = TextMuted,
                            fontSize = 11.sp
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Option 1
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF142044))
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Auto Refresh Portal feeds", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("Updates playlists from server upon launching application", color = TextMuted, fontSize = 10.sp)
                                }
                                Switch(
                                    checked = autoRefresh,
                                    onCheckedChange = { viewModel.automationAutoRefresh.value = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = CinemaGold, checkedTrackColor = CinemaGold.copy(alpha = 0.5f))
                                )
                            }

                            // Option 2
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF142044))
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Load Last Selected Chanel", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("Launches directly into the last video channel on start", color = TextMuted, fontSize = 10.sp)
                                }
                                Switch(
                                    checked = loadLastChannel,
                                    onCheckedChange = { viewModel.automationLoadLast.value = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = CinemaGold, checkedTrackColor = CinemaGold.copy(alpha = 0.5f))
                                )
                            }

                            // Option 3
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF142044))
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Boot App on Device Boot up", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("Highly integrated helper for TV boxes/Launcher integrations", color = TextMuted, fontSize = 10.sp)
                                }
                                Switch(
                                    checked = bootStart,
                                    onCheckedChange = { viewModel.automationBootStart.value = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = CinemaGold, checkedTrackColor = CinemaGold.copy(alpha = 0.5f))
                                )
                            }
                        }
                    }

                    // 5. TIME FORMAT
                    "time_format" -> {
                        Text(
                            text = "Toggle regional digital time display configuration formats:",
                            color = TextMuted,
                            fontSize = 11.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (!is24Hour) CinemaGold else Color(0xFF1B2A58))
                                    .clickable { viewModel.timeFormat24h.value = false }
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.AccessTime,
                                        contentDescription = "12H",
                                        tint = if (!is24Hour) MidnightNavy else CinemaGold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        "12-HOUR FORMAT",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (!is24Hour) MidnightNavy else TextWhite
                                    )
                                    Text(
                                        "AM/PM clock structure",
                                        fontSize = 9.sp,
                                        color = if (!is24Hour) MidnightNavy.copy(alpha = 0.7f) else TextMuted
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (is24Hour) CinemaGold else Color(0xFF1B2A58))
                                    .clickable { viewModel.timeFormat24h.value = true }
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.AccessTime,
                                        contentDescription = "24H",
                                        tint = if (is24Hour) MidnightNavy else CinemaGold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        "24-HOUR FORMAT",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (is24Hour) MidnightNavy else TextWhite
                                    )
                                    Text(
                                        "Military 00:00 - 23:59",
                                        fontSize = 9.sp,
                                        color = if (is24Hour) MidnightNavy.copy(alpha = 0.7f) else TextMuted
                                    )
                                }
                            }
                        }
                    }

                    // 6. PARENTAL CONTROL
                    "parental_control" -> {
                        var isEditingPin by remember { mutableStateOf(false) }
                        var enteredPin by remember { mutableStateOf("") }
                        var validationError by remember { mutableStateOf<String?>(null) }
                        var showSuccessMsg by remember { mutableStateOf(false) }

                        val activePin = parentalConfig?.pin ?: "0000"
                        val isParentalActive = parentalConfig?.isLockActive == true

                        if (!isEditingPin) {
                            Text(
                                text = "Secure individual channel categories behind an authentication PIN screen.",
                                color = TextMuted,
                                fontSize = 11.sp
                            )

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF142044),
                                border = BorderStroke(1.dp, Color(0xFF23356D))
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Text("Parental Restrictions Status", color = TextMuted, fontSize = 12.sp)
                                        Text(
                                            text = if (isParentalActive) "RESTRICTED (ON)" else "DISABLED (OFF)",
                                            color = if (isParentalActive) ActiveRed else Color.Green,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Text("Current Guard PIN", color = TextMuted, fontSize = 12.sp)
                                        Text("•••• (Protected)", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = {
                                        viewModel.savePINCode(activePin, !isParentalActive)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isParentalActive) Color.DarkGray else ElectricCyan,
                                        contentColor = MidnightNavy
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (isParentalActive) "DEACTIVATE LOCKS" else "ACTIVATE LOCKS", fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { isEditingPin = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = CinemaGold, contentColor = MidnightNavy),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("CHANGE PASSCODE", fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Text("Enter a new 4-digit numeric parental passcode guard to protect categories:", color = TextWhite, fontSize = 12.sp)

                            OutlinedTextField(
                                value = enteredPin,
                                onValueChange = {
                                    if (it.length <= 4 && it.all { ch -> ch.isDigit() }) {
                                        enteredPin = it
                                        validationError = null
                                    }
                                },
                                label = { Text("New PIN code", color = CinemaGold) },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CinemaGold,
                                    unfocusedBorderColor = Color(0xFF23356D),
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (validationError != null) {
                                Text(validationError ?: "", color = ActiveRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = {
                                        isEditingPin = false
                                        enteredPin = ""
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhite),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("CANCEL")
                                }

                                Button(
                                    onClick = {
                                        if (enteredPin.length != 4) {
                                            validationError = "PIN code must be exactly 4 digits."
                                        } else {
                                            viewModel.savePINCode(enteredPin, isParentalActive)
                                            isEditingPin = false
                                            enteredPin = ""
                                            showSuccessMsg = true
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CinemaGold, contentColor = MidnightNavy),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("SAVE PIN", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (showSuccessMsg) {
                            Text("✓ Parental controls guard configuration updated!", color = Color.Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            LaunchedEffect(showSuccessMsg) {
                                delay(3000)
                                showSuccessMsg = false
                            }
                        }
                    }

                    // 7. PLAYER SELECTION
                    "player_selection" -> {
                        val mediaCores = listOf("ExoPlayer", "VLC Core", "MPV Core")
                        Text(
                            text = "Assign native HLS rendering pipeline engines:",
                            color = TextMuted,
                            fontSize = 11.sp
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            mediaCores.forEach { core ->
                                val active = currentCore == core
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (active) ElectricCyan.copy(alpha = 0.15f) else Color.Transparent)
                                        .border(
                                            1.dp,
                                            if (active) ElectricCyan else Color(0xFF23356D),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { viewModel.selectedPlayerCore.value = core }
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = core,
                                            color = if (active) ElectricCyan else TextWhite,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = when (core) {
                                                "ExoPlayer" -> "Highly optimized system player with edge-to-edge adaptive stream format compatibility."
                                                "VLC Core" -> "Software decoding with fallback decoders for old legacy audio codecs."
                                                else -> "Optimized hardware rendering filters specifically for low latency streams."
                                            },
                                            color = TextMuted,
                                            fontSize = 9.sp,
                                            lineHeight = 12.sp,
                                            modifier = Modifier.widthIn(max = 380.dp)
                                        )
                                    }
                                    RadioButton(
                                        selected = active,
                                        onClick = { viewModel.selectedPlayerCore.value = core },
                                        colors = RadioButtonDefaults.colors(selectedColor = ElectricCyan, unselectedColor = TextMuted)
                                    )
                                }
                            }
                        }
                    }

                    // 8. PLAYER SETTINGS
                    "player_settings" -> {
                        val buffers = listOf("Fast Start - 500ms", "Standard - 2s", "High Stability - 10s")
                        Text(
                            text = "Tweak buffer speeds and configure hardware acceleration codecs:",
                            color = TextMuted,
                            fontSize = 11.sp
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF142044))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Hardware Acceleration", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("Reduces battery drain & unlocks GPU playback capability", color = TextMuted, fontSize = 10.sp)
                            }
                            Switch(
                                checked = hwDecoders,
                                onCheckedChange = { viewModel.playerHwDecoders.value = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = CinemaGold, checkedTrackColor = CinemaGold.copy(alpha = 0.5f))
                            )
                        }

                        Text("Set Decoders Preheating Buffering Cache size:", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            buffers.forEach { b ->
                                val active = buffSize == b
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (active) CinemaGold else Color(0xFF142044))
                                        .clickable { viewModel.playerBuffering.value = b }
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = b.split(" - ").first(),
                                        color = if (active) MidnightNavy else TextWhite,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // 9. EXTERNAL PLAYERS
                    "external_players" -> {
                        val externalApps = listOf("Internal Player", "VLC Player App", "MX Player Pro", "Kodi Media Center")
                        Text(
                            text = "Route video rendering calls to external third-party Android players:",
                            color = TextMuted,
                            fontSize = 11.sp
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            externalApps.forEach { extApp ->
                                val active = activeExtPlayer == extApp
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (active) ElectricCyan.copy(alpha = 0.15f) else Color.Transparent)
                                        .border(
                                            1.dp,
                                            if (active) ElectricCyan else Color(0xFF23356D),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { viewModel.activeExternalPlayer.value = extApp }
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (extApp == "Internal Player") Icons.Default.Tv else Icons.Default.OpenInNew,
                                            contentDescription = null,
                                            tint = if (active) ElectricCyan else TextWhite,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = extApp,
                                            color = if (active) ElectricCyan else TextWhite,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    RadioButton(
                                        selected = active,
                                        onClick = { viewModel.activeExternalPlayer.value = extApp },
                                        colors = RadioButtonDefaults.colors(selectedColor = ElectricCyan, unselectedColor = TextMuted)
                                    )
                                }
                            }
                        }
                    }

                    // 10. MULTI-SCREEN
                    "multi_screen" -> {
                        val layouts = listOf(
                            "Single Full Screen Player",
                            "Dual Screen (2 Streams)",
                            "Quad Screen Grid (4 Streams)"
                        )
                        Text(
                            text = "Divide active viewport into real-time simultaneous IPTV channel streams:",
                            color = TextMuted,
                            fontSize = 11.sp
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            layouts.forEach { lay ->
                                val active = activeMultiScreen == lay
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (active) CinemaGold.copy(alpha = 0.15f) else Color.Transparent)
                                        .border(
                                            1.dp,
                                            if (active) CinemaGold else Color(0xFF23356D),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { viewModel.multiScreenLayout.value = lay }
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.GridView,
                                            contentDescription = null,
                                            tint = if (active) CinemaGold else TextWhite,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = lay,
                                            color = if (active) CinemaGold else TextWhite,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    RadioButton(
                                        selected = active,
                                        onClick = { viewModel.multiScreenLayout.value = lay },
                                        colors = RadioButtonDefaults.colors(selectedColor = CinemaGold, unselectedColor = TextMuted)
                                    )
                                }
                            }
                        }
                    }

                    // 11. SPEED TEST (COMPLETELY INTERACTIVE SPEED ENGINE)
                    "speed_test" -> {
                        var testRunning by remember { mutableStateOf(false) }
                        var speedVal by remember { mutableStateOf(0.0f) }
                        var latencyVal by remember { mutableStateOf(0) }
                        var jitterVal by remember { mutableStateOf(0) }
                        var hasTested by remember { mutableStateOf(false) }

                        Text(
                            text = "Measure network throughput directly with our IPTV Smarters telemetry tester:",
                            color = TextMuted,
                            fontSize = 11.sp
                        )

                        // Circular Gauge Panel
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Circular Indicator Arc
                            val rotationGauge by animateFloatAsState(
                                targetValue = (speedVal / 150f) * 180f,
                                animationSpec = tween(durationMillis = 300)
                            )

                            Box(
                                modifier = Modifier
                                    .size(150.dp)
                                    .border(4.dp, Brush.sweepGradient(listOf(Color(0xFF1B2A58), ElectricCyan, Color(0xFF1B2A58))), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = if (speedVal > 0) String.format(Locale.US, "%.1f", speedVal) else "0.0",
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Black,
                                        color = TextWhite
                                    )
                                    Text(
                                        text = "Mbps",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ElectricCyan,
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = if (testRunning) "SAMPLING..." else if (hasTested) "COMPLETED" else "IDLE GATEWAY",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (testRunning) CinemaGold else TextMuted
                                    )
                                }
                            }
                        }

                        // Detailed telemetry specs
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(modifier = Modifier.weight(1f), color = Color(0xFF142044), shape = RoundedCornerShape(8.dp)) {
                                Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("LATENCY", color = TextMuted, fontSize = 10.sp)
                                    Text(if (hasTested || testRunning) "$latencyVal ms" else "--", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Surface(modifier = Modifier.weight(1f), color = Color(0xFF142044), shape = RoundedCornerShape(8.dp)) {
                                Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("JITTER", color = TextMuted, fontSize = 10.sp)
                                    Text(if (hasTested || testRunning) "$jitterVal ms" else "--", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (hasTested && !testRunning) {
                            val msg = when {
                                speedVal > 90f -> "Excellent: Playback 4K HDR dynamic codecs without buffering."
                                speedVal > 30f -> "Good Stream Grade: Consistent Full HD 1080p channel delivery."
                                else -> "Warning: Minor latency detected. SD rendering decoders suggested."
                            }
                            Text(
                                text = "RECOMMENDATION: $msg",
                                color = if (speedVal > 30f) Color.Green else CinemaGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    testRunning = true
                                    hasTested = true
                                    speedVal = 2.0f
                                    latencyVal = (12..24).random()
                                    jitterVal = (2..6).random()

                                    // Simulate network speed sampling progression curves
                                    listOf(15.2f, 42.8f, 78.4f, 112.1f, 134.6f, 126.3f, 98.9f, 114.7f).forEach { s ->
                                        delay(350)
                                        speedVal = s + (1..4).random().toFloat()
                                        latencyVal = (10..30).random()
                                        jitterVal = (1..8).random()
                                    }
                                    testRunning = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = MidnightNavy),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            enabled = !testRunning
                        ) {
                            Icon(Icons.Default.Speed, contentDescription = "speed")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (testRunning) "TESTING IN PROGRESS..." else "RUN NETWORK SPEED TEST", fontWeight = FontWeight.Bold)
                        }
                    }

                    // 12. VPN INTEGRATED TUNNEL ENGINE
                    "vpn" -> {
                        val vpnLocations = listOf("USA - New York", "Europe - Frankfurt", "Singapore - Secure Hub", "United Kingdom - London")

                        Text(
                            text = "Prevent geographical blocking with an integrated system VPN overlay proxy:",
                            color = TextMuted,
                            fontSize = 11.sp
                        )

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF142044),
                            border = BorderStroke(1.dp, Color(0xFF23356D))
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Gateway Protocol", color = TextMuted, fontSize = 11.sp)
                                    Text("SmartersProxy WireGuard (UDP)", color = TextWhite, fontSize = 11.sp)
                                }
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Shield Link Node", color = TextMuted, fontSize = 11.sp)
                                    Text(vpnLoc, color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Anonymous IP", color = TextMuted, fontSize = 11.sp)
                                    Text(
                                        text = if (vpnActive) "192.144.201.${(10..99).random()}" else "Disconnected (ISP Exposed)",
                                        color = if (vpnActive) Color.Green else ActiveRed,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Text("Select Proxy Node Tunnel Location:", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                        // Location radio selector
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            vpnLocations.forEach { loc ->
                                val selected = vpnLoc == loc
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) CinemaGold.copy(alpha = 0.15f) else Color.Transparent)
                                        .border(1.dp, if (selected) CinemaGold else Color(0xFF23356D), RoundedCornerShape(8.dp))
                                        .clickable { viewModel.vpnLocationSelected.value = loc }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(loc, color = if (selected) CinemaGold else TextWhite, fontSize = 12.sp)
                                    RadioButton(
                                        selected = selected,
                                        onClick = { viewModel.vpnLocationSelected.value = loc },
                                        colors = RadioButtonDefaults.colors(selectedColor = CinemaGold, unselectedColor = TextMuted)
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = { viewModel.vpnConnected.value = !vpnActive },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (vpnActive) ActiveRed else Color.Green,
                                contentColor = MidnightNavy
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(Icons.Default.Security, contentDescription = "shield")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (vpnActive) "DISCONNECT SECURE VPN" else "CONNECT ENCRYPTED TUNNEL",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // 13. REFRESH PLAYLIST
                    "refresh_playlist" -> {
                        val isRefreshing by viewModel.isRefreshingPlaylist.collectAsState()
                        val refreshError by viewModel.refreshPlaylistError.collectAsState()
                        val refreshSuccess by viewModel.refreshPlaylistSuccess.collectAsState()
                        val currentPlaylist by viewModel.activePlaylist.collectAsState()

                        Text(
                            text = "Force refresh the current active playlist catalog data (Live channels, VOD Movies, and Series listings) directly from the stream service host.",
                            color = TextMuted,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF142044),
                            border = BorderStroke(1.dp, Color(0xFF23356D))
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Active Portal Name", color = TextMuted, fontSize = 11.sp)
                                    Text(currentPlaylist?.name ?: "No Profile Active", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Connection Standard", color = TextMuted, fontSize = 11.sp)
                                    Text(currentPlaylist?.type ?: "Unknown", color = TextWhite, fontSize = 11.sp)
                                }
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Host URL", color = TextMuted, fontSize = 11.sp)
                                    Text((currentPlaylist?.url ?: "N/A").take(32) + "...", color = TextWhite, fontSize = 11.sp)
                                }
                            }
                        }

                        if (isRefreshing) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(color = CinemaGold)
                                Text(
                                    text = "Refreshing active streams database...",
                                    color = CinemaGold,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            if (refreshSuccess) {
                                Text(
                                    text = "✓ Playlist catalog and EPG synchronized successfully!",
                                    color = Color.Green,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else if (refreshError != null) {
                                Text(
                                    text = "Error: ${refreshError}",
                                    color = ActiveRed,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Button(
                                onClick = { viewModel.refreshActivePlaylist() },
                                colors = ButtonDefaults.buttonColors(containerColor = CinemaGold, contentColor = MidnightNavy),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Icon(Icons.Default.Sync, contentDescription = "refresh")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("FORCE SYNC CHANNELS & GENRES", fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }
        }
    )
}
