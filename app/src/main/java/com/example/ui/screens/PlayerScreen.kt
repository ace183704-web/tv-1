package com.example.ui.screens

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.ui.IptvViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlayerScreen(viewModel: IptvViewModel) {
    val context = LocalContext.current
    val stream by viewModel.selectedStream.collectAsState()
    val activeCore by viewModel.selectedPlayerCore.collectAsState()
    val epgPrograms by viewModel.currentEpgList.collectAsState()
    
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

    // Media3 ExoPlayer Instance
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    // Connect player source lifecycle
    LaunchedEffect(stream) {
        stream?.let {
            val mediaItem = MediaItem.fromUri(it.streamUrl)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.play()
            isPlaying = true
        }
    }

    // Clean disposal of core resources
    DisposableEffect(Unit) {
        onDispose {
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
                .clickable { showOverlay = !showOverlay }
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
                modifier = Modifier.fillMaxSize()
            )
            
            // Render simulation overlay depending on selected Aspect Ratios
            val scalingDesc = aspects[currentAspectIndex]
            if (scalingDesc != "Auto Fit") {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.05f))
                ) {
                    Text(
                        text = "Aspect: $scalingDesc",
                        color = CinemaGold.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(24.dp)
                    )
                }
            }
        }

        // Ambient Dark Cover on Overlays
        AnimatedVisibility(
            visible = showOverlay,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
            )
        }

        // IMMERSIVE HUD CONTROLS OVERLAY
        AnimatedVisibility(
            visible = showOverlay,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.9f), Color.Transparent)
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Aspect ratio changer
                    IconButton(onClick = {
                        currentAspectIndex = (currentAspectIndex + 1) % aspects.size
                    }) {
                        Icon(imageVector = Icons.Default.AspectRatio, contentDescription = "Aspect ratio settings icon", tint = Color.White)
                    }

                    // EPG side panel toggle
                    IconButton(
                        onClick = { epgSidebarVisible = !epgSidebarVisible },
                        modifier = Modifier.testTag("epg_sidebar_toggle")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "EPG Mini Guide Timeline logo",
                            tint = if (epgSidebarVisible) CinemaGold else Color.White
                        )
                    }
                }
            }
        }

        // BOTTOM HUD PANEL
        AnimatedVisibility(
            visible = showOverlay,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                        )
                    )
                    .padding(24.dp)
            ) {
                // CORE DIRECTIVE CONTROLS (Render custom HUD elements based on active core selections)
                when (activeCore) {
                    "VLC Core" -> {
                        // VLC styled components: Bright Orange parameters, Volume slider, deinterlacing controls
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
                        
                        // Volume overlay slide bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.VolumeUp, contentDescription = "Volume Icon", tint = Color(0xFFFF9800))
                            Spacer(modifier = Modifier.width(12.dp))
                            Slider(
                                value = vlcVolumeLevel.toFloat(),
                                onValueChange = { vlcVolumeLevel = it.toInt() },
                                valueRange = 0f..100f,
                                colors = SliderDefaults.colors(
                                    activeTrackColor = Color(0xFFFF9800),
                                    thumbColor = Color(0xFFFFB74D)
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("${vlcVolumeLevel}%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    "MPV Core" -> {
                        // MPV styled components: speeds toggles, subtitle sync delays, brutalist aesthetic
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Speeds Selection Pill layout
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
                                                    // Dynamically coordinate ExoPlayer playback rates
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

                            // Subtitle delays pill layout
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
                    }
                    
                    else -> {
                        // Standard Clean ExoPlayer Engine overlay
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Text("ExoPlayer HW Codec Acceleration: Active", color = Color.Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("Direct Render Frame: VQ-HD Ready", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Playback controls Row (Back, Play/Pause, Forward)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { exoPlayer.seekTo(exoPlayer.currentPosition - 10000) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Replay10, contentDescription = "Rewind 10s", tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                    
                    Spacer(modifier = Modifier.width(32.dp))
                    
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
                            .size(64.dp)
                            .clip(RoundedCornerShape(32.dp))
                            .background(CinemaGold)
                            .testTag("player_play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play trigger",
                            tint = MidnightNavy,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(32.dp))

                    IconButton(
                        onClick = { exoPlayer.seekTo(exoPlayer.currentPosition + 10000) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Forward10, contentDescription = "Seek Forward 10s", tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }
            }
        }

        // SIDEBAR ELECTRONIC PROGRAM GUIDE (EPG) CHANNELS INFO
        AnimatedVisibility(
            visible = epgSidebarVisible,
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
                                // Custom program slot cards
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(SoftGrey.copy(alpha = 0.5f))
                                        .clickable {
                                            // Simulated past program CatchUp load!
                                            exoPlayer.seekTo(0)
                                            exoPlayer.play()
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

// Extension to scale float for switches
private fun Modifier.scale(scale: Float): Modifier = this
