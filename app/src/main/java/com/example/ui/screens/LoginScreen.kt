package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.IptvViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LoginScreen(viewModel: IptvViewModel) {
    var activeTab by remember { mutableStateOf(0) } // 0 = Xtream, 1 = M3U
    
    // Xtream states
    var xtreamName by remember { mutableStateOf("") }
    var xtreamUrl by remember { mutableStateOf("") }
    var xtreamUser by remember { mutableStateOf("") }
    var xtreamPass by remember { mutableStateOf("") }
    
    // M3U states
    var m3uName by remember { mutableStateOf("") }
    var m3uUrl by remember { mutableStateOf("") }
    
    var passwordVisible by remember { mutableStateOf(false) }
    
    val isAuthenticating by viewModel.isAuthenticating.collectAsState()
    val errorMsg by viewModel.addPlaylistError.collectAsState()
    
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // Header / Brand Logo
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(CinemaGold.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Tv,
                    contentDescription = "Logo Icon",
                    tint = CinemaGold,
                    modifier = Modifier.size(44.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = viewModel.getString("app_title"),
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = CinemaGold,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("onboarding_title")
            )
            
            Text(
                text = "Professional IPTV Middleware Player",
                fontSize = 14.sp,
                color = TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)
            )
            
            // Glassmorphic Input Terminal Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SoftGrey.copy(alpha = 0.85f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Tab Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MidnightNavy.copy(alpha = 0.5f))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = { activeTab = 0 },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (activeTab == 0) CinemaGold else Color.Transparent,
                                contentColor = if (activeTab == 0) MidnightNavy else TextWhite
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("tab_xtream")
                        ) {
                            Text(viewModel.getString("xtream_tab"), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        
                        Button(
                            onClick = { activeTab = 1 },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (activeTab == 1) CinemaGold else Color.Transparent,
                                contentColor = if (activeTab == 1) MidnightNavy else TextWhite
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("tab_m3u")
                        ) {
                            Text(viewModel.getString("m3u_tab"), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Form fields
                    if (activeTab == 0) {
                        // Xtream Codes Terminal
                        OutlinedTextField(
                            value = xtreamName,
                            onValueChange = { xtreamName = it },
                            label = { Text(viewModel.getString("playlist_name"), color = TextMuted) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CinemaGold,
                                unfocusedBorderColor = SoftGrey,
                                focusedLabelColor = CinemaGold
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("xtream_name_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        OutlinedTextField(
                            value = xtreamUrl,
                            onValueChange = { xtreamUrl = it },
                            label = { Text(viewModel.getString("server_url"), color = TextMuted) },
                            placeholder = { Text("http://domain:8080", color = TextMuted.copy(alpha = 0.5f)) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CinemaGold,
                                unfocusedBorderColor = SoftGrey,
                                focusedLabelColor = CinemaGold
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("xtream_url_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        OutlinedTextField(
                            value = xtreamUser,
                            onValueChange = { xtreamUser = it },
                            label = { Text(viewModel.getString("username"), color = TextMuted) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CinemaGold,
                                unfocusedBorderColor = SoftGrey,
                                focusedLabelColor = CinemaGold
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("xtream_user_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        OutlinedTextField(
                            value = xtreamPass,
                            onValueChange = { xtreamPass = it },
                            label = { Text(viewModel.getString("password"), color = TextMuted) },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(imageVector = image, contentDescription = "Toggle Pass Visibility", tint = TextMuted)
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CinemaGold,
                                unfocusedBorderColor = SoftGrey,
                                focusedLabelColor = CinemaGold
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("xtream_pass_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    } else {
                        // M3U Playlist Terminal
                        OutlinedTextField(
                            value = m3uName,
                            onValueChange = { m3uName = it },
                            label = { Text(viewModel.getString("playlist_name"), color = TextMuted) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CinemaGold,
                                unfocusedBorderColor = SoftGrey,
                                focusedLabelColor = CinemaGold
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("m3u_name_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        OutlinedTextField(
                            value = m3uUrl,
                            onValueChange = { m3uUrl = it },
                            label = { Text(viewModel.getString("m3u_url"), color = TextMuted) },
                            placeholder = { Text("https://example.com/live.m3u", color = TextMuted.copy(alpha = 0.5f)) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CinemaGold,
                                unfocusedBorderColor = SoftGrey,
                                focusedLabelColor = CinemaGold
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("m3u_url_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    
                    // Alert Message Displays
                    if (errorMsg != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = errorMsg ?: "",
                            color = ActiveRed,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("error_message")
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Execute Connection Button
                    Button(
                        onClick = {
                            if (activeTab == 0) {
                                viewModel.addXtreamCodesPlaylist(xtreamName, xtreamUrl, xtreamUser, xtreamPass)
                            } else {
                                viewModel.addM3uPlaylist(m3uName, m3uUrl)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CinemaGold,
                            contentColor = MidnightNavy
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("connect_button"),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isAuthenticating
                    ) {
                        if (isAuthenticating) {
                            CircularProgressIndicator(color = MidnightNavy, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(viewModel.getString("authenticating"), fontWeight = FontWeight.Bold)
                        } else {
                            Icon(imageVector = Icons.Default.CloudSync, contentDescription = "Sync Connect")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(viewModel.getString("connect"), fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Demo Provisioner Callout
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { if (!isAuthenticating) viewModel.loadDemoProvider() }
                    .testTag("demo_provider_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SteelSlate),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ElectricCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = "Demo Play logo",
                            tint = ElectricCyan,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = viewModel.getString("load_demo"),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricCyan
                        )
                        Text(
                            text = viewModel.getString("demo_desc"),
                            fontSize = 11.sp,
                            color = TextMuted,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    
                    Icon(
                        imageVector = Icons.Default.ArrowForwardIos,
                        contentDescription = "Go Demo arrow",
                        tint = ElectricCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
