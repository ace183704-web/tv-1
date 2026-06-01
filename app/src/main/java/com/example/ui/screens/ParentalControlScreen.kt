package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.IptvViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ParentalControlScreen(viewModel: IptvViewModel) {
    val config by viewModel.parentalConfig.collectAsState()
    
    var pinText by remember { mutableStateOf("") }
    var pinActive by remember { mutableStateOf(false) }
    var successAlertMessage by remember { mutableStateOf<String?>(null) }

    // Synchronize local UI state when DB is fetched
    LaunchedEffect(config) {
        config?.let {
            pinText = it.pin
            pinActive = it.isLockActive
        }
    }

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
                .padding(24.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.navigateTo("dashboard") },
                    modifier = Modifier.testTag("parental_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = CinemaGold
                    )
                }
                Text(
                    text = viewModel.getString("parental"),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextWhite,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Informational Card detailing how locks operate
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SoftGrey.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = ElectricCyan,
                        modifier = Modifier
                            .size(24.dp)
                            .padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Security Locks Overview",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricCyan
                        )
                        Text(
                            text = viewModel.getString("parental_desc"),
                            fontSize = 12.sp,
                            color = TextMuted,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Configuration Canvas
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SoftGrey.copy(alpha = 0.85f))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    // Item: Toggle Enable locks
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Security Active Icon",
                                tint = if (pinActive) CinemaGold else TextMuted
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Activate Restriction Lock",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                                Text(
                                    text = if (pinActive) "Sensitive channels require PIN" else "No channels are locked",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        }

                        Switch(
                            checked = pinActive,
                            onCheckedChange = { pinActive = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MidnightNavy,
                                checkedTrackColor = CinemaGold
                            ),
                            modifier = Modifier.testTag("parental_lock_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = SoftGrey)
                    Spacer(modifier = Modifier.height(24.dp))

                    // Item: Numeric master PIN field configurer
                    Text(
                        text = viewModel.getString("set_pin"),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = pinText,
                        onValueChange = {
                            // Enforce numerical characters and restrict to max length 4
                            if (it.all { char -> char.isDigit() } && it.length <= 4) {
                                pinText = it
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        placeholder = { Text("0000", color = TextMuted.copy(alpha = 0.4f)) },
                        singleLine = true,
                        leadingIcon = { Icon(imageVector = Icons.Default.Pin, contentDescription = "PIN padlock logo", tint = CinemaGold) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CinemaGold,
                            unfocusedBorderColor = SoftGrey,
                            focusedLabelColor = CinemaGold
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("parental_pin_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Save alert confirmation status block
                    AnimatedVisibility(visible = successAlertMessage != null) {
                        Text(
                            text = successAlertMessage ?: "",
                            color = Color.Green,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Action buttons
                    Button(
                        onClick = {
                            if (pinText.length != 4) {
                                successAlertMessage = "Warning: PIN lock must consist of exactly 4 digits."
                            } else {
                                viewModel.savePINCode(pinText, pinActive)
                                successAlertMessage = if (pinActive) viewModel.getString("lock_activated") else viewModel.getString("lock_disabled")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CinemaGold,
                            contentColor = MidnightNavy
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("parental_save_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = "Save Lock logo")
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = viewModel.getString("save_changes"), fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}
