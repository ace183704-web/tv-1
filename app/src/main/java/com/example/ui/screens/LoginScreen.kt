package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
    val firebaseUser by viewModel.firebaseUser.collectAsState()
    val isAuthenticating by viewModel.isAuthenticating.collectAsState()
    val firebaseAuthError by viewModel.firebaseAuthError.collectAsState()
    val provisionedPortals by viewModel.provisionedPortals.collectAsState()
    val syncStatusMessage by viewModel.firebaseSyncStatus.collectAsState()
    val errorMsg by viewModel.addPlaylistError.collectAsState()

    var showLocalForms by remember { mutableStateOf(false) }
    var adminClickCount by remember { mutableStateOf(0) }
    var showAdminConsoleByUser by remember { mutableStateOf(false) }

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
        if (firebaseUser == null && !showLocalForms) {
            // ----------------------------------------------------
            // 1. FIREBASE AUTH CLOUD GATEWAY SCREEN (SKY GLASS THEME)
            // ----------------------------------------------------
            var isSignUpMode by remember { mutableStateOf(false) }
            var email by remember { mutableStateOf("") }
            var password by remember { mutableStateOf("") }
            var passVisibility by remember { mutableStateOf(false) }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 650.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                
                // Glowing Sky Glass Logo Header
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(CinemaGold.copy(alpha = 0.15f))
                        .border(1.5.dp, CinemaGold.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                        .clickable {
                            adminClickCount++
                            if (adminClickCount >= 5) {
                                showAdminConsoleByUser = true
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudQueue,
                        contentDescription = "Cloud Icon",
                        tint = CinemaGold,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isSignUpMode) "Create Sync Account" else "Cloud Sync Portal",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = CinemaGold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .testTag("onboarding_title")
                        .clickable {
                            adminClickCount++
                            if (adminClickCount >= 5) {
                                showAdminConsoleByUser = true
                            }
                        }
                )

                Text(
                    text = viewModel.getString("cloud_sync_desc"),
                    fontSize = 13.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp, bottom = 28.dp)
                )

                if (showAdminConsoleByUser) {
                    AdminProvisioningConsole(
                        viewModel = viewModel,
                        onClose = { showAdminConsoleByUser = false }
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Glassmorphic Auth Input Card
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
                        // Email input
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Address", color = TextMuted) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email Input icon", tint = TextMuted) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CinemaGold,
                                unfocusedBorderColor = SoftGrey,
                                focusedLabelColor = CinemaGold
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("firebase_email_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Password input
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password", color = TextMuted) },
                            singleLine = true,
                            visualTransformation = if (passVisibility) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password Input icon", tint = TextMuted) },
                            trailingIcon = {
                                val visIcon = if (passVisibility) Icons.Default.Visibility else Icons.Default.VisibilityOff
                                IconButton(onClick = { passVisibility = !passVisibility }) {
                                    Icon(visIcon, contentDescription = "Visibility Toggle", tint = TextMuted)
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CinemaGold,
                                unfocusedBorderColor = SoftGrey,
                                focusedLabelColor = CinemaGold
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("firebase_password_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        if (firebaseAuthError != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = firebaseAuthError ?: "",
                                color = ActiveRed,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Trigger Login Action
                        Button(
                            onClick = {
                                if (isSignUpMode) {
                                    viewModel.registerWithFirebase(email, password)
                                } else {
                                    viewModel.loginWithFirebase(email, password)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CinemaGold,
                                contentColor = MidnightNavy
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag(if (isSignUpMode) "firebase_signup_button" else "firebase_signin_button"),
                            enabled = !isAuthenticating
                        ) {
                            if (isAuthenticating) {
                                CircularProgressIndicator(color = MidnightNavy, modifier = Modifier.size(24.dp))
                            } else {
                                Text(
                                    text = if (isSignUpMode) "REGISTER & SYNC" else "SIGN IN & SYNC",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Toggle Register vs Sign in mode
                        Text(
                            text = if (isSignUpMode) "Already have a cloud account? Sign In" else "New to Cloud sync? Create Account",
                            color = ElectricCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .clickable { isSignUpMode = !isSignUpMode }
                                .padding(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Social authentications header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f).height(1.dp).background(SoftGrey))
                    Text(
                        text = "OR DIGITAL SINGLE SIGN-ON",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    Box(modifier = Modifier.weight(1f).height(1.dp).background(SoftGrey))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Single Sign-on Grid (Google & Facebook)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { viewModel.loginWithSocial("google") },
                        colors = ButtonDefaults.buttonColors(containerColor = SteelSlate),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("google_login_button"),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isAuthenticating
                    ) {
                        Icon(imageVector = Icons.Default.AccountCircle, contentDescription = "Google Icon", tint = TextWhite, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Google", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Button(
                        onClick = { viewModel.loginWithSocial("facebook") },
                        colors = ButtonDefaults.buttonColors(containerColor = SteelSlate),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("facebook_login_button"),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isAuthenticating
                    ) {
                        Icon(imageVector = Icons.Default.Facebook, contentDescription = "Facebook Icon", tint = TextWhite, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Facebook", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                DeviceProvisioningCodeCard(code = viewModel.deviceProvisioningCode)

                Spacer(modifier = Modifier.height(24.dp))

                // Alternate direct local playlist loader
                Text(
                    text = "Skip to Manual IPTV Entry",
                    color = TextMuted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showLocalForms = true }
                        .border(1.dp, SoftGrey, RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .testTag("skip_to_local_button")
                )
            }
        } 
        
        else if (firebaseUser == null && showLocalForms) {
            // ----------------------------------------------------
            // 2. BACKWARD COMPATIBLE PATHWAY (LOCAL DIRECT ENTRY FORM)
            // ----------------------------------------------------
            var activeTab by remember { mutableStateOf(0) } // 0 = Xtream, 1 = M3U
            
            // Xtream States
            var xtreamName by remember { mutableStateOf("") }
            var xtreamUrl by remember { mutableStateOf("") }
            var xtreamUser by remember { mutableStateOf("") }
            var xtreamPass by remember { mutableStateOf("") }
            
            // M3U States
            var m3uName by remember { mutableStateOf("") }
            var m3uUrl by remember { mutableStateOf("") }
            var passwordVisible by remember { mutableStateOf(false) }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 650.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { showLocalForms = false },
                        modifier = Modifier
                            .background(SoftGrey, RoundedCornerShape(10.dp))
                            .size(40.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back back", tint = TextWhite)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Direct IPTV Setup",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        modifier = Modifier.clickable {
                            adminClickCount++
                            if (adminClickCount >= 5) {
                                showAdminConsoleByUser = true
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (showAdminConsoleByUser) {
                    AdminProvisioningConsole(
                        viewModel = viewModel,
                        onClose = { showAdminConsoleByUser = false }
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }

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
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        if (activeTab == 0) {
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
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            
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
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            
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
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            
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
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            
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
                        
                        Button(
                            onClick = {
                                if (activeTab == 0) {
                                    viewModel.addXtreamCodesPlaylist(xtreamName, xtreamUrl, xtreamUser, xtreamPass)
                                } else {
                                    viewModel.addM3uPlaylist(m3uName, m3uUrl)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CinemaGold, contentColor = MidnightNavy),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("connect_button"),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isAuthenticating
                        ) {
                            if (isAuthenticating) {
                                CircularProgressIndicator(color = MidnightNavy, modifier = Modifier.size(24.dp))
                            } else {
                                Icon(Icons.Default.CloudSync, contentDescription = "Loc Sync connect")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(viewModel.getString("connect"), fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Demo provisioner card selection
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
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ElectricCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PlayCircle, contentDescription = "demo play sign", tint = ElectricCyan, modifier = Modifier.size(24.dp))
                        }
                        
                        Spacer(modifier = Modifier.width(14.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(viewModel.getString("load_demo"), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ElectricCyan)
                            Text(viewModel.getString("demo_desc"), fontSize = 11.sp, color = TextMuted, modifier = Modifier.padding(top = 1.dp))
                        }
                        
                        Icon(Icons.Default.ArrowForwardIos, contentDescription = "demo arrow mark", tint = ElectricCyan, modifier = Modifier.size(14.dp))
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                DeviceProvisioningCodeCard(code = viewModel.deviceProvisioningCode)
                Spacer(modifier = Modifier.height(48.dp))
            }
        } 
        
        else {
            // ----------------------------------------------------
            // 3. SECURE IPTV CLOUD REMOTE PROVISIONING PANEL (LOGGED IN)
            // ----------------------------------------------------
            var activeProvTab by remember { mutableStateOf(0) } // 0 = Xtream Codes API, 1 = M3U Playlist Link

            // Provision Form States
            var provName by remember { mutableStateOf("") }
            var provHost by remember { mutableStateOf("") }
            var provPort by remember { mutableStateOf("") }
            var provUsername by remember { mutableStateOf("") }
            var provPassword by remember { mutableStateOf("") }
            var provM3uUrl by remember { mutableStateOf("") }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 760.dp)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Display Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SteelSlate)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.clickable {
                            adminClickCount++
                            if (adminClickCount >= 5) {
                                showAdminConsoleByUser = true
                            }
                        }
                    ) {
                        Text(
                            text = if (showAdminConsoleByUser) "Cloud Sync Session (Admin Mode Enabled)" else "Cloud Sync Session",
                            fontSize = 11.sp,
                            color = if (showAdminConsoleByUser) ElectricCyan else CinemaGold,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = firebaseUser?.email ?: "Active Client",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextWhite
                        )
                    }

                    Button(
                        onClick = { viewModel.logoutFirebase() },
                        colors = ButtonDefaults.buttonColors(containerColor = SoftGrey),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.PowerSettingsNew, contentDescription = "Log out profile icon", tint = ActiveRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sign Out", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable Container for Provision Form & Active Stream Portals
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (showAdminConsoleByUser) {
                        item {
                            AdminProvisioningConsole(
                                viewModel = viewModel,
                                onClose = { showAdminConsoleByUser = false }
                            )
                        }
                    }

                    // Item A: Provision form
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = SteelSlate.copy(alpha = 0.95f)),
                            border = BorderStroke(1.dp, SoftGrey)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = viewModel.getString("provision_title"),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CinemaGold,
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                )

                                // Tab Selector inside Provision Frame
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MidnightNavy)
                                        .padding(3.dp)
                                ) {
                                    Button(
                                        onClick = { activeProvTab = 0 },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (activeProvTab == 0) CinemaGold else Color.Transparent,
                                            contentColor = if (activeProvTab == 0) MidnightNavy else TextWhite
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Xtream API", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = { activeProvTab = 1 },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (activeProvTab == 1) CinemaGold else Color.Transparent,
                                            contentColor = if (activeProvTab == 1) MidnightNavy else TextWhite
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("M3U Link", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Shared Name field
                                OutlinedTextField(
                                    value = provName,
                                    onValueChange = { provName = it },
                                    label = { Text("Provider/Portal Name", color = TextMuted) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CinemaGold, unfocusedBorderColor = SoftGrey),
                                    modifier = Modifier.fillMaxWidth().testTag("provision_name_input")
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                if (activeProvTab == 0) {
                                    // Host URL
                                    OutlinedTextField(
                                        value = provHost,
                                        onValueChange = { provHost = it },
                                        label = { Text("Host URL / Domain", color = TextMuted) },
                                        placeholder = { Text("e.g. portal.domain.com", color = TextMuted.copy(alpha = 0.4f)) },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CinemaGold, unfocusedBorderColor = SoftGrey),
                                        modifier = Modifier.fillMaxWidth().testTag("provision_host_input")
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Port URL
                                    OutlinedTextField(
                                        value = provPort,
                                        onValueChange = { provPort = it },
                                        label = { Text("Port", color = TextMuted) },
                                        placeholder = { Text("e.g. 8080 (Leave empty for default)", color = TextMuted.copy(alpha = 0.4f)) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CinemaGold, unfocusedBorderColor = SoftGrey),
                                        modifier = Modifier.fillMaxWidth().testTag("provision_port_input")
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Username
                                    OutlinedTextField(
                                        value = provUsername,
                                        onValueChange = { provUsername = it },
                                        label = { Text("Username", color = TextMuted) },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CinemaGold, unfocusedBorderColor = SoftGrey),
                                        modifier = Modifier.fillMaxWidth().testTag("provision_username_input")
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Password
                                    OutlinedTextField(
                                        value = provPassword,
                                        onValueChange = { provPassword = it },
                                        label = { Text("Password", color = TextMuted) },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CinemaGold, unfocusedBorderColor = SoftGrey),
                                        modifier = Modifier.fillMaxWidth().testTag("provision_password_input")
                                    )
                                } else {
                                    // M3U URL Input
                                    OutlinedTextField(
                                        value = provM3uUrl,
                                        onValueChange = { provM3uUrl = it },
                                        label = { Text("M3U Playlist URL Link", color = TextMuted) },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CinemaGold, unfocusedBorderColor = SoftGrey),
                                        modifier = Modifier.fillMaxWidth().testTag("provision_m3u_input")
                                    )
                                }

                                if (errorMsg != null) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(text = errorMsg ?: "", color = ActiveRed, fontSize = 12.sp, textAlign = TextAlign.Center)
                                }

                                if (syncStatusMessage != null) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(text = syncStatusMessage ?: "", color = ElectricCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        if (activeProvTab == 0) {
                                            viewModel.provisionPortal(provName, provHost, provPort, provUsername, provPassword, "XTREAM")
                                        } else {
                                            viewModel.provisionPortal(provName, "m3u_sync_source", "", "", "", "M3U")
                                            // Trigger manual sync under local db
                                            viewModel.addM3uPlaylist(provName, provM3uUrl)
                                        }
                                        // Reset fields
                                        provName = ""
                                        provHost = ""
                                        provPort = ""
                                        provUsername = ""
                                        provPassword = ""
                                        provM3uUrl = ""
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CinemaGold, contentColor = MidnightNavy),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().height(46.dp).testTag("provision_save_button")
                                ) {
                                    Icon(Icons.Default.CloudUpload, contentDescription = "upload sync system")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("UPLOAD & PROVISION", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    // Item B: Provisioned list headers
                    item {
                        Text(
                            text = "Cloud Provisioned Portals (${provisionedPortals.size})",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextWhite,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }

                    // Empty feedback
                    if (provisionedPortals.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = SteelSlate)
                            ) {
                                Text(
                                    text = "No remotely provisioned portals found. Create one above to instantly synchronize it with the Cloud database.",
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(24.dp)
                                )
                            }
                        }
                    }

                    // List items
                    items(provisionedPortals) { portal ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (portal.type == "XTREAM") {
                                        // Server url is assembled dynamically
                                        val portSuffix = if (portal.port.isBlank()) "" else ":${portal.port}"
                                        val hostWithProtocol = if (portal.hostUrl.startsWith("http")) portal.hostUrl else "http://${portal.hostUrl}"
                                        val serverUrl = "$hostWithProtocol$portSuffix"
                                        viewModel.addXtreamCodesPlaylist(portal.name, serverUrl, portal.username, portal.password)
                                    } else {
                                        viewModel.addM3uPlaylist(portal.name, portal.url)
                                    }
                                }
                                .testTag("connect_provisioned_item_card"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SoftGrey),
                            border = BorderStroke(1.dp, CinemaGold.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(CinemaGold.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (portal.type == "XTREAM") Icons.Default.Router else Icons.Default.FeaturedPlayList,
                                        contentDescription = "Portal Item",
                                        tint = CinemaGold,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = portal.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextWhite
                                    )
                                    Text(
                                        text = if (portal.type == "XTREAM") "${portal.hostUrl}:${portal.port} (${portal.username})" else "M3U Link Sub",
                                        fontSize = 11.sp,
                                        color = TextMuted,
                                        modifier = Modifier.padding(top = 1.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.deleteProvisionedPortal(portal.id) },
                                    modifier = Modifier.testTag("delete_provisioned_${portal.id}")
                                ) {
                                    Icon(Icons.Default.DeleteSweep, contentDescription = "Delete synchronization", tint = ActiveRed)
                                }
                            }
                        }
                    }

                    // Bottom list spacer
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceProvisioningCodeCard(code: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("device_provisioning_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SoftGrey.copy(alpha = 0.5f)),
        border = BorderStroke(1.2.dp, CinemaGold.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Tv,
                    contentDescription = "Remote Provisioning",
                    tint = CinemaGold,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "REMOTE PROVISIONING CODE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = CinemaGold,
                    letterSpacing = 1.2.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = code.chunked(4).joinToString(" "),
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextWhite,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Provide this unique code to an administrator or service provider to remotely configure IPTV channels on this device.",
                fontSize = 11.sp,
                color = TextMuted,
                textAlign = TextAlign.Center,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
fun AdminProvisioningConsole(
    viewModel: IptvViewModel,
    onClose: () -> Unit
) {
    val isProvisioning by viewModel.isRemoteProvisioning.collectAsState()
    val provisionSuccess by viewModel.remoteProvisionSuccess.collectAsState()
    val provisionError by viewModel.remoteProvisionError.collectAsState()

    var targetCode by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("XTREAM") } // "XTREAM" or "M3U"
    var m3uUrl by remember { mutableStateOf("") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_provisioning_console"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MidnightNavy.copy(alpha = 0.95f)),
        border = BorderStroke(1.5.dp, ElectricCyan)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Admin Console Icon",
                        tint = ElectricCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ADMIN REMOTE PUSH CONSOLE",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ElectricCyan,
                        letterSpacing = 1.sp
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close admin panel", tint = ActiveRed, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Admins can deploy IPTV portal credentials directly into a user's cloud session via Firebase realtime Sync.",
                fontSize = 11.sp,
                color = TextMuted,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Target Device 8-digit provisioning code
            OutlinedTextField(
                value = targetCode,
                onValueChange = { if (it.length <= 8) targetCode = it },
                label = { Text("Target Device Code (8-digits)", color = ElectricCyan) },
                placeholder = { Text("e.g. 12345678", color = TextMuted.copy(alpha = 0.3f)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = SoftGrey,
                    focusedLabelColor = ElectricCyan
                ),
                modifier = Modifier.fillMaxWidth().testTag("admin_target_code_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Playlist Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Playlist / Portal Name", color = TextMuted) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricCyan, unfocusedBorderColor = SoftGrey),
                modifier = Modifier.fillMaxWidth().testTag("admin_playlist_name_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Type tab selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MidnightNavy)
                    .padding(3.dp)
            ) {
                Button(
                    onClick = { type = "XTREAM" },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (type == "XTREAM") ElectricCyan else Color.Transparent,
                        contentColor = if (type == "XTREAM") MidnightNavy else TextWhite
                    ),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("XTREAM API", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { type = "M3U" },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (type == "M3U") ElectricCyan else Color.Transparent,
                        contentColor = if (type == "M3U") MidnightNavy else TextWhite
                    ),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("M3U PLAYLIST Link", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (type == "XTREAM") {
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text("Server Host / Domain", color = TextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricCyan, unfocusedBorderColor = SoftGrey),
                    modifier = Modifier.fillMaxWidth().testTag("admin_host_input")
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = port,
                        onValueChange = { port = it },
                        label = { Text("Port", color = TextMuted) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricCyan, unfocusedBorderColor = SoftGrey),
                        modifier = Modifier.weight(1f).testTag("admin_port_input")
                    )
                    OutlinedTextField(
                        value = user,
                        onValueChange = { user = it },
                        label = { Text("Username", color = TextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricCyan, unfocusedBorderColor = SoftGrey),
                        modifier = Modifier.weight(2f).testTag("admin_user_input")
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = pass,
                    onValueChange = { pass = it },
                    label = { Text("Password", color = TextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricCyan, unfocusedBorderColor = SoftGrey),
                    modifier = Modifier.fillMaxWidth().testTag("admin_pass_input")
                )
            } else {
                OutlinedTextField(
                    value = m3uUrl,
                    onValueChange = { m3uUrl = it },
                    label = { Text("M3U Playlist Source URL Link", color = TextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricCyan, unfocusedBorderColor = SoftGrey),
                    modifier = Modifier.fillMaxWidth().testTag("admin_m3u_url_input")
                )
            }

            if (provisionError != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = provisionError ?: "", color = ActiveRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            if (provisionSuccess == true) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = "✓ Remote Provision Pushed Successfully!", color = ElectricCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    viewModel.remoteProvisionDevice(
                        targetCode = targetCode,
                        name = name,
                        host = host,
                        port = port,
                        user = user,
                        pass = pass,
                        type = type,
                        url = m3uUrl
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = MidnightNavy),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("admin_send_btn"),
                enabled = !isProvisioning
            ) {
                if (isProvisioning) {
                    CircularProgressIndicator(color = MidnightNavy, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.Send, contentDescription = "send admin cmd")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("PUSH REMOTE CONFIG", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}
