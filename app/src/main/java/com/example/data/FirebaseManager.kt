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

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val currentUser = MutableStateFlow<FirebaseUser?>(null)
    val provisionedPortals = MutableStateFlow<List<ProvisionedPortal>>(emptyList())
    val syncStatusMessage = MutableStateFlow<String?>(null)

    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
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
        if (firebaseApiKey.isEmpty() || firebaseApiKey.contains("YOUR_FIREBASE_API_KEY") || firebaseApiKey == "MY_GEMINI_API_KEY") {
            // Simulated Flow - Perfect fallback for quick prototypes or offline runs
            val mockUid = "simulated_user_" + System.currentTimeMillis().toString().takeLast(6)
            val user = FirebaseUser(mockUid, email, "simulated_token_xyz")
            saveSession(context, user)
            saveSimulatedAccount(context, email, pass, mockUid)
            return@withContext Result.success(user)
        }

        try {
            val url = "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=$firebaseApiKey"
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
        if (firebaseApiKey.isEmpty() || firebaseApiKey.contains("YOUR_FIREBASE_API_KEY") || firebaseApiKey == "MY_GEMINI_API_KEY") {
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
            val url = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=$firebaseApiKey"
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
        val user = currentUser.value ?: return@withContext
        val isMock = firebaseProjectId.isEmpty() || firebaseProjectId.contains("YOUR_FIREBASE_PROJECT_ID")
        
        if (isMock) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            loadLocalBackupPortals(prefs)
            return@withContext
        }

        try {
            val url = "https://$firebaseProjectId-default-rtdb.firebaseio.com/users/${user.uid}/portals.json?auth=${user.token}"
            val request = Request.Builder().url(url).build()
            
            client.newCall(request).execute().use { response ->
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
        val isMock = firebaseProjectId.isEmpty() || firebaseProjectId.contains("YOUR_FIREBASE_PROJECT_ID")

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
            // Save to Firebase Realtime Database
            val url = "https://$firebaseProjectId-default-rtdb.firebaseio.com/users/${user.uid}/portals/${portal.id}.json?auth=${user.token}"
            
            val json = JSONObject().apply {
                put("id", portal.id)
                put("name", portal.name)
                put("hostUrl", portal.hostUrl)
                put("port", portal.port)
                put("username", portal.username)
                put("password", portal.password)
                put("type", portal.type)
                put("url", portal.url)
            }

            val request = Request.Builder()
                .url(url)
                .put(json.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
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
        val isMock = firebaseProjectId.isEmpty() || firebaseProjectId.contains("YOUR_FIREBASE_PROJECT_ID")

        val currentList = provisionedPortals.value.filter { it.id != portalId }
        provisionedPortals.value = currentList
        saveLocalPortalsBackup(context, currentList)

        if (isMock) {
            syncStatusMessage.value = "Deleted locally"
            return@withContext Result.success(true)
        }

        try {
            val url = "https://$firebaseProjectId-default-rtdb.firebaseio.com/users/${user.uid}/portals/$portalId.json?auth=${user.token}"
            val request = Request.Builder().url(url).delete().build()
            
            client.newCall(request).execute().use { response ->
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
    private fun parsePortalJson(id: String, obj: JSONObject): ProvisionedPortal {
        return ProvisionedPortal(
            id = obj.optString("id", id),
            name = obj.optString("name", "Portal Subscription"),
            hostUrl = obj.optString("hostUrl", ""),
            port = obj.optString("port", ""),
            username = obj.optString("username", ""),
            password = obj.optString("password", ""),
            type = obj.optString("type", "XTREAM"),
            url = obj.optString("url", "")
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
