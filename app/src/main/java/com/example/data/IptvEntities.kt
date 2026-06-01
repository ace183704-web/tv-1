package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val url: String = "",
    val username: String = "",
    val password: String = "",
    val type: String = "XTREAM", // "M3U" or "XTREAM"
    val createdTime: Long = System.currentTimeMillis()
)

@Entity(tableName = "stream_items")
data class StreamItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playlistId: Long,
    val streamId: String, // Stream identifier inside the playlist/API
    val name: String,
    val categoryName: String, // e.g. "Action", "News"
    val categoryType: String, // "live", "movie", "series"
    val iconUrl: String,
    val streamUrl: String, // Playback stream URL
    val isFavorite: Boolean = false,
    val isLocked: Boolean = false // Parental control lock status
)

@Entity(tableName = "epg_programs")
data class EpgProgram(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playlistId: Long,
    val channelId: String, // Matches the streamId or M3U's tvg-id
    val title: String,
    val description: String,
    val startTime: Long, // Epoch millis
    val endTime: Long // Epoch millis
)

@Entity(tableName = "parental_config")
data class ParentalConfig(
    @PrimaryKey val id: Int = 1, // Single row entry
    val pin: String = "0000",
    val isLockActive: Boolean = false
)
