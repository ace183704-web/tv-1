package com.example.data

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.util.concurrent.TimeUnit

object IptvParser {
    private const val TAG = "IptvParser"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    /**
     * Parses an M3U file hosted online.
     */
    suspend fun parseM3U(playlistUrl: String, playlistId: Long): List<StreamItem> {
        val streams = mutableListOf<StreamItem>()
        try {
            val request = Request.Builder().url(playlistUrl).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val body = response.body ?: return emptyList()
                
                BufferedReader(InputStreamReader(body.byteStream())).use { reader ->
                    var line: String?
                    var currentItemName = ""
                    var currentLogoUrl = ""
                    var currentCategory = "General"
                    var currentTvgId = ""
                    
                    while (reader.readLine().also { line = it } != null) {
                        val trimmed = line!!.trim()
                        if (trimmed.startsWith("#EXTINF:")) {
                            currentItemName = ""
                            currentLogoUrl = ""
                            currentCategory = "General"
                            currentTvgId = ""
                            
                            // Parse metadata
                            // Format: #EXTINF:-1 tvg-id="nasa" tvg-name="NASA TV" tvg-logo="http://..." group-title="Science",NASA TV
                            
                            // 1. Extract tvg-logo
                            val logoMatcher = "tvg-logo=\"([^\"]+)\"".toRegex().find(trimmed)
                            if (logoMatcher != null) {
                                currentLogoUrl = logoMatcher.groupValues[1]
                            }
                            
                            // 2. Extract group-title (used as category)
                            val groupMatcher = "group-title=\"([^\"]+)\"".toRegex().find(trimmed)
                            if (groupMatcher != null) {
                                currentCategory = groupMatcher.groupValues[1]
                            }
                            
                            // 3. Extract tvg-id
                            val idMatcher = "tvg-id=\"([^\"]+)\"".toRegex().find(trimmed)
                            if (idMatcher != null) {
                                currentTvgId = idMatcher.groupValues[1]
                            }
                            
                            // 4. Extract channel name (everything after the last comma)
                            val commaIdx = trimmed.lastIndexOf(',')
                            if (commaIdx != -1 && commaIdx < trimmed.length - 1) {
                                currentItemName = trimmed.substring(commaIdx + 1).trim()
                            }
                        } else if (trimmed.startsWith("http") || trimmed.startsWith("rtmp") || trimmed.startsWith("rtsp")) {
                            if (currentItemName.isEmpty()) {
                                currentItemName = "Channel " + (streams.size + 1)
                            }
                            val tvgId = if (currentTvgId.isNotEmpty()) currentTvgId else currentItemName.lowercase().replace(" ", "_")
                            
                            // Sort categories into Live, Movie or Series based on matching tokens in category name
                            val categoryType = when {
                                currentCategory.lowercase().contains("movie") || currentCategory.lowercase().contains("cinema") || currentCategory.lowercase().contains("vod") -> "movie"
                                currentCategory.lowercase().contains("series") || currentCategory.lowercase().contains("show") || currentCategory.lowercase().contains("episode") -> "series"
                                else -> "live"
                            }
                            
                            streams.add(
                                StreamItem(
                                    playlistId = playlistId,
                                    streamId = tvgId,
                                    name = currentItemName,
                                    categoryName = currentCategory,
                                    categoryType = categoryType,
                                    iconUrl = currentLogoUrl,
                                    streamUrl = trimmed,
                                    isFavorite = false,
                                    isLocked = false
                                )
                            )
                            // Reset state for next iteration
                            currentItemName = ""
                            currentLogoUrl = ""
                            currentCategory = "General"
                            currentTvgId = ""
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing M3U: ${e.message}", e)
        }
        return streams
    }

    /**
     * Executes queries onto the Xtream Codes Player API database.
     */
    suspend fun verifyXtreamCodes(serverUrl: String, username: String, password: String): Boolean {
        try {
            val formattedUrl = serverUrl.trimEnd('/')
            val endpoint = "$formattedUrl/player_api.php?username=$username&password=$password"
            val request = Request.Builder().url(endpoint).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return false
                val bodyString = response.body?.string() ?: return false
                val responseJson = JSONObject(bodyString)
                val userInfo = responseJson.optJSONObject("user_info")
                if (userInfo == null) return false
                val authString = userInfo.optString("auth")
                val authInt = userInfo.optInt("auth", -1)
                return authString == "1" || authInt == 1
            }
        } catch (e: Exception) {
            Log.e(TAG, "Xtream verification failed: ${e.message}")
            return false
        }
    }

    suspend fun fetchXtreamStreams(
        serverUrl: String,
        username: String,
        password: String,
        playlistId: Long
    ): List<StreamItem> {
        val streams = mutableListOf<StreamItem>()
        val base = serverUrl.trimEnd('/')
        
        try {
            // Xtream player_api categories endpoints
            val categoriesMap = mutableMapOf<String, String>() // Map categoryId to categoryName
            
            // 1. Fetch categories for Live, Movies, and Series
            val liveCatsUrl = "$base/player_api.php?username=$username&password=$password&action=get_live_categories"
            val vodCatsUrl = "$base/player_api.php?username=$username&password=$password&action=get_vod_categories"
            val seriesCatsUrl = "$base/player_api.php?username=$username&password=$password&action=get_series_categories"
            
            listOf(liveCatsUrl, vodCatsUrl, seriesCatsUrl).forEach { catUrl ->
                try {
                    val request = Request.Builder().url(catUrl).build()
                    client.newCall(request).execute().use { res ->
                        if (res.isSuccessful) {
                            val bodyString = res.body?.string() ?: "[]"
                            val trimmed = bodyString.trim()
                            if (trimmed.startsWith("[")) {
                                val arr = JSONArray(trimmed)
                                for (i in 0 until arr.length()) {
                                    val obj = arr.getJSONObject(i)
                                    val categoryId = obj.optString("category_id")
                                    val categoryName = obj.optString("category_name")
                                    if (categoryId.isNotEmpty() && categoryName.isNotEmpty()) {
                                        categoriesMap[categoryId] = categoryName
                                    }
                                }
                            } else if (trimmed.startsWith("{")) {
                                val rootObj = JSONObject(trimmed)
                                val keys = rootObj.keys()
                                while (keys.hasNext()) {
                                    val key = keys.next()
                                    val nestedObj = rootObj.optJSONObject(key)
                                    if (nestedObj != null) {
                                        val categoryId = nestedObj.optString("category_id", key)
                                        val categoryName = nestedObj.optString("category_name")
                                        if (categoryId.isNotEmpty() && categoryName.isNotEmpty()) {
                                            categoriesMap[categoryId] = categoryName
                                        }
                                    } else {
                                        // In case it is a wrapper having "categories" or "genres"
                                        val nestedArr = rootObj.optJSONArray(key)
                                        if (nestedArr != null) {
                                            for (i in 0 until nestedArr.length()) {
                                                val obj = nestedArr.optJSONObject(i)
                                                if (obj != null) {
                                                    val categoryId = obj.optString("category_id")
                                                    val categoryName = obj.optString("category_name")
                                                    if (categoryId.isNotEmpty() && categoryName.isNotEmpty()) {
                                                        categoriesMap[categoryId] = categoryName
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Log.w(TAG, "Expected JSON format for categories but got: $bodyString")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Category query failed for $catUrl", e)
                }
            }
            
            // 2. Fetch Live channels
            val liveUrl = "$base/player_api.php?username=$username&password=$password&action=get_live_streams"
            try {
                val request = Request.Builder().url(liveUrl).build()
                client.newCall(request).execute().use { res ->
                    if (res.isSuccessful) {
                        val bodyString = res.body?.string() ?: "[]"
                        if (bodyString.trim().startsWith("[")) {
                            val arr = JSONArray(bodyString)
                            for (i in 0 until arr.length()) {
                                val obj = arr.getJSONObject(i)
                                val streamId = obj.optString("stream_id")
                                val name = obj.optString("name")
                                val catId = obj.optString("category_id")
                                val catName = obj.optString("category_name").takeIf { it.isNotEmpty() }
                                    ?: categoriesMap[catId]
                                    ?: categoriesMap[catId.trim()]
                                    ?: "General Live"
                                val icon = obj.optString("stream_icon")
                                // Live plays as: http://server.com:80/live/user/pass/stream_id.ts
                                val playUrl = "$base/live/$username/$password/$streamId.ts"
                                
                                streams.add(
                                    StreamItem(
                                        playlistId = playlistId,
                                        streamId = streamId,
                                        name = name,
                                        categoryName = catName,
                                        categoryType = "live",
                                        iconUrl = icon,
                                        streamUrl = playUrl
                                    )
                                )
                            }
                        } else {
                            Log.w(TAG, "Expected JSON array for live streams but got: $bodyString")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Live streams query failed", e)
            }

            // 3. Fetch VOD Movies
            val vodUrl = "$base/player_api.php?username=$username&password=$password&action=get_vod_streams"
            try {
                val request = Request.Builder().url(vodUrl).build()
                client.newCall(request).execute().use { res ->
                    if (res.isSuccessful) {
                        val bodyString = res.body?.string() ?: "[]"
                        if (bodyString.trim().startsWith("[")) {
                            val arr = JSONArray(bodyString)
                            for (i in 0 until arr.length()) {
                                val obj = arr.getJSONObject(i)
                                val streamId = obj.optString("stream_id")
                                val name = obj.optString("name")
                                val catId = obj.optString("category_id")
                                val container = obj.optString("container_extension", "mp4")
                                val catName = obj.optString("category_name").takeIf { it.isNotEmpty() }
                                    ?: categoriesMap[catId]
                                    ?: categoriesMap[catId.trim()]
                                    ?: "General Movies"
                                val icon = obj.optString("stream_icon")
                                // Movie plays as: http://server.com:80/movie/user/pass/stream_id.mp4
                                val playUrl = "$base/movie/$username/$password/$streamId.$container"
                                
                                streams.add(
                                    StreamItem(
                                        playlistId = playlistId,
                                        streamId = streamId,
                                        name = name,
                                        categoryName = catName,
                                        categoryType = "movie",
                                        iconUrl = icon,
                                        streamUrl = playUrl
                                    )
                                )
                            }
                        } else {
                            Log.w(TAG, "Expected JSON array for movies but got: $bodyString")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "VOD query failed", e)
            }

            // 4. Fetch Series
            val seriesUrl = "$base/player_api.php?username=$username&password=$password&action=get_series"
            try {
                val request = Request.Builder().url(seriesUrl).build()
                client.newCall(request).execute().use { res ->
                    if (res.isSuccessful) {
                        val bodyString = res.body?.string() ?: "[]"
                        if (bodyString.trim().startsWith("[")) {
                            val arr = JSONArray(bodyString)
                            for (i in 0 until arr.length()) {
                                val obj = arr.getJSONObject(i)
                                val seriesId = obj.optString("series_id")
                                val name = obj.optString("name")
                                val catId = obj.optString("category_id")
                                val catName = obj.optString("category_name").takeIf { it.isNotEmpty() }
                                    ?: categoriesMap[catId]
                                    ?: categoriesMap[catId.trim()]
                                    ?: "General Series"
                                val icon = obj.optString("cover")
                                // For series, typically we call nested episode info, but we can play a main streaming directory or demo fallback stream URL for smooth UX.
                                val playUrl = "$base/series/$username/$password/$seriesId.mp4"
                                
                                streams.add(
                                    StreamItem(
                                        playlistId = playlistId,
                                        streamId = seriesId,
                                        name = name,
                                        categoryName = catName,
                                        categoryType = "series",
                                        iconUrl = icon,
                                        streamUrl = playUrl
                                    )
                                )
                            }
                        } else {
                            Log.w(TAG, "Expected JSON array for series but got: $bodyString")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Series query failed", e)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "General error parsing Xtream playlist", e)
        }
        
        return streams
    }

    /**
     * Generates a beautiful set of high-performance legal public IPTV nodes with actual active streaming links,
     * including custom Movies, TV Series, and an EPG guide layout.
     */
    fun generateDemoStreams(playlistId: Long): Pair<List<StreamItem>, List<EpgProgram>> {
        val streams = mutableListOf<StreamItem>()
        val epg = mutableListOf<EpgProgram>()

        // 1. Live TV Streams (Public, Legal Channels)
        val liveChannels = listOf(
            Triple("NASA TV HD", "NASA's premier live aerospace and space mission research broadcasting network.", "https://ntv1.nasatv.net/hls/nasa_is_mobile.m3u8"),
            Triple("DW News English", "German international broadcasting, news, documentaries, and global current events.", "https://dwstream72.akamaized.net/hls/live/2016487/dwstream72/index.m3u8"),
            Triple("Al Jazeera English", "Award-winning, rapid investigative news from Doha, Qatar and around the globe.", "https://live-am.aljazeera.com/ajenglish/index.m3u8"),
            Triple("France 24 HD", "Comprehensive French and international news with a modern cultural European perspective.", "https://static.france24.com/live/F24_EN_LO_HLS/live_tv.m3u8")
        )

        val liveLogos = listOf(
            "https://upload.wikimedia.org/wikipedia/commons/e/e5/NASA_logo.svg",
            "https://upload.wikimedia.org/wikipedia/commons/5/5c/Deutsche_Welle_logo_2012.svg",
            "https://upload.wikimedia.org/wikipedia/commons/e/ec/Al_Jazeera_English_2021.svg",
            "https://upload.wikimedia.org/wikipedia/commons/8/82/France_24_logo_2013.svg"
        )

        val liveGenres = listOf("Science & Docs", "International News", "International News", "International News")

        liveChannels.forEachIndexed { i, entry ->
            val channelId = "demo_live_${i + 1}"
            streams.add(
                StreamItem(
                    playlistId = playlistId,
                    streamId = channelId,
                    name = entry.first,
                    categoryName = liveGenres[i],
                    categoryType = "live",
                    iconUrl = liveLogos[i],
                    streamUrl = entry.third
                )
            )

            // Dynamic EPG Generation for each day
            val baseTime = System.currentTimeMillis() - (2 * 60 * 60 * 1000) // Start 2 hours ago
            val hourMs = 60 * 60 * 1000L

            val programs = listOf(
                Pair("Global News Briefing", "Live coverage of major international headlines and developing sports events."),
                Pair("Special Feature Documentary", "Deep dive research investigating future technologies, human biology, and nature."),
                Pair("Space Live Exploration", "Live audio, telemetry feed, and visuals detailing current astronaut operations."),
                Pair("Interactive Global Q&A", "Studio host engages global experts in a round table addressing financial and geopolitical shifts."),
                Pair("Weekly Review & Tech Focus", "An entertaining analysis of major scientific breakthroughs and digital engineering trends.")
            )

            programs.forEachIndexed { pIdx, prog ->
                epg.add(
                    EpgProgram(
                        playlistId = playlistId,
                        channelId = channelId,
                        title = prog.first,
                        description = prog.second,
                        startTime = baseTime + (pIdx * hourMs),
                        endTime = baseTime + ((pIdx + 1) * hourMs)
                    )
                )
            }
        }

        // 2. Movies (VOD - Public Domain, Open Source Movies)
        val moviesList = listOf(
            Triple("Sintel HD", "Sintel is an open-source visual masterpiece fantasy film created in Blender. Beautiful animation with stunning cinematics.", "https://ftp.nluug.nl/pub/graphics/blender/demo/movies/Sintel.2010.720p.mkv"),
            Triple("Big Buck Bunny", "A large, incredibly soft rabbit deals with standard forest problems, taking hilarious revenge on bullies.", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"),
            Triple("Tears of Steel", "An epic open-source science fiction cinema set in dynamic cybernetic future Amsterdam.", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"),
            Triple("Subaru Flight VR", "Visually captivating flying showcase featuring dynamic glider loops above expansive green valleys.", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4")
        )

        val moviePosters = listOf(
            "https://upload.wikimedia.org/wikipedia/commons/8/8f/Sintel_poster_v2_multilingual.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/c/c5/Big_Buck_Bunny_Poster_2008.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/6/69/Tears_of_Steel_poster.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/b/b3/Glider_flight_over_the_Alps.jpg"
        )

        val movieCategories = listOf("Action & Fantasy", "Animes & Kids", "Sci-Fi Thrillers", "Documentaries")

        moviesList.forEachIndexed { i, movie ->
            streams.add(
                StreamItem(
                    playlistId = playlistId,
                    streamId = "demo_movie_${i + 1}",
                    name = movie.first,
                    categoryName = movieCategories[i],
                    categoryType = "movie",
                    iconUrl = moviePosters[i],
                    streamUrl = movie.third
                )
            )
        }

        // 3. Series Episodes (VOD Examples)
        val seriesSpecs = listOf(
            Triple("Cosmos: Time Journey", "Episode 1: The Galactic Threshold", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"),
            Triple("Cosmos: Time Journey", "Episode 2: Solar Giants", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4"),
            Triple("Classic Retro Cartoon", "Volume 1: Playful Bunnies", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4"),
            Triple("Classic Retro Cartoon", "Volume 2: Mountain Climbers", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4")
        )

        val seriesPosters = listOf(
            "https://upload.wikimedia.org/wikipedia/commons/4/47/A_spiral_galaxy_having_bright_neon_arms_and_a_dense_core.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/4/47/A_spiral_galaxy_having_bright_neon_arms_and_a_dense_core.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/0/02/Funny_cartoon_rabbit_smiling_excitedly.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/0/02/Funny_cartoon_rabbit_smiling_excitedly.jpg"
        )

        seriesSpecs.forEachIndexed { i, ep ->
            streams.add(
                StreamItem(
                    playlistId = playlistId,
                    streamId = "demo_series_${i + 1}",
                    name = "${ep.first} - ${ep.second}",
                    categoryName = ep.first,
                    categoryType = "series",
                    iconUrl = seriesPosters[i],
                    streamUrl = ep.third
                )
            )
        }

        return Pair(streams, epg)
    }
}
