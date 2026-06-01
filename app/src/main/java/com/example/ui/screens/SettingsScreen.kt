package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.IptvViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(viewModel: IptvViewModel) {
    val languages = listOf(
        Pair("en", "English"),
        Pair("es", "Español"),
        Pair("fr", "Français"),
        Pair("ar", "العربية"),
        Pair("hi", "हिन्दी")
    )
    val playerCores = listOf("ExoPlayer", "VLC Core", "MPV Core")
    
    val currentLang by viewModel.currentLanguage.collectAsState()
    val currentCore by viewModel.selectedPlayerCore.collectAsState()
    val playlists by viewModel.playlists.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(MidnightNavy, SteelSlate, MidnightNavy)
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .padding(24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.navigateTo("dashboard") },
                    modifier = Modifier.testTag("settings_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = CinemaGold
                    )
                }
                Text(
                    text = viewModel.getString("settings"),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextWhite,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // UNIT: LANGUAGE TOGGLE CARDS
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = SoftGrey.copy(alpha = 0.85f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Translate, contentDescription = "Language", tint = CinemaGold)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = viewModel.getString("change_lang"),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                languages.forEach { lang ->
                                    val active = currentLang == lang.first
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (active) CinemaGold else MidnightNavy)
                                            .clickable { viewModel.setLanguage(lang.first) }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                            .testTag("lang_btn_${lang.first}")
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
                        }
                    }
                }

                // UNIT: DEF AUDIO PLAYER DIRECTIVE OVERRIDE
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = SoftGrey.copy(alpha = 0.85f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.SpeakerGroup, contentDescription = "Engine Core", tint = CinemaGold)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = viewModel.getString("player_core"),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                            }
                            
                            Text(
                                text = "Switch player decoding core overlays for custom HLS speeds or deinterlace EQ controls.",
                                fontSize = 11.sp,
                                color = TextMuted,
                                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                playerCores.forEach { core ->
                                    val active = currentCore == core
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (active) CinemaGold else MidnightNavy)
                                            .clickable { viewModel.setPlayerCore(core) }
                                            .padding(vertical = 10.dp)
                                            .testTag("core_p_btn_${core.replace(" ", "_")}"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = core,
                                            color = if (active) MidnightNavy else TextWhite,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // UNIT: PORTALS PLAYLIST MANAGER
                item {
                    Text(
                        text = "Connected IPTV Portals Directory",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextWhite,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                
                if (playlists.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SoftGrey.copy(alpha = 0.4f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(viewModel.getString("no_playlists"), color = TextMuted, fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    items(playlists) { playlist ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("playlist_item_${playlist.id}"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SoftGrey.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MidnightNavy),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (playlist.type == "XTREAM") Icons.Default.CloudQueue else Icons.Default.FormatListBulleted,
                                            contentDescription = "Playlist format",
                                            tint = CinemaGold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(
                                            text = playlist.name,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextWhite,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (playlist.type == "XTREAM") "Type: Xtream Codes Server" else "Type: Local/Remote M3U",
                                            fontSize = 11.sp,
                                            color = TextMuted
                                        )
                                    }
                                }
                                
                                IconButton(
                                    onClick = { viewModel.removePlaylist(playlist) },
                                    modifier = Modifier.testTag("delete_playlist_${playlist.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Delete Playlist connection",
                                        tint = ActiveRed
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
