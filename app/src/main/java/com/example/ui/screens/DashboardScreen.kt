package com.example.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.IptvViewModel
import com.example.ui.theme.*

@Composable
fun DashboardScreen(viewModel: IptvViewModel) {
    val activePlaylist by viewModel.activePlaylist.collectAsState()
    
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isTablet = configuration.screenWidthDp >= 600
    val isWideScreen = isLandscape || isTablet

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(MidnightNavy, SteelSlate, MidnightNavy)
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        if (isWideScreen) {
            // Adaptive Side-by-Side row layout for horizontal, TV, and Tablet screens
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Left Panel: Brand info, Current playlist state and critical navigations
                Column(
                    modifier = Modifier
                        .weight(0.4f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = viewModel.getString("app_title"),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = CinemaGold,
                            modifier = Modifier.testTag("dashboard_header")
                        )
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Text(
                            text = "${viewModel.getString("active_playlist")}: ${activePlaylist?.name ?: "Unknown"}",
                            fontSize = 12.sp,
                            color = TextMuted,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Switch Playlist / Re-authenticator Button
                        Button(
                            onClick = { viewModel.navigateTo("login") },
                            colors = ButtonDefaults.buttonColors(containerColor = SoftGrey),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("switch_provider_button"),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwitchAccount,
                                contentDescription = "Switch Playlist Setup",
                                tint = CinemaGold,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Switch Playlist / Account",
                                color = TextWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Secondary Setting nodes
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Parental locks configuration
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .clickable { viewModel.navigateTo("parental_controls") }
                                .testTag("dashboard_parental_btn"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SoftGrey.copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ActiveRed.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.Pin, contentDescription = "Parental", tint = ActiveRed)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = viewModel.getString("parental"),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // App Settings configurer
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .clickable { viewModel.navigateTo("settings") }
                                .testTag("dashboard_settings_btn"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SoftGrey.copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(CinemaGold.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings", tint = CinemaGold)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = viewModel.getString("settings"),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Right Panel: 2x2 grid containing IPTV directory nodes with adapted layout sizing
                Column(
                    modifier = Modifier.weight(0.6f)
                ) {
                    Text(
                        text = "Media Library Console",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            DashboardNode(
                                title = viewModel.getString("live_tv"),
                                icon = Icons.Default.LiveTv,
                                gradient = Brush.linearGradient(listOf(Color(0xFFE53935), Color(0xFFFF5252))),
                                onClick = {
                                    viewModel.selectType("live")
                                    viewModel.navigateTo("list_viewer")
                                },
                                tag = "grid_live_tv",
                                heightDp = 100
                            )
                        }
                        item {
                            DashboardNode(
                                title = viewModel.getString("movies"),
                                icon = Icons.Default.MovieFilter,
                                gradient = Brush.linearGradient(listOf(Color(0xFF3949AB), Color(0xFF5C6BC0))),
                                onClick = {
                                    viewModel.selectType("movie")
                                    viewModel.navigateTo("list_viewer")
                                },
                                tag = "grid_movies",
                                heightDp = 100
                            )
                        }
                        item {
                            DashboardNode(
                                title = viewModel.getString("series"),
                                icon = Icons.Default.VideoLibrary,
                                gradient = Brush.linearGradient(listOf(Color(0xFF00897B), Color(0xFF26A69A))),
                                onClick = {
                                    viewModel.selectType("series")
                                    viewModel.navigateTo("list_viewer")
                                },
                                tag = "grid_series",
                                heightDp = 100
                            )
                        }
                        item {
                            DashboardNode(
                                title = viewModel.getString("favorites"),
                                icon = Icons.Default.Favorite,
                                gradient = Brush.linearGradient(listOf(Color(0xFFD81B60), Color(0xFFEC407A))),
                                onClick = {
                                    viewModel.selectType("favorites")
                                    viewModel.navigateTo("list_viewer")
                                },
                                tag = "grid_favorites",
                                heightDp = 100
                            )
                        }
                    }
                }
            }
        } else {
            // Traditional Portrait stack layout for standard phones
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Upper App Header Pane
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = viewModel.getString("app_title"),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = CinemaGold,
                            modifier = Modifier.testTag("dashboard_header")
                        )
                        
                        Text(
                            text = "${viewModel.getString("active_playlist")}: ${activePlaylist?.name ?: "Unknown"}",
                            fontSize = 12.sp,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Switch account button
                    IconButton(
                        onClick = { viewModel.navigateTo("login") },
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(SoftGrey)
                            .size(44.dp)
                            .testTag("switch_provider_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwitchAccount,
                            contentDescription = "Switch Playlist Setup",
                            tint = CinemaGold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Main Console Dashboard Nodes Grid
                Text(
                    text = "Media Library Console",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        DashboardNode(
                            title = viewModel.getString("live_tv"),
                            icon = Icons.Default.LiveTv,
                            gradient = Brush.linearGradient(listOf(Color(0xFFE53935), Color(0xFFFF5252))),
                            onClick = {
                                viewModel.selectType("live")
                                viewModel.navigateTo("list_viewer")
                            },
                            tag = "grid_live_tv"
                        )
                    }
                    item {
                        DashboardNode(
                            title = viewModel.getString("movies"),
                            icon = Icons.Default.MovieFilter,
                            gradient = Brush.linearGradient(listOf(Color(0xFF3949AB), Color(0xFF5C6BC0))),
                            onClick = {
                                viewModel.selectType("movie")
                                viewModel.navigateTo("list_viewer")
                            },
                                tag = "grid_movies"
                        )
                    }
                    item {
                        DashboardNode(
                            title = viewModel.getString("series"),
                            icon = Icons.Default.VideoLibrary,
                            gradient = Brush.linearGradient(listOf(Color(0xFF00897B), Color(0xFF26A69A))),
                            onClick = {
                                viewModel.selectType("series")
                                viewModel.navigateTo("list_viewer")
                            },
                            tag = "grid_series"
                        )
                    }
                    item {
                        DashboardNode(
                            title = viewModel.getString("favorites"),
                            icon = Icons.Default.Favorite,
                            gradient = Brush.linearGradient(listOf(Color(0xFFD81B60), Color(0xFFEC407A))),
                            onClick = {
                                viewModel.selectType("favorites")
                                viewModel.navigateTo("list_viewer")
                            },
                            tag = "grid_favorites"
                        )
                    }
                }

                // Bottom Settings Toolbar Grid
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = SoftGrey)
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Parental Console
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(76.dp)
                            .clickable { viewModel.navigateTo("parental_controls") }
                            .testTag("dashboard_parental_btn"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SoftGrey.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ActiveRed.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.Pin, contentDescription = "Parental", tint = ActiveRed)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = viewModel.getString("parental"),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Settings Console
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(76.dp)
                            .clickable { viewModel.navigateTo("settings") }
                            .testTag("dashboard_settings_btn"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SoftGrey.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CinemaGold.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings", tint = CinemaGold)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = viewModel.getString("settings"),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun DashboardNode(
    title: String,
    icon: ImageVector,
    gradient: Brush,
    onClick: () -> Unit,
    tag: String,
    heightDp: Int = 140
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(heightDp.dp)
            .clickable { onClick() }
            .testTag(tag),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SoftGrey.copy(alpha = 0.9f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (heightDp < 120) 12.dp else 20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(if (heightDp < 120) 36.dp else 48.dp)
                    .clip(RoundedCornerShape(if (heightDp < 120) 10.dp else 14.dp))
                    .background(gradient),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = TextWhite,
                    modifier = Modifier.size(if (heightDp < 120) 20.dp else 26.dp)
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = if (heightDp < 120) 14.sp else 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextWhite
                )
                
                Icon(
                    imageVector = Icons.Default.PlayCircleFilled,
                    contentDescription = "Open",
                    tint = TextWhite.copy(alpha = 0.6f),
                    modifier = Modifier.size(if (heightDp < 120) 18.dp else 24.dp)
                )
            }
        }
    }
}
