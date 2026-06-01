package com.example.ui.screens

import android.view.ViewGroup
import android.widget.FrameLayout
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.ui.IptvViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlayerScreen(viewModel: IptvViewModel) {
    val context = LocalContext.current
    val stream by viewModel.selectedStream.collectAsState()
    val activeCore by viewModel.selectedPlayerCore.collectAsState()
    val epgPrograms by viewModel.currentEpgList.collectAsState()
    val streamsList by viewModel.streamsList.collectAsState()
    
    var isPlaying by remember { mutableStateOf(true) }
    var showOverlay by remember { mutableStateOf(true) }
    var currentAspectIndex by remember { mutableStateOf(0) }
    val aspects = listOf("Auto Fit", "16:9 Cinema", "4:3 Classic", "Zoom Fill")
    
    // Core parameters for VLC/MPV mock engines
    var vlcDeinterlace by remember { mutableStateOf(false) }
    var vlcVolumeLevel by remember { mutableStateOf(85) }
    var mpvSpeedIndex by remember { mutableStateOf(1) }
    val mpvSpeeds = listOf(0.5f, 1.0f, 1.5f, 2.0f)
    var mpvSubtitleDelay by remember { mutableStateOf(0) } // millis
    
    var epgSidebarVisible by remember { mutableStateOf(false) }

    // Screen Lock capability
    var isLocked by remember { mutableStateOf(false) }
    var lockPressedOnce by remember { mutableStateOf(false) }

    // Extra volume and brightness adjustments
    var audioLevel by remember { mutableStateOf(0.70f) }
    var brightnessLevel by remember { mutableStateOf(0.65f) }

    // Dynamic stream configuration selections
    val qualityOptions = listOf("Auto VQ", "1080p FHD", "720p HD", "480p SD")
    var selectedQualityIndex by remember { mutableStateOf(1) } // Default: 1080p FHD

    val audioOptions = listOf("Stereo 2.0", "Surround 5.1", "Native ENG")
    var selectedAudioIndex by remember { mutableStateOf(0) }

    val subtitleOptions = listOf("Sub: Off", "Sub: Eng cc", "Sub: Spa cc")
    var selectedSubtitleIndex by remember { mutableStateOf(0) }

    // Broadcast stream dynamic simulation stats
    var simBufferTime by remember { mutableStateOf(0.18f) }
    var simBitrate by remember { mutableStateOf(5.4f) }
    var simFps by remember { mutableStateOf(60) }

    // Toast feedback overlay state
    var toastMessage by remember { mutableStateOf<String?>(null) }

    // Media3 ExoPlayer Instance
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    val playerListener = remember {
        object : androidx.media3.common.Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Log.e("PlayerScreen", "ExoPlayer playback error: ${error.message}", error)
                toastMessage = "Playback Error: ${error.localizedMessage ?: "Invalid Stream format"}"
                isPlaying = false
            }
        }
    }

    // Connect player source lifecycle
    LaunchedEffect(stream) {
        stream?.let {
            if (it.streamUrl.isNotBlank() && (it.streamUrl.startsWith("http://") || it.streamUrl.startsWith("https://") || it.streamUrl.startsWith("rtsp://") || it.streamUrl.startsWith("content://") || it.streamUrl.startsWith("file://"))) {
                try {
                    val mediaItem = MediaItem.fromUri(it.streamUrl)
                    exoPlayer.setMediaItem(mediaItem)
                    exoPlayer.prepare()
                    exoPlayer.play()
                    isPlaying = true
                    
                    // Show a greeting channel toast load
                    toastMessage = "Playing: ${it.name}"
                } catch (e: Exception) {
                    Log.e("PlayerScreen", "Error playing stream URL: ${it.streamUrl}", e)
                    toastMessage = "Failed to parse stream URL"
                    isPlaying = false
                }
            } else {
                toastMessage = "Stream URL is invalid or malformed"
                isPlaying = false
            }
        }
    }

    // Dynamic simulation update loop (Buffer health and speed flicker)
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            delay(3000)
            simBufferTime = (0.10f + Math.random() * 0.25).toFloat()
            simBitrate = (4.8f + Math.random() * 1.6).toFloat()
            simFps = if (Math.random() > 0.05) 60 else 59
        }
    }

    // Toast Timer dismisser
    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(2800)
            toastMessage = null
        }
    }

    // Clean disposal of core resources
    DisposableEffect(Unit) {
        exoPlayer.addListener(playerListener)
        onDispose {
            exoPlayer.removeListener(playerListener)
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // Video View Renderer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (!isLocked) {
                        showOverlay = !showOverlay
                    } else {
                        // Flash lock guide
                        lockPressedOnce = true
                    }
                }
                .testTag("video_player_surface"),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false // Use custom gorgeous Compose overlay HUD Instead
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { view ->
                    // Set PlayerView resizing style based on active index
                    view.resizeMode = when (currentAspectIndex) {
                        0 -> 0 // AspectRatioFrameLayout.RESIZE_MODE_FIT
                        1 -> 3 // AspectRatioFrameLayout.RESIZE_MODE_FILL
                        2 -> 4 // AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        else -> 0
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            
            // Render simulation overlay depending on selected Aspect Ratios
            val scalingDesc = aspects[currentAspectIndex]
            if (scalingDesc != "Auto Fit") {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.02f))
                ) {
                    Text(
                        text = "Aspect: $scalingDesc",
                        color = CinemaGold.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 96.dp, end = 24.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Ambient Dark Cover on Overlays
        AnimatedVisibility(
            visible = showOverlay && !isLocked,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            )
        }

        // LEFT SIDEBAR BRIGHTNESS CARD LEVEL CONTROLS
        AnimatedVisibility(
            visible = showOverlay && !isLocked,
            enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 20.dp)
        ) {
            VerticalControlCard(
                title = "Brightness",
                level = brightnessLevel,
                onLevelChange = { brightnessLevel = it },
                activeColor = CinemaGold,
                icon = Icons.Default.Brightness5,
                testTagPrefix = "brightness"
            )
        }

        // RIGHT SIDEBAR AUDIO LEVEL CONTROLS
        AnimatedVisibility(
            visible = showOverlay && !isLocked,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 20.dp)
        ) {
            VerticalControlCard(
                title = "Volume",
                level = audioLevel,
                onLevelChange = { 
                    audioLevel = it
                    // Update exoplayer sound limit too
                    exoPlayer.volume = it
                },
                activeColor = ElectricCyan,
                icon = if (audioLevel == 0f) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                testTagPrefix = "audio"
            )
        }

        // FLOATING LOCK INDICATOR/BUTTON IF SCREEN IS LOCKED
        AnimatedVisibility(
            visible = isLocked,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(24.dp)
        ) {
            Button(
                onClick = {
                    isLocked = false
                    showOverlay = true
                    toastMessage = "Player Controls Unlocked"
                },
                colors = ButtonDefaults.buttonColors(containerColor = ActiveRed),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                modifier = Modifier.testTag("unlock_screen_button")
            ) {
                Icon(imageVector = Icons.Default.Lock, contentDescription = "Locked badge", tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Tap to Unlock", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        // locked warning dialog tap helper
        LaunchedEffect(lockPressedOnce) {
            if (lockPressedOnce) {
                delay(2000)
                lockPressedOnce = false
            }
        }

        AnimatedVisibility(
            visible = isLocked && lockPressedOnce,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkOverlay.copy(alpha = 0.9f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, ActiveRed.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = "Lock Alert", tint = ActiveRed, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Screen Secured",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Click 'Tap to Unlock' button in top-right corner to open player controls.",
                        color = TextMuted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp).width(240.dp)
                    )
                }
            }
        }

        // IMMERSIVE HUD CONTROLS OVERLAY (TOP PANEL)
        // Hidden when locked to ensure pristine viewing
        AnimatedVisibility(
            visible = showOverlay && !isLocked,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.95f), Color.Transparent)
                        )
                    )
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.navigateTo("list_viewer") },
                        modifier = Modifier.testTag("player_close_button")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close Player", tint = Color.White)
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stream?.name ?: "IPTV Broadcasting stream",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            // Premium HD Stream Indicator
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(ElectricCyan)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "HD 1080P",
                                    color = MidnightNavy,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                        
                        Text(
                            text = "${viewModel.getString("switch_core")}: $activeCore",
                            color = CinemaGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                // Top right HUD Buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Aspect ratio changer
                    IconButton(
                        onClick = {
                            currentAspectIndex = (currentAspectIndex + 1) % aspects.size
                            toastMessage = "Scaling: ${aspects[currentAspectIndex]}"
                        },
                        modifier = Modifier.background(SoftGrey, RoundedCornerShape(12.dp))
                    ) {
                        Icon(imageVector = Icons.Default.AspectRatio, contentDescription = "Aspect ratio settings icon", tint = Color.White)
                    }

                    // Screen Lock toggle
                    IconButton(
                        onClick = {
                            isLocked = true
                            showOverlay = false
                            toastMessage = "Player Controls Locked"
                        },
                        modifier = Modifier
                            .background(SoftGrey, RoundedCornerShape(12.dp))
                            .testTag("lock_screen_toggle")
                    ) {
                        Icon(imageVector = Icons.Default.LockOpen, contentDescription = "Screen lock feature icon", tint = Color.White)
                    }

                    // EPG side panel toggle
                    IconButton(
                        onClick = { epgSidebarVisible = !epgSidebarVisible },
                        modifier = Modifier
                            .background(if (epgSidebarVisible) CinemaGold else SoftGrey, RoundedCornerShape(12.dp))
                            .testTag("epg_sidebar_toggle")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "EPG Mini Guide Timeline logo",
                            tint = if (epgSidebarVisible) MidnightNavy else Color.White
                        )
                    }
                }
            }
        }

        // TOAST PILL OVERLAY
        AnimatedVisibility(
            visible = toastMessage != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { 30 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { 30 }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 144.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CinemaGold.copy(alpha = 0.95f)),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.LiveTv, contentDescription = "Live feedback badge icon", tint = MidnightNavy, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = toastMessage ?: "",
                        color = MidnightNavy,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // BOTTOM HUD PANEL (MAIN INFO BAR CONTROLS)
        // Hidden when locked to guarantee continuous view
        AnimatedVisibility(
            visible = showOverlay && !isLocked,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.98f))
                        )
                    )
                    .padding(horizontal = 24.dp, vertical = 18.dp)
            ) {
                // INFO SCREEN PILLS PANEL (Dynamic Stream Health metrics)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // LIVE Status indicator with pulse blinker
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ActiveRed.copy(alpha = 0.15f))
                            .border(1.dp, ActiveRed.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(ActiveRed)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "LIVE",
                                color = ActiveRed,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Network speed pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SoftGrey)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Speed, contentDescription = "Speed rate icon", tint = ElectricCyan, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format("%.1f MB/s", simBitrate),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Buffer stats pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SoftGrey)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Buffer: ${String.format("%.2fs", simBufferTime)}",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Resolution frame limit
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SoftGrey)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "1080p @ ${simFps}fps",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Re-sync stream button
                    IconButton(
                        onClick = {
                            stream?.let {
                                toastMessage = "Refreshing stream feed..."
                                exoPlayer.stop()
                                val mediaItem = MediaItem.fromUri(it.streamUrl)
                                exoPlayer.setMediaItem(mediaItem)
                                exoPlayer.prepare()
                                exoPlayer.play()
                                isPlaying = true
                            }
                        },
                        modifier = Modifier
                            .background(SoftGrey, RoundedCornerShape(8.dp))
                            .size(28.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh connection sync key", tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }

                // EPISODE/SHOW REAL-TIME PROGRESS TRACKER BAR
                var simProgressVal by remember { mutableStateOf(0.40f) }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Current Show: ${if (epgPrograms.isNotEmpty()) epgPrograms.first().title else "Broadcasting Digital Channel FEED"}",
                            fontSize = 13.sp,
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        
                        Text(
                            text = "${(simProgressVal * 100).toInt()}% elapsed",
                            fontSize = 11.sp,
                            color = CinemaGold,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Slider representation of show progression
                    Slider(
                        value = simProgressVal,
                        onValueChange = { simProgressVal = it },
                        colors = SliderDefaults.colors(
                            activeTrackColor = CinemaGold,
                            inactiveTrackColor = SoftGrey,
                            thumbColor = CinemaGold
                        ),
                        modifier = Modifier.fillMaxWidth().height(18.dp)
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "08:30 AM", color = TextMuted, fontSize = 10.sp)
                        Text(text = "09:30 AM (Remaining: 36 mins)", color = TextMuted, fontSize = 10.sp)
                    }
                }

                // CORE DIRECTIVE CONTROLS
                when (activeCore) {
                    "VLC Core" -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFE65100).copy(alpha = 0.2f))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("VLC DEINTERLACING FILTER", color = Color(0xFFFFB74D), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (vlcDeinterlace) "ON (YADIF 2X)" else "OFF",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Switch(
                                    checked = vlcDeinterlace,
                                    onCheckedChange = { vlcDeinterlace = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFFFF9800),
                                        checkedTrackColor = Color(0xFFE65100)
                                    ),
                                    modifier = Modifier.scale(0.8f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    
                    "MPV Core" -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.DarkGray.copy(alpha = 0.6f))
                                    .padding(12.dp)
                            ) {
                                Text("MPV MULTI-SPEED", color = ElectricCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    mpvSpeeds.forEachIndexed { sIdx, speed ->
                                        val active = mpvSpeedIndex == sIdx
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (active) ElectricCyan else Color.Transparent)
                                                .clickable {
                                                    mpvSpeedIndex = sIdx
                                                    exoPlayer.setPlaybackSpeed(speed)
                                                }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "${speed}x",
                                                color = if (active) MidnightNavy else Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.DarkGray.copy(alpha = 0.6f))
                                    .padding(12.dp)
                            ) {
                                Text("SUBTITLE OFFSET SYNC", color = ElectricCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    IconButton(
                                        onClick = { mpvSubtitleDelay -= 100 },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.RemoveCircleOutline, contentDescription = "Slow subtitle", tint = Color.White)
                                    }
                                    
                                    Text("${mpvSubtitleDelay}ms", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    
                                    IconButton(
                                        onClick = { mpvSubtitleDelay += 100 },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.AddCircleOutline, contentDescription = "Fast subtitle", tint = Color.White)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // SELECTION CHIPS BAR (Tracks setup adjustments)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quality modifier Chip
                    OutlinedButton(
                        onClick = {
                            selectedQualityIndex = (selectedQualityIndex + 1) % qualityOptions.size
                            toastMessage = "Quality changed: ${qualityOptions[selectedQualityIndex]}"
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, CinemaGold.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Hd, contentDescription = "Quality HD", tint = CinemaGold, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = qualityOptions[selectedQualityIndex], fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Audio track modifier Chip
                    OutlinedButton(
                        onClick = {
                            selectedAudioIndex = (selectedAudioIndex + 1) % audioOptions.size
                            toastMessage = "Audio Track: ${audioOptions[selectedAudioIndex]}"
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, SoftGrey),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Audiotrack, contentDescription = "Audio Feed", tint = TextMuted, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = audioOptions[selectedAudioIndex], fontSize = 11.sp, color = TextWhite)
                    }

                    // Subtitles changer Chip
                    OutlinedButton(
                        onClick = {
                            selectedSubtitleIndex = (selectedSubtitleIndex + 1) % subtitleOptions.size
                            toastMessage = "Subtitles: ${subtitleOptions[selectedSubtitleIndex]}"
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, SoftGrey),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Subtitles, contentDescription = "Subtitles icon selector", tint = TextMuted, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = subtitleOptions[selectedSubtitleIndex], fontSize = 11.sp, color = TextWhite)
                    }
                }

                // PRIMARY NAVIGATION & PLAYBACK CONTROL BAR (Back, Play/Pause, Forward, Prev Channel, Next Channel)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous Channel Switcher ⏮️
                    IconButton(
                        onClick = {
                            // Find active channel index and load previous channel
                            val currentInList = streamsList.indexOfFirst { it.id == stream?.id }
                            if (currentInList != -1 && streamsList.size > 1) {
                                val prevIndex = if (currentInList == 0) streamsList.size - 1 else currentInList - 1
                                val targetCh = streamsList[prevIndex]
                                viewModel.selectStreamForPlayback(targetCh)
                            } else {
                                toastMessage = "No preceding channel found"
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .background(SoftGrey, RoundedCornerShape(22.dp))
                            .testTag("player_prev_channel_button")
                    ) {
                        Icon(imageVector = Icons.Default.SkipPrevious, contentDescription = "Previous IPTV Channel", tint = Color.White, modifier = Modifier.size(22.dp))
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Seek Backward 10 seconds button
                    IconButton(
                        onClick = { 
                            exoPlayer.seekTo(exoPlayer.currentPosition - 10000) 
                            toastMessage = "Rewind -10s"
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Replay10, contentDescription = "Rewind 10s", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(24.dp))
                    }
                    
                    Spacer(modifier = Modifier.width(20.dp))
                    
                    // Central play/pause button
                    IconButton(
                        onClick = {
                            if (isPlaying) {
                                exoPlayer.pause()
                            } else {
                                exoPlayer.play()
                            }
                            isPlaying = !isPlaying
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(CinemaGold)
                            .testTag("player_play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play status trigger",
                            tint = MidnightNavy,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    // Seek Forward 10 seconds button
                    IconButton(
                        onClick = { 
                            exoPlayer.seekTo(exoPlayer.currentPosition + 10000) 
                            toastMessage = "Forward +10s"
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Forward10, contentDescription = "Seek Forward 10s", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(24.dp))
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Next Channel Switcher ⏭️
                    IconButton(
                        onClick = {
                            // Find active channel index and load next channel
                            val currentInList = streamsList.indexOfFirst { it.id == stream?.id }
                            if (currentInList != -1 && streamsList.size > 1) {
                                val nextIndex = (currentInList + 1) % streamsList.size
                                val targetCh = streamsList[nextIndex]
                                viewModel.selectStreamForPlayback(targetCh)
                            } else {
                                toastMessage = "No preceding channel found"
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .background(SoftGrey, RoundedCornerShape(22.dp))
                            .testTag("player_next_channel_button")
                    ) {
                        Icon(imageVector = Icons.Default.SkipNext, contentDescription = "Next IPTV Channel", tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }

        // SIDEBAR ELECTRONIC PROGRAM GUIDE (EPG) CHANNELS INFO PANEL
        AnimatedVisibility(
            visible = epgSidebarVisible && !isLocked,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(280.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = 12.dp, horizontal = 8.dp)
                    .testTag("epg_guide_panel"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SteelSlate.copy(alpha = 0.95f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = viewModel.getString("epg_timeline"),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CinemaGold
                    )
                    
                    Text(
                        text = "Interactive Guide Timeline",
                        fontSize = 11.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    HorizontalDivider(color = SoftGrey)
                    Spacer(modifier = Modifier.height(10.dp))

                    if (epgPrograms.isEmpty()) {
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = viewModel.getString("no_epg"),
                                color = TextMuted,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(epgPrograms) { prog ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(SoftGrey.copy(alpha = 0.5f))
                                        .clickable {
                                            // Simulated past program CatchUp load!
                                            exoPlayer.seekTo(0)
                                            exoPlayer.play()
                                            toastMessage = "Playing Live CatchUp: ${prog.title}"
                                        }
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = prog.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextWhite,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = prog.description,
                                        fontSize = 10.sp,
                                        color = TextMuted,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                    
                                    Row(
                                        modifier = Modifier.padding(top = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(imageVector = Icons.Default.AccessTime, contentDescription = "Time duration clock icon", tint = ElectricCyan, modifier = Modifier.size(10.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Past Show • CatchUp Play",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ElectricCyan
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Side control slider custom helper card
@Composable
fun VerticalControlCard(
    title: String,
    level: Float,
    onLevelChange: (Float) -> Unit,
    activeColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    testTagPrefix: String
) {
    Card(
        modifier = Modifier
            .width(54.dp)
            .wrapContentHeight()
            .testTag("${testTagPrefix}_control_root"),
        colors = CardDefaults.cardColors(containerColor = SoftGrey.copy(alpha = 0.85f)),
        shape = RoundedCornerShape(27.dp),
        border = BorderStroke(1.dp, activeColor.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Level UP (+) Button
            IconButton(
                onClick = { onLevelChange((level + 0.05f).coerceAtMost(1f)) },
                modifier = Modifier
                    .size(36.dp)
                    .background(SoftGrey, RoundedCornerShape(18.dp))
                    .testTag("${testTagPrefix}_up_btn")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Increase level", tint = activeColor, modifier = Modifier.size(16.dp))
            }

            // Status Indicator & Icon
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = activeColor,
                    modifier = Modifier.size(18.dp)
                )
                
                Spacer(modifier = Modifier.height(3.dp))
                
                // Audio level / Brightness percentage label
                Text(
                    text = "${(level * 100).toInt()}%",
                    color = TextWhite,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            // Level DOWN (-) Button
            IconButton(
                onClick = { onLevelChange((level - 0.05f).coerceAtLeast(0f)) },
                modifier = Modifier
                    .size(36.dp)
                    .background(SoftGrey, RoundedCornerShape(18.dp))
                    .testTag("${testTagPrefix}_down_btn")
            ) {
                Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease level", tint = activeColor, modifier = Modifier.size(16.dp))
            }
        }
    }
}

// Extension to scale float for switches
private fun Modifier.scale(scale: Float): Modifier = this
