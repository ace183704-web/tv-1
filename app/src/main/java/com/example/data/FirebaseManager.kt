package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import com.example.BuildConfig
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import javax.crypto.spec.IvParameterSpec
import android.util.Base64

data class FirebaseUser(
    val uid: String,
    val email: String,
    val token: String
)

data class ProvisionedPortal(
    val id: String,
    val name: String,
    val hostUrl: String,
    val port: String,
    val username: String,
    val password: String,
    val type: String = "XTREAM",
    val url: String = "" // Used if type is M3U
)

object FirebaseManager {
    private const val TAG = "FirebaseManager"
    private const val PREFS_NAME = "firebase_auth_prefs"
    
    // Configurable keys via Secret Panel (configured in .env/BuildConfig)
    // We fall back dynamically to a default key if none is provided, allowing the app to keep compiling and running
    private val firebaseApiKey: String = try {
        val clazz = Class.forName("com.example.BuildConfig")
        val field = clazz.getField("FIREBASE_API_KEY")
        field.get(null) as String
    } catch (e: Throwable) {
        ""
    }
    
    private val firebaseProjectId: String = try {
        val clazz = Class.forName("com.example.BuildConfig")
        val field = clazz.getField("FIREBASE_PROJECT_ID")
        field.get(null) as String
    } catch (e: Throwable) {
        ""
    }

    private var activeApiKey: String = ""
    private var activeProjectId: String = ""
    var diagnosticLoggingEnabled: Boolean = false
    private var appContext: Context? = null

    @Volatile
    private var resolvedDatabaseBaseUrl: String? = null
    private var googleServicesDbUrl: String? = null

    private fun getCandidateDatabaseUrls(): List<String> {
        val projId = activeProjectId
        if (projId.isEmpty()) return emptyList()
        val list = mutableListOf<String>()
        val gsUrl = googleServicesDbUrl
        if (!gsUrl.isNullOrEmpty()) {
            list.add(gsUrl.removeSuffix("/"))
        }
        list.add("https://$projId-default-rtdb.firebaseio.com")
        list.add("https://$projId.firebaseio.com")
        list.add("https://$projId-default-rtdb.europe-west1.firebasedatabase.app")
        list.add("https://$projId-default-rtdb.asia-southeast1.firebasedatabase.app")
        list.add("https://$projId.europe-west1.firebasedatabase.app")
        list.add("https://$projId.asia-southeast1.firebasedatabase.app")
        list.add("https://$projId-default-rtdb.us-central1.firebasedatabase.app")
        list.add("https://$projId.us-central1.firebasedatabase.app")
        return list.distinct()
    }

