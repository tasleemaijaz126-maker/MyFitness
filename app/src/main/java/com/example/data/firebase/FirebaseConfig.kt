package com.example.data.firebase

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.storage.FirebaseStorage

/**
 * Enterprise Firebase Configuration and Client Factory.
 * Manages Firebase App initialization, Firestore offline caching, Auth, and Storage.
 */
object FirebaseConfig {

    private const val PREFS_NAME = "my_fitness_firebase_prefs"
    private const val KEY_GYM_ID = "gym_id"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_OWNER_NAME = "owner_name"
    private const val KEY_GYM_NAME = "gym_name"
    private const val KEY_PROJECT_ID = "project_id"
    private const val KEY_APP_ID = "app_id"
    private const val KEY_API_KEY = "api_key"

    private var isFirestoreConfigured = false

    /**
     * Initializes Firebase App if not already initialized, with graceful fallback.
     */
    fun initFirebase(context: Context): FirebaseApp {
        val appContext = context.applicationContext
        val app = try {
            FirebaseApp.getInstance()
        } catch (_: Exception) {
            val customProjectId = getCustomProjectId(appContext)
            val customApiKey = getCustomApiKey(appContext)
            val customAppId = getCustomAppId(appContext)

            if (!customProjectId.isNullOrBlank() && !customApiKey.isNullOrBlank()) {
                val options = FirebaseOptions.Builder()
                    .setProjectId(customProjectId)
                    .setApiKey(customApiKey)
                    .setApplicationId(customAppId ?: "1:160843225538:android:5fa7216c3e3a479a96e210")
                    .setStorageBucket("$customProjectId.appspot.com")
                    .build()
                try {
                    FirebaseApp.initializeApp(appContext, options)
                } catch (_: Exception) {
                    try {
                        FirebaseApp.getInstance()
                    } catch (_: Exception) {
                        FirebaseApp.initializeApp(appContext) ?: FirebaseApp.getInstance()
                    }
                }
            } else {
                try {
                    FirebaseApp.initializeApp(appContext) ?: run {
                        val fallbackOptions = FirebaseOptions.Builder()
                            .setProjectId("myfitness-gym-app")
                            .setApiKey("AIzaSyDummyGymKeyForFirebaseAppInitialization001")
                            .setApplicationId("1:160843225538:android:5fa7216c3e3a479a96e210")
                            .setStorageBucket("myfitness-gym-app.appspot.com")
                            .build()
                        try {
                            FirebaseApp.initializeApp(appContext, fallbackOptions)
                        } catch (_: Exception) {
                            FirebaseApp.getInstance()
                        }
                    }
                } catch (_: Exception) {
                    try {
                        FirebaseApp.getInstance()
                    } catch (_: Exception) {
                        val fallbackOptions = FirebaseOptions.Builder()
                            .setProjectId("myfitness-gym-app")
                            .setApiKey("AIzaSyDummyGymKeyForFirebaseAppInitialization001")
                            .setApplicationId("1:160843225538:android:5fa7216c3e3a479a96e210")
                            .setStorageBucket("myfitness-gym-app.appspot.com")
                            .build()
                        FirebaseApp.initializeApp(appContext, fallbackOptions)
                    }
                }
            }
        }

        configureFirestoreCache(app)
        return app
    }

    private fun configureFirestoreCache(app: FirebaseApp? = null) {
        if (!isFirestoreConfigured) {
            try {
                val firestore = if (app != null) FirebaseFirestore.getInstance(app) else FirebaseFirestore.getInstance()
                val settings = FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(
                        PersistentCacheSettings.newBuilder()
                            .setSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                            .build()
                    )
                    .build()
                firestore.firestoreSettings = settings
                isFirestoreConfigured = true
            } catch (_: Exception) {
                // Ignore if settings already set
            }
        }
    }

    fun getAuth(context: Context): FirebaseAuth {
        val app = initFirebase(context)
        return try {
            FirebaseAuth.getInstance(app)
        } catch (_: Exception) {
            FirebaseAuth.getInstance()
        }
    }

    fun getFirestore(context: Context): FirebaseFirestore {
        val app = initFirebase(context)
        return try {
            FirebaseFirestore.getInstance(app)
        } catch (_: Exception) {
            FirebaseFirestore.getInstance()
        }
    }

    fun getStorage(context: Context): FirebaseStorage {
        val app = initFirebase(context)
        return try {
            FirebaseStorage.getInstance(app)
        } catch (_: Exception) {
            FirebaseStorage.getInstance()
        }
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveSession(
        context: Context,
        userId: String,
        email: String,
        gymId: String? = null,
        ownerName: String? = null,
        gymName: String? = null
    ) {
        getPrefs(context).edit().apply {
            putString(KEY_USER_ID, userId)
            putString(KEY_USER_EMAIL, email)
            if (!gymId.isNullOrBlank()) putString(KEY_GYM_ID, gymId)
            if (!ownerName.isNullOrBlank()) putString(KEY_OWNER_NAME, ownerName)
            if (!gymName.isNullOrBlank()) putString(KEY_GYM_NAME, gymName)
            apply()
        }
    }

    fun getUserId(context: Context): String? {
        return getPrefs(context).getString(KEY_USER_ID, null) ?: FirebaseAuth.getInstance().currentUser?.uid
    }

    fun getUserEmail(context: Context): String? {
        return getPrefs(context).getString(KEY_USER_EMAIL, null) ?: FirebaseAuth.getInstance().currentUser?.email
    }

    fun getGymId(context: Context): String? {
        return getPrefs(context).getString(KEY_GYM_ID, null)
    }

    fun saveGymId(context: Context, gymId: String) {
        getPrefs(context).edit().putString(KEY_GYM_ID, gymId).apply()
    }

    fun getOwnerName(context: Context): String? {
        return getPrefs(context).getString(KEY_OWNER_NAME, null)
    }

    fun getGymName(context: Context): String? {
        return getPrefs(context).getString(KEY_GYM_NAME, null)
    }

    fun clearSession(context: Context) {
        getPrefs(context).edit().apply {
            remove(KEY_USER_ID)
            remove(KEY_USER_EMAIL)
            remove(KEY_GYM_ID)
            remove(KEY_OWNER_NAME)
            remove(KEY_GYM_NAME)
            apply()
        }
    }

    fun getCustomProjectId(context: Context): String? {
        return getPrefs(context).getString(KEY_PROJECT_ID, null)
    }

    fun getCustomApiKey(context: Context): String? {
        return getPrefs(context).getString(KEY_API_KEY, null)
    }

    fun getCustomAppId(context: Context): String? {
        return getPrefs(context).getString(KEY_APP_ID, null)
    }

    fun getProjectUrl(context: Context): String {
        return getCustomProjectId(context) ?: "myfitness-gym-app"
    }

    fun getAnonKey(context: Context): String {
        return getCustomApiKey(context) ?: ""
    }

    fun saveConfig(context: Context, projectId: String, apiKey: String) {
        saveCustomConfig(context, projectId, apiKey)
    }

    fun saveCustomConfig(context: Context, projectId: String, apiKey: String, appId: String? = null) {
        getPrefs(context).edit().apply {
            putString(KEY_PROJECT_ID, projectId.trim())
            putString(KEY_API_KEY, apiKey.trim())
            if (!appId.isNullOrBlank()) putString(KEY_APP_ID, appId.trim())
            apply()
        }
    }
}
