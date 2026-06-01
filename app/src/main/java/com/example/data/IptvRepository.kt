package com.example.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class IptvRepository(private val dao: IptvDao) {
    private val TAG = "IptvRepository"

    val playlists: Flow<List<Playlist>> = dao.getAllPlaylists()
        .flowOn(Dispatchers.IO)

    fun getStreamsByType(playlistId: Long, categoryType: String): Flow<List<StreamItem>> {
        return dao.getStreamsByType(playlistId, categoryType).flowOn(Dispatchers.IO)
    }

    fun getCategoriesByType(playlistId: Long, categoryType: String): Flow<List<String>> {
        return dao.getCategoriesByType(playlistId, categoryType).flowOn(Dispatchers.IO)
    }

    fun getStreamsByCategory(playlistId: Long, categoryType: String, categoryName: String): Flow<List<StreamItem>> {
        return dao.getStreamsByCategory(playlistId, categoryType, categoryName).flowOn(Dispatchers.IO)
    }

    fun getFavoriteStreams(playlistId: Long): Flow<List<StreamItem>> {
        return dao.getFavoriteStreams(playlistId).flowOn(Dispatchers.IO)
    }

    fun searchStreams(playlistId: Long, query: String): Flow<List<StreamItem>> {
        return dao.searchStreams(playlistId, "%$query%").flowOn(Dispatchers.IO)
    }

    fun getEPGForChannel(playlistId: Long, channelId: String): Flow<List<EpgProgram>> {
        return dao.getEPGForChannel(playlistId, channelId, System.currentTimeMillis()).flowOn(Dispatchers.IO)
    }

    fun getParentalConfig(): Flow<ParentalConfig?> {
        return dao.getParentalConfig().flowOn(Dispatchers.IO)
    }

    suspend fun saveParentalConfig(config: ParentalConfig) = withContext(Dispatchers.IO) {
        dao.saveParentalConfig(config)
    }

    suspend fun updateFavorite(id: Long, isFav: Boolean) = withContext(Dispatchers.IO) {
        dao.updateFavorite(id, isFav)
    }

    suspend fun updateLocked(id: Long, isLock: Boolean) = withContext(Dispatchers.IO) {
        dao.updateLocked(id, isLock)
    }

    suspend fun deletePlaylist(playlistId: Long) = withContext(Dispatchers.IO) {
        dao.deleteStreamsByPlaylist(playlistId)
        dao.deleteEpgByPlaylist(playlistId)
        dao.deletePlaylist(playlistId)
    }

    suspend fun addM3UPlaylist(name: String, url: String): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val playlist = Playlist(name = name, url = url, type = "M3U")
            val playlistId = dao.insertPlaylist(playlist)
            
            val streams = IptvParser.parseM3U(url, playlistId)
            if (streams.isNotEmpty()) {
                dao.insertStreams(streams)
                Result.success(playlistId)
            } else {
                // If remote M3U parsing returned empty streams, delete the provisional entry
                dao.deletePlaylist(playlistId)
                Result.failure(Exception("Failed to fetch or parse M3U. No valid streams found."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "M3U addition error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun addXtreamCodesPlaylist(
        name: String,
        serverUrl: String,
        username: String,
        password: String
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val isVerified = IptvParser.verifyXtreamCodes(serverUrl, username, password)
            if (!isVerified) {
                return@withContext Result.failure(Exception("Xtream Codes authentication failed. Please check credentials or host."))
            }
            
            val playlist = Playlist(
                name = name,
                url = serverUrl,
                username = username,
                password = password,
                type = "XTREAM"
            )
            val playlistId = dao.insertPlaylist(playlist)
            
            val streams = IptvParser.fetchXtreamStreams(serverUrl, username, password, playlistId)
            if (streams.isNotEmpty()) {
                dao.insertStreams(streams)
                Result.success(playlistId)
            } else {
                dao.deletePlaylist(playlistId)
                Result.failure(Exception("Authenticated successfully, but no streams were returned from the server."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Xtream addition error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun setupDemoPlaylist(): Long = withContext(Dispatchers.IO) {
        val playlist = Playlist(
            name = "Demo Stream Provider",
            url = "https://demo.iptvextreme.com/api",
            type = "XTREAM",
            username = "demo_user",
            password = "demo_password"
        )
        val playlistId = dao.insertPlaylist(playlist)
        val (streams, epg) = IptvParser.generateDemoStreams(playlistId)
        
        dao.insertStreams(streams)
        dao.insertEpgPrograms(epg)
        
        // Initialize parental configs with 0000 PIN if not already initialized
        val defaultParental = ParentalConfig(1, "0000", false)
        dao.saveParentalConfig(defaultParental)
        
        playlistId
    }
}
