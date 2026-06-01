package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.StreamItem
import com.example.ui.IptvViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ListViewerScreen(viewModel: IptvViewModel) {
    val activeType by viewModel.activeType.collectAsState()
    val activeCategory by viewModel.activeCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val streams by viewModel.streamsList.collectAsState()
    val categories by viewModel.categoriesList.collectAsState()
    val parentalConfig by viewModel.parentalConfig.collectAsState()
    
    // Parental PIN Entry modal states
    var pinDialogVisible by remember { mutableStateOf(false) }
    var streamPendingLock by remember { mutableStateOf<StreamItem?>(null) }
    var pinValue by remember { mutableStateOf("") }
    var pinErrorMessage by remember { mutableStateOf<String?>(null) }

    val pageTitle = when (activeType) {
        "live" -> viewModel.getString("live_tv")
        "movie" -> viewModel.getString("movies")
        "series" -> viewModel.getString("series")
        else -> viewModel.getString("favorites")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightNavy)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Screen Header containing Search
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.navigateTo("dashboard") },
                    modifier = Modifier.testTag("back_button_channel_list")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = CinemaGold
                    )
                }
                
                Text(
                    text = pageTitle,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextWhite,
                    modifier = Modifier.weight(1f)
                )
            }

            // Real-time Text Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchQuery.value = it },
                placeholder = { Text(viewModel.getString("search_hint"), color = TextMuted) },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = TextMuted) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CinemaGold,
                    unfocusedBorderColor = SoftGrey,
                    focusedContainerColor = SoftGrey.copy(alpha = 0.3f),
                    unfocusedContainerColor = SoftGrey.copy(alpha = 0.3f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("stream_search_input")
            )

            // Dynamic Category Filter Strip (Horizontal row of pills)
            if (activeType != "favorites" && categories.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { category ->
                        val isSelected = (category == "All" && activeCategory == null) || (category == activeCategory)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) CinemaGold else SoftGrey)
                                .clickable { viewModel.selectCategory(category) }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .testTag("cat_pill_$category")
                        ) {
                            Text(
                                text = category,
                                color = if (isSelected) MidnightNavy else TextWhite,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Stream List View Container (Grid for Movie/Series, Column for Channels)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                if (streams.isEmpty()) {
                    // Modern Empty State Illustration
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (activeType == "favorites") Icons.Default.FavoriteBorder else Icons.Default.Inbox,
                            contentDescription = "Empty Logo",
                            tint = TextMuted.copy(alpha = 0.5f),
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (activeType == "favorites") viewModel.getString("favorites_empty") else "No streams found in this section.",
                            fontSize = 15.sp,
                            color = TextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                } else if (activeType == "live" || activeType == "favorites") {
                    // Column list representing Live Channels
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 850.dp),
                        contentPadding = PaddingValues(bottom = 16.dp, start = 16.dp, end = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(streams) { stream ->
                            LiveChannelItem(
                                stream = stream,
                                onFavClick = { viewModel.toggleFavorite(stream) },
                                onLockClick = { viewModel.toggleStreamLock(stream) },
                                onChannelClick = {
                                    // Assess parental controls lock
                                    if (stream.isLocked && parentalConfig?.isLockActive == true) {
                                        streamPendingLock = stream
                                        pinValue = ""
                                        pinErrorMessage = null
                                        pinDialogVisible = true
                                    } else {
                                        viewModel.selectStreamForPlayback(stream)
                                    }
                                }
                            )
                        }
                    }
                } else {
                    // Grid mapping for Movies & Series posters - Adaptive cells count for TV, Tablet and Landscape configurations
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 145.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 1100.dp),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(streams) { stream ->
                            VodMediaItem(
                                stream = stream,
                                onFavClick = { viewModel.toggleFavorite(stream) },
                                onMediaClick = {
                                    if (stream.isLocked && parentalConfig?.isLockActive == true) {
                                        streamPendingLock = stream
                                        pinValue = ""
                                        pinErrorMessage = null
                                        pinDialogVisible = true
                                    } else {
                                        viewModel.selectStreamForPlayback(stream)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
        
        // Parental Pass PIN input dialog modal
        if (pinDialogVisible) {
            AlertDialog(
                onDismissRequest = { pinDialogVisible = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (pinValue == (parentalConfig?.pin ?: "0000")) {
                                pinDialogVisible = false
                                streamPendingLock?.let { viewModel.selectStreamForPlayback(it) }
                            } else {
                                pinErrorMessage = viewModel.getString("pin_incorrect")
                            }
                        },
                        modifier = Modifier.testTag("pin_dialog_confirm")
                    ) {
                        Text(viewModel.getString("unlock"), color = CinemaGold, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pinDialogVisible = false }) {
                        Text("Cancel", color = TextMuted)
                    }
                },
                title = { Text(viewModel.getString("enter_pin"), fontSize = 18.sp, color = TextWhite) },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "This channel or category is parentally locked. Introduce security code to stream.",
                            fontSize = 13.sp,
                            color = TextMuted,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        OutlinedTextField(
                            value = pinValue,
                            onValueChange = { if (it.length <= 4) pinValue = it },
                            placeholder = { Text("xxxx", color = TextMuted.copy(alpha = 0.5f)) },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CinemaGold,
                                unfocusedBorderColor = SoftGrey
                            ),
                            modifier = Modifier
                                .width(120.dp)
                                .testTag("pin_dialog_text")
                        )
                        
                        if (pinErrorMessage != null) {
                            Text(
                                text = pinErrorMessage ?: "",
                                color = ActiveRed,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                },
                containerColor = SteelSlate,
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Composable
fun LiveChannelItem(
    stream: StreamItem,
    onFavClick: () -> Unit,
    onLockClick: () -> Unit,
    onChannelClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChannelClick() }
            .testTag("channel_card_${stream.streamId}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SoftGrey.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Channels logo icon/symbol
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MidnightNavy),
                contentAlignment = Alignment.Center
            ) {
                if (stream.iconUrl.isNotEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(stream.iconUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Channel Logo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(40.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = "No Logo",
                        tint = CinemaGold.copy(alpha = 0.7f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Channels Metadata
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stream.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (stream.isLocked) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = ActiveRed,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Text(
                    text = stream.categoryName,
                    fontSize = 11.sp,
                    color = ElectricCyan,
                    modifier = Modifier.padding(top = 2.dp)
                )

                // Simulated live EPG subtopic descriptor ticker
                val guideLabel = when (stream.streamId) {
                    "demo_live_1" -> "Space Station Live Mission Status • 10:00 AM - 12:00 PM"
                    "demo_live_2" -> "Global Political Debates & Headlines • 11:00 AM - 11:45 AM"
                    "demo_live_3" -> "Geopolitical Investigative Feature Documentary • 11:30 AM - 1:00 PM"
                    "demo_live_4" -> "European Culture, Fashion & Global Review • 10:30 AM - 11:30 AM"
                    else -> "Broadcasting Stream Guide Loaded. Click to tune in."
                }
                
                Text(
                    text = guideLabel,
                    fontSize = 11.sp,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action: Lock PIN Switch
            IconButton(
                onClick = onLockClick,
                modifier = Modifier.testTag("lock_channel_${stream.streamId}")
            ) {
                Icon(
                    imageVector = if (stream.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = "Toggle Parental Security",
                    tint = if (stream.isLocked) ActiveRed else TextMuted.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Action: Favorites switch
            IconButton(
                onClick = onFavClick,
                modifier = Modifier.testTag("fav_channel_${stream.streamId}")
            ) {
                Icon(
                    imageVector = if (stream.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Favorite Channel Status",
                    tint = if (stream.isFavorite) CinemaGold else TextMuted.copy(alpha = 0.6f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun VodMediaItem(
    stream: StreamItem,
    onFavClick: () -> Unit,
    onMediaClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .clickable { onMediaClick() }
            .testTag("vod_item_${stream.streamId}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SoftGrey.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Posters Canvas
            if (stream.iconUrl.isNotEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(stream.iconUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = stream.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SoftGrey),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = "Movie placeholder",
                        tint = TextMuted.copy(alpha = 0.4f),
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            // High contrast shadow gradient covering texts
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.95f))
                        )
                    )
            )

            // Left aligned Parental and Favorite top elements
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (stream.isLocked) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ActiveRed)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("18+", color = TextWhite, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Spacer(modifier = Modifier.width(4.dp))
                }

                IconButton(
                    onClick = onFavClick,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(17.dp))
                        .background(MidnightNavy.copy(alpha = 0.7f))
                        .testTag("vod_fav_${stream.streamId}")
                ) {
                    Icon(
                        imageVector = if (stream.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite vod icon",
                        tint = if (stream.isFavorite) ActiveRed else TextWhite,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Lower Info Panel overlay text
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Text(
                    text = stream.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = stream.categoryName,
                    fontSize = 10.sp,
                    color = ElectricCyan,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
