package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface IptvDao {

    // Playlists
    @Query("SELECT * FROM playlists ORDER BY createdTime DESC")
    fun getAllPlaylists(): Flow<List<Playlist>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist): Long

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Query("SELECT * FROM playlists WHERE id = :playlistId LIMIT 1")
    suspend fun getPlaylistById(playlistId: Long): Playlist?

    // Stream Items
    @Query("SELECT * FROM stream_items WHERE playlistId = :playlistId AND categoryType = :categoryType")
    fun getStreamsByType(playlistId: Long, categoryType: String): Flow<List<StreamItem>>

    @Query("SELECT DISTINCT categoryName FROM stream_items WHERE playlistId = :playlistId AND categoryType = :categoryType")
    fun getCategoriesByType(playlistId: Long, categoryType: String): Flow<List<String>>

    @Query("SELECT * FROM stream_items WHERE playlistId = :playlistId AND categoryType = :categoryType AND categoryName = :categoryName")
    fun getStreamsByCategory(playlistId: Long, categoryType: String, categoryName: String): Flow<List<StreamItem>>

    @Query("SELECT * FROM stream_items WHERE playlistId = :playlistId AND isFavorite = 1")
    fun getFavoriteStreams(playlistId: Long): Flow<List<StreamItem>>

    @Query("SELECT * FROM stream_items WHERE playlistId = :playlistId AND name LIKE :query")
    fun searchStreams(playlistId: Long, query: String): Flow<List<StreamItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStreams(streams: List<StreamItem>)

    @Query("UPDATE stream_items SET isFavorite = :isFav WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFav: Boolean)

    @Query("UPDATE stream_items SET isLocked = :isLock WHERE id = :id")
    suspend fun updateLocked(id: Long, isLock: Boolean)

    @Query("DELETE FROM stream_items WHERE playlistId = :playlistId")
    suspend fun deleteStreamsByPlaylist(playlistId: Long)

    @Query("SELECT * FROM stream_items WHERE playlistId = :playlistId")
    suspend fun getStreamsByPlaylistDirect(playlistId: Long): List<StreamItem>

    // EPG Guide
    @Query("SELECT * FROM epg_programs WHERE playlistId = :playlistId AND channelId = :channelId AND endTime > :now ORDER BY startTime ASC")
    fun getEPGForChannel(playlistId: Long, channelId: String, now: Long): Flow<List<EpgProgram>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEpgPrograms(epg: List<EpgProgram>)

    @Query("DELETE FROM epg_programs WHERE playlistId = :playlistId")
    suspend fun deleteEpgByPlaylist(playlistId: Long)

    // Parental Pin
    @Query("SELECT * FROM parental_config WHERE id = 1")
    fun getParentalConfig(): Flow<ParentalConfig?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveParentalConfig(config: ParentalConfig)
}
