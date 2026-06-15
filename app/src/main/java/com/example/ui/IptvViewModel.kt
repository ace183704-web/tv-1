package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class FilterParams(
    val playlistId: Long,
    val type: String,
    val category: String?,
    val query: String
)

class IptvViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "IptvViewModel"
    private val database = IptvDatabase.getDatabase(application)
    private val repository = IptvRepository(database.iptvDao())

    // Language switching
    val currentLanguage = MutableStateFlow("en")

    // IPTV Smarters Settings States
    val streamFormat = MutableStateFlow("HLS (.m3u8)")
    val timeFormat24h = MutableStateFlow(false)
    val automationAutoRefresh = MutableStateFlow(true)
    val automationLoadLast = MutableStateFlow(false)
    val automationBootStart = MutableStateFlow(false)
    val playerBuffering = MutableStateFlow("Standard - 2s")
    val playerHwDecoders = MutableStateFlow(true)
    val activeExternalPlayer = MutableStateFlow("Internal Player")
    val multiScreenLayout = MutableStateFlow("Dual Screen (2 Streams)")
    val vpnConnected = MutableStateFlow(false)
    val vpnLocationSelected = MutableStateFlow("USA - New York")
    val diagnosticLoggingEnabled = MutableStateFlow(false)

    // Playlists
    val playlists: StateFlow<List<Playlist>> = repository.playlists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activePlaylist = MutableStateFlow<Playlist?>(null)

    // Browsing Category structure: "live", "movie", "series", "favorites"
    val activeType = MutableStateFlow("live")
    
    // Sub-category names: "All Channels", "Action Movies", etc.
    val activeCategory = MutableStateFlow<String?>(null)

    // Search queries
    val searchQuery = MutableStateFlow("")

    // Parent PIN settings
    val parentalConfig: StateFlow<ParentalConfig?> = repository.getParentalConfig()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ParentalConfig(1, "0000", false))

    // Stream list reactive flow
    @OptIn(ExperimentalCoroutinesApi::class)
    val streamsList: StateFlow<List<StreamItem>> = combine(
        combine(activePlaylist, activeType) { p, t -> Pair(p, t) },
        combine(activeCategory, searchQuery) { c, q -> Pair(c, q) }
    ) { pt, cq ->
        val playlist = pt.first
        val type = pt.second
        val category = cq.first
        val query = cq.second
        FilterParams(playlist?.id ?: -1L, type, category, query)
    }.flatMapLatest { params ->
        val playlistId = params.playlistId
        if (playlistId == -1L) {
            flowOf(emptyList())
        } else {
            val baseFlow = when {
                params.type == "favorites" -> repository.getFavoriteStreams(playlistId)
                params.category != null -> repository.getStreamsByCategory(playlistId, params.type, params.category)
                else -> repository.getStreamsByType(playlistId, params.type)
            }
            
            if (params.query.isNotEmpty()) {
                baseFlow.map { list ->
                    list.filter { it.name.contains(params.query, ignoreCase = true) }
                }
            } else {
                baseFlow
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Category lists reactive flow
    @OptIn(ExperimentalCoroutinesApi::class)
    val categoriesList: StateFlow<List<String>> = combine(
        activePlaylist,
        activeType
    ) { playlist: Playlist?, type: String ->
        if (playlist == null) Pair(-1L, type) else Pair(playlist.id, type)
    }.flatMapLatest { pair ->
        val playlistId = pair.first
        val type = pair.second
        if (playlistId == -1L || type == "favorites") {
            flowOf(emptyList())
        } else {
            repository.getCategoriesByType(playlistId, type).map { list ->
                listOf("All") + list.sorted()
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // EPG Guide live scheduling
    val currentEpgList = MutableStateFlow<List<EpgProgram>>(emptyList())

    // Select Player variables
    val selectedStream = MutableStateFlow<StreamItem?>(null)
    val selectedPlayerCore = MutableStateFlow("ExoPlayer") // "ExoPlayer", "VLC Core", "MPV Core"

    // App Navigation router
    val navigationDestination = MutableStateFlow("login")

    // Operation loadings
    val isAuthenticating = MutableStateFlow(false)
    val addPlaylistError = MutableStateFlow<String?>(null)

    // Firebase Cloud Provisioning States
    val firebaseUser = FirebaseManager.currentUser
    val provisionedPortals = FirebaseManager.provisionedPortals
    val firebaseSyncStatus = FirebaseManager.syncStatusMessage
    val firebaseAuthError = MutableStateFlow<String?>(null)

    fun registerWithFirebase(email: String, pass: String) {
        viewModelScope.launch {
            if (email.isBlank() || pass.isBlank()) {
                firebaseAuthError.value = "Email and password cannot be empty."
                return@launch
            }
            isAuthenticating.value = true
            firebaseAuthError.value = null
            val result = FirebaseManager.signUpWithEmail(getApplication(), email, pass)
            if (result.isSuccess) {
                firebaseAuthError.value = null
            } else {
                firebaseAuthError.value = result.exceptionOrNull()?.message ?: "Registration failed."
            }
            isAuthenticating.value = false
        }
    }

    fun loginWithFirebase(email: String, pass: String) {
        viewModelScope.launch {
            if (email.isBlank() || pass.isBlank()) {
                firebaseAuthError.value = "Email and password cannot be empty."
                return@launch
            }
            isAuthenticating.value = true
            firebaseAuthError.value = null
            val result = FirebaseManager.signInWithEmail(getApplication(), email, pass)
            if (result.isSuccess) {
                firebaseAuthError.value = null
            } else {
                firebaseAuthError.value = result.exceptionOrNull()?.message ?: "Authentication failed."
            }
            isAuthenticating.value = false
        }
    }

    fun loginWithSocial(provider: String) {
        viewModelScope.launch {
            isAuthenticating.value = true
            firebaseAuthError.value = null
            val result = FirebaseManager.loginWithSocial(getApplication(), provider)
            if (result.isSuccess) {
                firebaseAuthError.value = null
            } else {
                firebaseAuthError.value = result.exceptionOrNull()?.message ?: "Social auth failed."
            }
            isAuthenticating.value = false
        }
    }

    fun logoutFirebase() {
        FirebaseManager.clearSession(getApplication())
    }

    fun provisionPortal(name: String, host: String, port: String, user: String, pass: String, type: String) {
        viewModelScope.launch {
            if (name.isBlank() || host.isBlank() || user.isBlank() || pass.isBlank()) {
                addPlaylistError.value = "All credentials fields are required."
                return@launch
            }
            isAuthenticating.value = true
            addPlaylistError.value = null
            val id = "portal_" + System.currentTimeMillis()
            val portal = ProvisionedPortal(
                id = id,
                name = name,
                hostUrl = host,
                port = port,
                username = user,
                password = pass,
                type = type
            )
            val result = FirebaseManager.addProvisionedPortal(getApplication(), portal)
            if (result.isSuccess) {
                addPlaylistError.value = null
            } else {
                addPlaylistError.value = "Failed to sync provision: " + result.exceptionOrNull()?.message
            }
            isAuthenticating.value = false
        }
    }

    fun deleteProvisionedPortal(id: String) {
        viewModelScope.launch {
            FirebaseManager.deleteProvisionedPortal(getApplication(), id)
        }
    }

    val deviceProvisioningCode = FirebaseManager.getProvisioningCode(application)
    val remoteProvisionSuccess = MutableStateFlow<Boolean?>(null)
    val remoteProvisionError = MutableStateFlow<String?>(null)
    val isRemoteProvisioning = MutableStateFlow(false)

    fun remoteProvisionDevice(
        targetCode: String,
        name: String,
        host: String,
        port: String,
        user: String,
        pass: String,
        type: String = "XTREAM",
        url: String = ""
    ) {
        viewModelScope.launch {
            if (targetCode.length != 8 && targetCode.length != 16) {
                remoteProvisionError.value = "Target code must be an 8-digit pairing code or a 16-character ANDROID_ID."
                return@launch
            }
            if (name.isBlank() || (type == "XTREAM" && (host.isBlank() || user.isBlank() || pass.isBlank())) || (type == "M3U" && url.isBlank())) {
                remoteProvisionError.value = "Please fill all required playlist credentials fields."
                return@launch
            }
            isRemoteProvisioning.value = true
            remoteProvisionSuccess.value = null
            remoteProvisionError.value = null

            val result = FirebaseManager.remoteProvisionPortal(
                getApplication(),
                targetCode,
                name,
                host,
                port,
                user,
                pass,
                type,
                url
            )

            if (result.isSuccess) {
                remoteProvisionSuccess.value = true
                remoteProvisionError.value = null
            } else {
                remoteProvisionSuccess.value = false
                remoteProvisionError.value = result.exceptionOrNull()?.message ?: "Remote provisioning failed."
            }
            isRemoteProvisioning.value = false
        }
    }

    fun refreshDeviceRegistration() {
        viewModelScope.launch {
            FirebaseManager.registerDeviceForProvisioning(getApplication())
            FirebaseManager.loadProvisionedPortals(getApplication())
        }
    }

    init {
        val prefs = application.getSharedPreferences("iptv_settings_prefs", android.content.Context.MODE_PRIVATE)
        diagnosticLoggingEnabled.value = prefs.getBoolean("diagnostic_logging_enabled", false)
        FirebaseManager.diagnosticLoggingEnabled = diagnosticLoggingEnabled.value

        viewModelScope.launch {
            diagnosticLoggingEnabled.collect { enabled ->
                FirebaseManager.diagnosticLoggingEnabled = enabled
                prefs.edit().putBoolean("diagnostic_logging_enabled", enabled).apply()
            }
        }

        // Automatically check if playlists are loaded from previous launches
        viewModelScope.launch {
            playlists.collect { list ->
                if (list.isNotEmpty() && activePlaylist.value == null) {
                    activePlaylist.value = list.first()
                    navigationDestination.value = "dashboard"
                }
            }
        }

        // Keep device registered for remote provisioning upon configuration/auth change
        viewModelScope.launch {
            FirebaseManager.currentUser.collect { user ->
                FirebaseManager.registerDeviceForProvisioning(getApplication())
                FirebaseManager.loadProvisionedPortals(getApplication())
            }
        }
    }

    /**
     * Translates custom applet strings relative to selected translation code.
     */
    fun getString(key: String): String {
        return LanguageHelper.getString(currentLanguage.value, key)
    }

    fun setLanguage(lang: String) {
        currentLanguage.value = lang
    }

    fun navigateTo(dest: String) {
        navigationDestination.value = dest
        // Clear searches when navigating screens
        searchQuery.value = ""
    }

    fun selectPlaylist(playlist: Playlist) {
        activePlaylist.value = playlist
        activeCategory.value = null
        activeType.value = "live"
        navigateTo("dashboard")
    }

    fun selectCategory(category: String) {
        activeCategory.value = if (category == "All") null else category
    }

    fun selectType(type: String) {
        activeType.value = type
        activeCategory.value = null
    }

    fun toggleFavorite(stream: StreamItem) {
        viewModelScope.launch {
            repository.updateFavorite(stream.id, !stream.isFavorite)
        }
    }

    fun toggleStreamLock(stream: StreamItem) {
        viewModelScope.launch {
            repository.updateLocked(stream.id, !stream.isLocked)
        }
    }

    fun selectStreamForPlayback(stream: StreamItem) {
        selectedStream.value = stream
        // Sync custom EPG listings
        activePlaylist.value?.let { playlist ->
            viewModelScope.launch {
                repository.getEPGForChannel(playlist.id, stream.streamId).collect { epg ->
                    currentEpgList.value = epg
                }
            }
        }
        navigateTo("player")
    }

    fun setPlayerCore(core: String) {
        selectedPlayerCore.value = core
    }

    fun savePINCode(pin: String, active: Boolean) {
        viewModelScope.launch {
            repository.saveParentalConfig(ParentalConfig(1, pin, active))
        }
    }

    fun loadDemoProvider() {
        viewModelScope.launch {
            isAuthenticating.value = true
            addPlaylistError.value = null
            try {
                val pId = repository.setupDemoPlaylist()
                val playlist = database.iptvDao().getPlaylistById(pId)
                if (playlist != null) {
                    selectPlaylist(playlist)
                }
            } catch (e: Exception) {
                addPlaylistError.value = "Demo system failed to sync: ${e.message}"
            } finally {
                isAuthenticating.value = false
            }
        }
    }

    fun addM3uPlaylist(name: String, url: String) {
        viewModelScope.launch {
            if (name.isBlank() || url.isBlank()) {
                addPlaylistError.value = "Please complete all text parameters."
                return@launch
            }
            isAuthenticating.value = true
            addPlaylistError.value = null
            
            var cleanedUrl = url.trim()
            if (!cleanedUrl.startsWith("http://") && !cleanedUrl.startsWith("https://")) {
                cleanedUrl = "http://$cleanedUrl"
            }
            
            val result = repository.addM3UPlaylist(name, cleanedUrl)
            if (result.isSuccess) {
                val pId = result.getOrThrow()
                val playlist = database.iptvDao().getPlaylistById(pId)
                if (playlist != null) {
                    selectPlaylist(playlist)
                }
            } else {
                addPlaylistError.value = result.exceptionOrNull()?.message ?: "Failed loading M3U Playlist."
            }
            isAuthenticating.value = false
        }
    }

    fun addXtreamCodesPlaylist(name: String, serverUrl: String, user: String, pass: String) {
        viewModelScope.launch {
            if (name.isBlank() || serverUrl.isBlank() || user.isBlank() || pass.isBlank()) {
                addPlaylistError.value = "Please complete all field requirements."
                return@launch
            }
            isAuthenticating.value = true
            addPlaylistError.value = null
            
            var cleanedUrl = serverUrl.trim()
            if (!cleanedUrl.startsWith("http://") && !cleanedUrl.startsWith("https://")) {
                cleanedUrl = "http://$cleanedUrl"
            }
            
            val result = repository.addXtreamCodesPlaylist(name, cleanedUrl, user.trim(), pass.trim())
            if (result.isSuccess) {
                val pId = result.getOrThrow()
                val playlist = database.iptvDao().getPlaylistById(pId)
                if (playlist != null) {
                    selectPlaylist(playlist)
                }
            } else {
                addPlaylistError.value = result.exceptionOrNull()?.message ?: "Xtream Connection refused."
            }
            isAuthenticating.value = false
        }
    }

    // Playlist Refresh States
    val isRefreshingPlaylist = MutableStateFlow(false)
    val refreshPlaylistError = MutableStateFlow<String?>(null)
    val refreshPlaylistSuccess = MutableStateFlow(false)

    fun refreshActivePlaylist() {
        val currentPlaylist = activePlaylist.value ?: return
        viewModelScope.launch {
            isRefreshingPlaylist.value = true
            refreshPlaylistError.value = null
            refreshPlaylistSuccess.value = false
            
            val result = repository.refreshPlaylist(currentPlaylist)
            if (result.isSuccess) {
                refreshPlaylistSuccess.value = true
                refreshPlaylistError.value = null
            } else {
                refreshPlaylistSuccess.value = false
                refreshPlaylistError.value = result.exceptionOrNull()?.message ?: "Failed to refresh playlist."
            }
            isRefreshingPlaylist.value = false
        }
    }

    fun removePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            repository.deletePlaylist(playlist.id)
            if (activePlaylist.value?.id == playlist.id) {
                activePlaylist.value = null
                navigateTo("login")
            }
        }
    }
}
