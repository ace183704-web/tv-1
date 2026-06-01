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

    init {
        // Automatically check if playlists are loaded from previous launches
        viewModelScope.launch {
            playlists.collect { list ->
                if (list.isNotEmpty() && activePlaylist.value == null) {
                    activePlaylist.value = list.first()
                    navigationDestination.value = "dashboard"
                }
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
                    activePlaylist.value = playlist
                    navigateTo("dashboard")
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
            
            val result = repository.addM3UPlaylist(name, url)
            if (result.isSuccess) {
                val pId = result.getOrThrow()
                val playlist = database.iptvDao().getPlaylistById(pId)
                if (playlist != null) {
                    activePlaylist.value = playlist
                    navigateTo("dashboard")
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
            
            val result = repository.addXtreamCodesPlaylist(name, serverUrl, user, pass)
            if (result.isSuccess) {
                val pId = result.getOrThrow()
                val playlist = database.iptvDao().getPlaylistById(pId)
                if (playlist != null) {
                    activePlaylist.value = playlist
                    navigateTo("dashboard")
                }
            } else {
                addPlaylistError.value = result.exceptionOrNull()?.message ?: "Xtream Connection refused."
            }
            isAuthenticating.value = false
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
