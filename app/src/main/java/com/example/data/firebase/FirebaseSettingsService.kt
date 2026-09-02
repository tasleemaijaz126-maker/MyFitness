package com.example.data.firebase

import android.content.Context
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Service managing application settings in Firestore `appSettings` collection.
 */
class FirebaseSettingsService(private val context: Context) {

    private val firestore: FirebaseFirestore by lazy { FirebaseConfig.getFirestore(context) }

    suspend fun getSettings(gymId: String): Result<FirebaseAppSettings?> = withContext(Dispatchers.IO) {
        try {
            val doc = firestore.collection("appSettings").document(gymId).get().await()
            if (doc.exists()) {
                val s = doc.toObject(FirebaseAppSettings::class.java)?.apply { id = doc.id }
                Result.success(s)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveSettings(gymId: String, settings: FirebaseAppSettings): Result<FirebaseAppSettings> = withContext(Dispatchers.IO) {
        try {
            settings.id = gymId
            settings.gymId = gymId
            if (settings.createdAt == null) settings.createdAt = Timestamp.now()
            settings.updatedAt = Timestamp.now()

            firestore.collection("appSettings").document(gymId).set(settings, SetOptions.merge()).await()
            Result.success(settings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun listenToSettings(gymId: String): Flow<FirebaseAppSettings?> = callbackFlow {
        var listener: ListenerRegistration? = null
        try {
            listener = firestore.collection("appSettings").document(gymId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    if (snapshot != null && snapshot.exists()) {
                        val s = snapshot.toObject(FirebaseAppSettings::class.java)?.apply { id = snapshot.id }
                        trySend(s)
                    }
                }
        } catch (_: Exception) {}

        awaitClose { listener?.remove() }
    }
}