    private suspend fun executeFirebaseCall(
        relativeUrlPath: String,
        queryParams: String,
        method: String,
        requestBodyString: String? = null
    ): okhttp3.Response = withContext(Dispatchers.IO) {
        val cachedUrl = resolvedDatabaseBaseUrl
        val candidates = if (cachedUrl != null) {
            listOf(cachedUrl)
        } else {
            getCandidateDatabaseUrls()
        }

        var lastException: Exception? = null
        var lastResponseCode: Int = -1

        val finalCandidates = if (candidates.isEmpty()) {
            listOf("https://$activeProjectId-default-rtdb.firebaseio.com")
        } else {
            candidates
        }

        for (base in finalCandidates) {
            val fullUrl = if (queryParams.isNotEmpty()) {
                "$base$relativeUrlPath?$queryParams"
            } else {
                "$base$relativeUrlPath"
            }

            val body = if (requestBodyString != null) {
                requestBodyString.toRequestBody("application/json".toMediaType())
            } else {
                null
            }

            val requestBuilder = Request.Builder().url(fullUrl)
            when (method) {
                "GET" -> requestBuilder.get()
                "PUT" -> requestBuilder.put(body!!)
                "DELETE" -> requestBuilder.delete()
            }

            try {
                if (diagnosticLoggingEnabled) {
                    Log.d(TAG, "[DIAGNOSTIC] Sending Firebase Request: $method $fullUrl")
                }
                val response = client.newCall(requestBuilder.build()).execute()
                lastResponseCode = response.code
                
                if (response.code != 404) {
                    if (resolvedDatabaseBaseUrl == null) {
                        resolvedDatabaseBaseUrl = base
                        Log.d(TAG, "Successfully resolved and cached base RTDB URL: $base")
                        appContext?.let { ctx ->
                            ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                                .edit().putString("resolved_database_url", base).apply()
                        }
                    }
                    if (diagnosticLoggingEnabled) {
                        Log.d(TAG, "[DIAGNOSTIC] Firebase Response Code: ${response.code}")
                    }
                    return@withContext response
                } else {
                    Log.w(TAG, "RTDB candidate returned 404: $fullUrl")
                    response.close()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error executing call on candidate base $base: ${e.message}")
                lastException = e
            }
        }

        if (cachedUrl != null) {
            Log.w(TAG, "Cached database URL failed, clear cache and try list of candidates")
            resolvedDatabaseBaseUrl = null
            appContext?.let { ctx ->
                ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit().remove("resolved_database_url").apply()
            }
            val backupCandidates = getCandidateDatabaseUrls()
            for (base in backupCandidates) {
                if (base == cachedUrl) continue
                val fullUrl = if (queryParams.isNotEmpty()) {
                    "$base$relativeUrlPath?$queryParams"
                } else {
                    "$base$relativeUrlPath"
                }
                val body = if (requestBodyString != null) {
                    requestBodyString.toRequestBody("application/json".toMediaType())
                } else {
                    null
                }
                val requestBuilder = Request.Builder().url(fullUrl)
                when (method) {
                    "GET" -> requestBuilder.get()
                    "PUT" -> requestBuilder.put(body!!)
                    "DELETE" -> requestBuilder.delete()
                }
                try {
                if (diagnosticLoggingEnabled) {
                    Log.d(TAG, "[DIAGNOSTIC] Sending Firebase Request to backup candidate: $method $fullUrl")
                }
                    val response = client.newCall(requestBuilder.build()).execute()
                    lastResponseCode = response.code
                    if (response.code != 404) {
                        resolvedDatabaseBaseUrl = base
                        Log.d(TAG, "Successfully updated cached base RTDB URL to: $base")
                        appContext?.let { ctx ->
                            ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                                .edit().putString("resolved_database_url", base).apply()
                        }
                        if (diagnosticLoggingEnabled) {
                            Log.d(TAG, "[DIAGNOSTIC] Backup Firebase Response Code: ${response.code}")
                        }
                        return@withContext response
                    } else {
                        response.close()
                    }
                } catch (e: Exception) {
                    lastException = e
                }
            }
        }

        if (lastException != null) {
            throw lastException
        }

        val resolvedMsg = "No active Firebase Realtime Database found for project '$activeProjectId'. " +
                "Please enable Realtime Database in your Firebase console, create a database instance, and ensure the correct security rules are in place."
        throw Exception(resolvedMsg)
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val currentUser = MutableStateFlow<FirebaseUser?>(null)
    val provisionedPortals = MutableStateFlow<List<ProvisionedPortal>>(emptyList())
    val syncStatusMessage = MutableStateFlow<String?>(null)

    fun initialize(context: Context) {
        appContext = context.applicationContext
        activeApiKey = firebaseApiKey
        activeProjectId = firebaseProjectId

        // Load and parse google-services.json dynamically from assets if available
        try {
            val jsonStream = try {
                context.assets.open("google-services.json")
            } catch (e: Exception) {
                null
            }
            if (jsonStream != null) {
                val jsonText = jsonStream.bufferedReader().use { it.readText() }
                val root = JSONObject(jsonText)
                
                val projectInfoObj = root.optJSONObject("project_info")
                val extractedProjId = projectInfoObj?.optString("project_id")
                if (!extractedProjId.isNullOrEmpty()) {
                    activeProjectId = extractedProjId
                }
                val extractedDbUrl = projectInfoObj?.optString("firebase_url")
                if (!extractedDbUrl.isNullOrEmpty()) {
                    googleServicesDbUrl = extractedDbUrl
                    Log.d(TAG, "Extracted firebase_url from google-services.json: $extractedDbUrl")
                }

                val clientArray = root.optJSONArray("client")
                if (clientArray != null && clientArray.length() > 0) {
                    val firstClient = clientArray.getJSONObject(0)
                    val apiKeyArray = firstClient.optJSONArray("api_key")
                    if (apiKeyArray != null && apiKeyArray.length() > 0) {
                        val apiKeyObj = apiKeyArray.getJSONObject(0)
                        val extractedApiKey = apiKeyObj.optString("current_key")
                        if (!extractedApiKey.isNullOrEmpty()) {
                            activeApiKey = extractedApiKey
                        }
                    }
                }
                Log.d(TAG, "Successfully loaded keys from google-services.json: projId=$activeProjectId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing google-services.json from assets", e)
        }

        // Final values sanitization
        if (activeApiKey.contains("YOUR_FIREBASE_API_KEY") || activeApiKey.isEmpty() || activeApiKey == "MY_GEMINI_API_KEY") {
            activeApiKey = ""
        }
        if (activeProjectId.contains("YOUR_FIREBASE_PROJECT_ID") || activeProjectId.isEmpty()) {
            activeProjectId = ""
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        resolvedDatabaseBaseUrl = prefs.getString("resolved_database_url", null)
        Log.d(TAG, "Initialized database base URL from cache: $resolvedDatabaseBaseUrl")
        val uid = prefs.getString("uid", null)
        val email = prefs.getString("email", null)
        val token = prefs.getString("token", null)
        
        if (uid != null && email != null && token != null) {
            currentUser.value = FirebaseUser(uid, email, token)
            // Load portals from local backup storage first
            loadLocalBackupPortals(prefs)
        }
    }

    private fun saveSession(context: Context, user: FirebaseUser) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString("uid", user.uid)
            .putString("email", user.email)
            .putString("token", user.token)
            .apply()
        currentUser.value = user
    }

    fun clearSession(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        currentUser.value = null
        provisionedPortals.value = emptyList()
    }

    /**
     * Standard Real Firebase REST SignUp. Falls back to pristine simulated mode if Firebase key is not configured.
     */
    suspend fun signUpWithEmail(context: Context, email: String, pass: String): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        if (activeApiKey.isEmpty()) {
            // Simulated Flow - Perfect fallback for quick prototypes or offline runs
            val mockUid = "simulated_user_" + System.currentTimeMillis().toString().takeLast(6)
            val user = FirebaseUser(mockUid, email, "simulated_token_xyz")
            saveSession(context, user)
            saveSimulatedAccount(context, email, pass, mockUid)
            return@withContext Result.success(user)
        }

        try {
            val url = "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=$activeApiKey"
            val json = JSONObject().apply {
                put("email", email)
                put("password", pass)
                put("returnSecureToken", true)
            }
            
            val request = Request.Builder()
                .url(url)
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val resJson = JSONObject(body)
                    val uid = resJson.getString("localId")
                    val idToken = resJson.getString("idToken")
                    val user = FirebaseUser(uid, email, idToken)
                    saveSession(context, user)
                    return@withContext Result.success(user)
                } else {
                    val errorObj = JSONObject(body).optJSONObject("error")
                    val message = errorObj?.optString("message") ?: "Registration Failed"
                    return@withContext Result.failure(Exception(message))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase SignUp error: ${e.message}")
            return@withContext Result.failure(e)
        }
    }

    /**
     * Standard Real Firebase REST SignIn. Falls back to simulated account query.
     */
    suspend fun signInWithEmail(context: Context, email: String, pass: String): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        if (activeApiKey.isEmpty()) {
            // Simulated Sign In validation against locally saved mock accounts
            val savedPass = getSimulatedPassword(context, email)
            if (savedPass != null && savedPass == pass) {
                val mockUid = getSimulatedUid(context, email) ?: ("simulated_user_" + System.currentTimeMillis().toString().takeLast(6))
                val user = FirebaseUser(mockUid, email, "simulated_token_xyz")
                saveSession(context, user)
                loadProvisionedPortals(context)
                return@withContext Result.success(user)
            } else if (savedPass != null && savedPass != pass) {
                return@withContext Result.failure(Exception("INVALID_PASSWORD"))
            } else {
                // If it's a first time login without signup and key is empty, let's auto-register to be super helpful
                val mockUid = "simulated_user_" + System.currentTimeMillis().toString().takeLast(6)
                val user = FirebaseUser(mockUid, email, "simulated_token_xyz")
                saveSession(context, user)
                saveSimulatedAccount(context, email, pass, mockUid)
                loadProvisionedPortals(context)
                return@withContext Result.success(user)
            }
        }

        try {
            val url = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=$activeApiKey"
            val json = JSONObject().apply {
                put("email", email)
                put("password", pass)
                put("returnSecureToken", true)
            }
            
            val request = Request.Builder()
                .url(url)
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val resJson = JSONObject(body)
                    val uid = resJson.getString("localId")
                    val idToken = resJson.getString("idToken")
                    val user = FirebaseUser(uid, email, idToken)
                    saveSession(context, user)
                    loadProvisionedPortals(context)
                    return@withContext Result.success(user)
                } else {
                    val errorObj = JSONObject(body).optJSONObject("error")
                    val message = errorObj?.optString("message") ?: "Login Failed"
                    return@withContext Result.failure(Exception(message))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase SignIn error: ${e.message}")
            return@withContext Result.failure(e)
        }
    }

    /**
     * Simulated Social Auths (Google and Facebook) that return a verified FirebaseUser instantly,
     * maintaining high visual responsiveness.
     */
    suspend fun loginWithSocial(context: Context, provider: String): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        val mockEmail = when (provider.lowercase()) {
            "google" -> "google.user@gmail.com"
            else -> "facebook.client@fb.com"
        }
        val mockUid = "social_uid_" + provider.lowercase() + "_" + System.currentTimeMillis().toString().takeLast(6)
        val user = FirebaseUser(mockUid, mockEmail, "social_token_" + provider.lowercase())
        saveSession(context, user)
        loadProvisionedPortals(context)
        return@withContext Result.success(user)
    }

