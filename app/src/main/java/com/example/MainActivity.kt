package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.IptvViewModel
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Instantiate our lifecycle-secured IPTV ViewModel directly
        val viewModel = IptvViewModel(application)
        
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    val dest by viewModel.navigationDestination.collectAsState()
                    
                    // Immersive Crossfade Animation for navigating screens under 300ms
                    Crossfade(
                        targetState = dest,
                        label = "NavigationTransition"
                    ) { screen ->
                        when (screen) {
                            "login" -> LoginScreen(viewModel)
                            "dashboard" -> DashboardScreen(viewModel)
                            "list_viewer" -> ListViewerScreen(viewModel)
                            "player" -> PlayerScreen(viewModel)
                            "parental_controls" -> ParentalControlScreen(viewModel)
                            "settings" -> SettingsScreen(viewModel)
                            else -> LoginScreen(viewModel)
                        }
                    }
                }
            }
        }
    }
}