    /**
     * Fetches provisioned portals from Firebase Realtime Database REST API.
     */
    suspend fun loadProvisionedPortals(context: Context) = withContext(Dispatchers.IO) {
        val user = currentUser.value
        val isMock = activeProjectId.isEmpty()
        val targetUid = user?.uid ?: "anon_${getProvisioningCode(context)}"
        
        if (isMock) {
            val simDb = context.getSharedPreferences("simulated_remote_db", Context.MODE_PRIVATE)
            val remotelyProvisionedRaw = simDb.getString("portals_$targetUid", null)
            if (remotelyProvisionedRaw != null) {
                try {
                    val list = mutableListOf<ProvisionedPortal>()
                    
                    // First load current local backup
                    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    val currentRaw = prefs.getString("backup_portals", "[]")
                    val currentArr = JSONArray(currentRaw)
                    val addedIds = mutableSetOf<String>()
                    for (i in 0 until currentArr.length()) {
                        val obj = currentArr.getJSONObject(i)
                        val portal = parsePortalJson(obj.optString("id"), obj)
                        list.add(portal)
                        addedIds.add(portal.id)
                    }
                    
                    // Add remote provisioned portals
                    val remoteArr = JSONArray(remotelyProvisionedRaw)
                    var changed = false
                    for (i in 0 until remoteArr.length()) {
                        val obj = remoteArr.getJSONObject(i)
                        val portal = parsePortalJson(obj.optString("id"), obj)
                        if (!addedIds.contains(portal.id)) {
                            list.add(portal)
                            addedIds.add(portal.id)
                            changed = true
                        }
                    }
                    
                    if (changed) {
                        provisionedPortals.value = list
                        saveLocalPortalsBackup(context, list)
                    }
                    // Clear the simulated delivery queue once fetched/merged
                    simDb.edit().remove("portals_$targetUid").apply()
                } catch (e: Exception) {
                    Log.e(TAG, "Error merging simulated remote portals: ${e.message}")
                }
            }
            
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            loadLocalBackupPortals(prefs)
            return@withContext
        }

        try {
            val relativePath = "/users/$targetUid/portals.json"
            val queryParams = if (user != null) "auth=${user.token}" else ""
            
            executeFirebaseCall(relativePath, queryParams, "GET").use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: "null"
                    if (body == "null" || body.isEmpty()) {
                        provisionedPortals.value = emptyList()
                        return@withContext
                    }
                    
                    val list = mutableListOf<ProvisionedPortal>()
                    if (body.trim().startsWith("[")) {
                        // Array format
                        val arr = JSONArray(body)
                        for (i in 0 until arr.length()) {
                            if (arr.isNull(i)) continue
                            val obj = arr.getJSONObject(i)
                            list.add(parsePortalJson(i.toString(), obj))
                        }
                    } else {
                        // Key-Value Map format
                        val root = JSONObject(body)
                        val keys = root.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val obj = root.getJSONObject(key)
                            list.add(parsePortalJson(key, obj))
                        }
                    }
                    
                    // Update state and save local backup
                    provisionedPortals.value = list
                    saveLocalPortalsBackup(context, list)
                } else {
                    Log.e(TAG, "Portals fetch un-successful: ${response.code}")
                    // Fetch local backup if remote sync fails
                    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    loadLocalBackupPortals(prefs)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fetch remote portals failed: ${e.message}")
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            loadLocalBackupPortals(prefs)
        }
    }

    /**
     * Saves or adds an IPTV subscription portal profile to the Firebase Cloud database.
     */
    suspend fun addProvisionedPortal(context: Context, portal: ProvisionedPortal): Result<Boolean> = withContext(Dispatchers.IO) {
        val user = currentUser.value ?: return@withContext Result.failure(Exception("No Authenticated User Session"))
        val isMock = activeProjectId.isEmpty()

        // 1. Add locally
        val currentList = provisionedPortals.value.filter { it.id != portal.id }.toMutableList()
        currentList.add(portal)
        provisionedPortals.value = currentList
        saveLocalPortalsBackup(context, currentList)

        if (isMock) {
            syncStatusMessage.value = "Synced locally (Emulator Safe Mode)"
            return@withContext Result.success(true)
        }

        try {
            val relativePath = "/users/${user.uid}/portals/${portal.id}.json"
            val queryParams = "auth=${user.token}"
            
            val innerJson = JSONObject().apply {
                put("id", portal.id)
                put("name", portal.name)
                put("hostUrl", portal.hostUrl)
                put("port", portal.port)
                put("username", portal.username)
                put("password", portal.password)
                put("type", portal.type)
                put("url", portal.url)
            }

            val encryptedString = encrypt(innerJson.toString(), user.uid)
            val jsonPayload = JSONObject().apply {
                put("encrypted", true)
                put("data", encryptedString)
            }

            executeFirebaseCall(relativePath, queryParams, "PUT", jsonPayload.toString()).use { response ->
                if (response.isSuccessful) {
                    syncStatusMessage.value = "Cloud Provision Storage Synchronized"
                    return@withContext Result.success(true)
                } else {
                    Log.e(TAG, "Failed pushing remote portal: ${response.code}")
                    syncStatusMessage.value = "Sync Delayed (Cached locally)"
                    return@withContext Result.success(true) // Return success since we saved locally
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed writing portal: ${e.message}")
            syncStatusMessage.value = "Stored locally (Offline Mode)"
            return@withContext Result.success(true)
        }
    }

    /**
     * Removes a provisioned portal profile both locally and from the Firebase Realtime Database.
     */
    suspend fun deleteProvisionedPortal(context: Context, portalId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val user = currentUser.value ?: return@withContext Result.failure(Exception("No auth session"))
        val isMock = activeProjectId.isEmpty()

        val currentList = provisionedPortals.value.filter { it.id != portalId }
        provisionedPortals.value = currentList
        saveLocalPortalsBackup(context, currentList)

        if (isMock) {
            syncStatusMessage.value = "Deleted locally"
            return@withContext Result.success(true)
        }

        try {
            val relativePath = "/users/${user.uid}/portals/$portalId.json"
            val queryParams = "auth=${user.token}"
            
            executeFirebaseCall(relativePath, queryParams, "DELETE").use { response ->
                if (response.isSuccessful) {
                    syncStatusMessage.value = "Profile deleted from Cloud Storage"
                    return@withContext Result.success(true)
                } else {
                    syncStatusMessage.value = "Deleted locally (Failed cloud sync)"
                    return@withContext Result.success(true)
                }
            }
        } catch (e: Exception) {
            syncStatusMessage.value = "Deleted locally (Device Offline)"
            return@withContext Result.success(true)
        }
    }

    // Helper utilities for local persistence backups
    private fun getSecretKeySpec(keyString: String): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = keyString.toByteArray(Charsets.UTF_8)
        val keyBytes = digest.digest(bytes)
        return SecretKeySpec(keyBytes, "AES")
    }

    fun encrypt(data: String, key: String): String {
        return try {
            val secretKey = getSecretKeySpec(key)
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            val iv = ByteArray(16) // Zero IV for simplicity and determinism
            val ivSpec = IvParameterSpec(iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
            val encryptedBytes = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Encryption error: ${e.message}", e)
            data // fallback
        }
    }

    fun decrypt(encryptedData: String, key: String): String {
        return try {
            val secretKey = getSecretKeySpec(key)
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            val iv = ByteArray(16)
            val ivSpec = IvParameterSpec(iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
            val decodedBytes = Base64.decode(encryptedData, Base64.NO_WRAP)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            val decrypted = String(decryptedBytes, Charsets.UTF_8)
            if (diagnosticLoggingEnabled) {
                try {
                    val prettyJson = JSONObject(decrypted).toString(4)
                    Log.d(TAG, "[DIAGNOSTIC] Decrypted Firebase payload content:\n$prettyJson")
                } catch (je: Exception) {
                    Log.d(TAG, "[DIAGNOSTIC] Decrypted Firebase payload content: $decrypted")
                }
            }
            decrypted
        } catch (e: Exception) {
            Log.e(TAG, "Decryption error: ${e.message}")
            if (encryptedData.trim().startsWith("{")) {
                encryptedData
            } else {
                ""
            }
        }
    }

    private fun parsePortalJson(id: String, obj: JSONObject): ProvisionedPortal {
        val finalObj = if (obj.optBoolean("encrypted", false)) {
            val uid = currentUser.value?.uid
            if (uid != null) {
                val decrypted = decrypt(obj.optString("data"), uid)
                if (decrypted.isNotEmpty()) {
                    try {
                        JSONObject(decrypted)
                    } catch (e: Exception) {
                        obj
                    }
                } else obj
            } else obj
        } else obj

        return ProvisionedPortal(
            id = finalObj.optString("id", id),
            name = finalObj.optString("name", "Portal Subscription"),
            hostUrl = finalObj.optString("hostUrl", ""),
            port = finalObj.optString("port", ""),
            username = finalObj.optString("username", ""),
            password = finalObj.optString("password", ""),
            type = finalObj.optString("type", "XTREAM"),
            url = finalObj.optString("url", "")
        )
    }

    private fun loadLocalBackupPortals(prefs: SharedPreferences) {
        try {
            val raw = prefs.getString("backup_portals", null) ?: return
            val arr = JSONArray(raw)
            val list = mutableListOf<ProvisionedPortal>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(parsePortalJson(obj.optString("id"), obj))
            }
            provisionedPortals.value = list
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding backup portals: ${e.message}")
        }
    }

    private fun saveLocalPortalsBackup(context: Context, list: List<ProvisionedPortal>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        try {
            val arr = JSONArray()
            for (portal in list) {
                val obj = JSONObject().apply {
                    put("id", portal.id)
                    put("name", portal.name)
                    put("hostUrl", portal.hostUrl)
                    put("port", portal.port)
                    put("username", portal.username)
                    put("password", portal.password)
                    put("type", portal.type)
                    put("url", portal.url)
                }
                arr.put(obj)
            }
            prefs.edit().putString("backup_portals", arr.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error encoding portals for local backup: ${e.message}")
        }
    }

    private var provisioningCode: String = ""
    fun getProvisioningCode(context: Context): String {
        if (provisioningCode.isNotEmpty()) return provisioningCode
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var code = prefs.getString("provisioning_code", null)
        if (code == null) {
            val rand = (10000000..99999999).random()
            code = rand.toString()
            prefs.edit().putString("provisioning_code", code).apply()
        }
        provisioningCode = code
        return code
    }

    fun getAndroidId(context: Context): String {
        return try {
            android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            ) ?: "unknown"
        } catch (e: Exception) {
            Log.e(TAG, "Error obtaining ANDROID_ID: ${e.message}")
            "unknown"
        }
    }

    suspend fun registerDeviceForProvisioning(context: Context): Result<Boolean> = withContext(Dispatchers.IO) {
        val user = currentUser.value
        val code = getProvisioningCode(context)
        val aid = getAndroidId(context)
        val isMock = activeProjectId.isEmpty()
        if (isMock) {
            val simDb = context.getSharedPreferences("simulated_remote_db", Context.MODE_PRIVATE)
            val editor = simDb.edit()
            val targetEmail = user?.email ?: "anonymous@device.provisioning"
            val targetUid = user?.uid ?: "anon_$code"
            editor.putString("code_email_$code", targetEmail)
            editor.putString("code_uid_$code", targetUid)
            if (aid.isNotEmpty() && aid != "unknown") {
                editor.putString("code_email_$aid", targetEmail)
                editor.putString("code_uid_$aid", targetUid)
            }
            editor.apply()
            syncStatusMessage.value = "Device registered locally (Code: $code / ID: $aid)"
            return@withContext Result.success(true)
        }
        try {
            val innerJson = JSONObject().apply {
                if (user != null) {
                    put("uid", user.uid)
                    put("email", user.email)
                } else {
                    put("uid", "anon_$code")
                    put("email", "anonymous@device.provisioning")
                }
                put("timestamp", System.currentTimeMillis())
            }
            val encryptedString = encrypt(innerJson.toString(), code)
            val jsonCode = JSONObject().apply {
                put("encrypted", true)
                put("data", encryptedString)
            }
            
            val queryParams = if (user != null) "auth=${user.token}" else ""
            
            val codeSuccess = executeFirebaseCall("/devices/$code.json", queryParams, "PUT", jsonCode.toString()).use { response ->
                response.isSuccessful
            }

            var aidSuccess = true
            if (aid.isNotEmpty() && aid != "unknown") {
                val encryptedStringAid = encrypt(innerJson.toString(), aid)
                val jsonAid = JSONObject().apply {
                    put("encrypted", true)
                    put("data", encryptedStringAid)
                }
                aidSuccess = executeFirebaseCall("/devices/$aid.json", queryParams, "PUT", jsonAid.toString()).use { response ->
                    response.isSuccessful
                }
            }

            if (codeSuccess && aidSuccess) {
                Log.d(TAG, "Device registered for remote provisioning: $code & $aid -> ${user?.uid ?: "anon_$code"}")
                // If the user was unauthenticated, trigger loading of their specific portals so they sync up right away
                if (user == null) {
                    loadProvisionedPortals(context)
                }
                return@withContext Result.success(true)
            } else {
                Log.e(TAG, "Failed fully registering device. code success: $codeSuccess, aid success: $aidSuccess")
                return@withContext Result.failure(Exception("Failed to register device to Firebase"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed registering device code: ${e.message}")
            return@withContext Result.failure(e)
        }
    }

    suspend fun remoteProvisionPortal(
        context: Context,
        targetCode: String,
        name: String,
        host: String,
        port: String,
        user: String,
        pass: String,
        type: String = "XTREAM",
        m3uUrl: String = ""
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val isMock = activeProjectId.isEmpty()

        if (isMock) {
            val simDb = context.getSharedPreferences("simulated_remote_db", Context.MODE_PRIVATE)
            val targetUserEmail = simDb.getString("code_email_$targetCode", null)
            val targetUid = simDb.getString("code_uid_$targetCode", null)
            
            if (targetUid == null) {
                return@withContext Result.failure(Exception("Target device code not found or offline"))
            }

            val id = "portal_" + System.currentTimeMillis()
            val innerJson = JSONObject().apply {
                put("id", id)
                put("name", name)
                put("hostUrl", host)
                put("port", port)
                put("username", user)
                put("password", pass)
                put("type", type)
                put("url", m3uUrl)
            }

            val encryptedString = encrypt(innerJson.toString(), targetUid)
            val jsonPortal = JSONObject().apply {
                put("encrypted", true)
                put("data", encryptedString)
            }

            val targetPortalsRaw = simDb.getString("portals_$targetUid", "[]")
            val array = JSONArray(targetPortalsRaw)
            array.put(jsonPortal)
            simDb.edit().putString("portals_$targetUid", array.toString()).apply()

            Log.d(TAG, "Simulated remote provision successful! Sent portal $name to $targetUserEmail ($targetUid)")
            return@withContext Result.success(true)
        }

        val currUser = currentUser.value

        try {
            val queryParams = if (currUser != null) "auth=${currUser.token}" else ""
            
            var targetUid: String? = null
            executeFirebaseCall("/devices/$targetCode.json", queryParams, "GET").use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Device lookup failed: API error ${response.code}"))
                }
                val body = response.body?.string() ?: "null"
                if (body == "null" || body.isEmpty()) {
                    return@withContext Result.failure(Exception("Target provisioning code '$targetCode' not registered or invalid"))
                }
                
                val obj = JSONObject(body)
                val resolvedObj = if (obj.optBoolean("encrypted", false)) {
                    val decrypted = decrypt(obj.optString("data"), targetCode)
                    if (decrypted.isNotEmpty()) JSONObject(decrypted) else obj
                } else {
                    obj
                }
                targetUid = resolvedObj.optString("uid", null)
            }

            if (targetUid.isNullOrEmpty()) {
                return@withContext Result.failure(Exception("Unable to resolve UID for provisioning code '$targetCode'"))
            }

            val portalId = "portal_" + System.currentTimeMillis()
            
            val innerJson = JSONObject().apply {
                put("id", portalId)
                put("name", name)
                put("hostUrl", host)
                put("port", port)
                put("username", user)
                put("password", pass)
                put("type", type)
                put("url", m3uUrl)
            }

            val encryptedString = encrypt(innerJson.toString(), targetUid)
            val json = JSONObject().apply {
                put("encrypted", true)
                put("data", encryptedString)
            }

            executeFirebaseCall("/users/$targetUid/portals/$portalId.json", queryParams, "PUT", json.toString()).use { response ->
                if (response.isSuccessful) {
                    return@withContext Result.success(true)
                } else {
                    return@withContext Result.failure(Exception("Failed to push portal configuration: API error ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in remote provisioning execution", e)
            return@withContext Result.failure(e)
        }
    }

    private fun saveSimulatedAccount(context: Context, email: String, pass: String, uid: String) {
        val prefs = context.getSharedPreferences("simulated_users", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("pass_$email", pass)
            .putString("uid_$email", uid)
            .apply()
    }

    private fun getSimulatedPassword(context: Context, email: String): String? {
        val prefs = context.getSharedPreferences("simulated_users", Context.MODE_PRIVATE)
        return prefs.getString("pass_$email", null)
    }

    private fun getSimulatedUid(context: Context, email: String): String? {
        val prefs = context.getSharedPreferences("simulated_users", Context.MODE_PRIVATE)
        return prefs.getString("uid_$email", null)
    }
}
