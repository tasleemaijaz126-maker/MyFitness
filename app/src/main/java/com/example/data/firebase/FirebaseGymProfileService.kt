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
 * Service managing gym profiles in Firestore `gyms` and `gymProfiles` collections.
 */
class FirebaseGymProfileService(private val context: Context) {

    private val firestore: FirebaseFirestore by lazy { FirebaseConfig.getFirestore(context) }

    suspend fun getGymProfile(gymId: String): Result<FirebaseGym?> = withContext(Dispatchers.IO) {
        try {
            val doc = firestore.collection("gyms").document(gymId).get().await()
            if (doc.exists()) {
                val gym = doc.toObject(FirebaseGym::class.java)?.apply { id = doc.id }
                Result.success(gym)
            } else {
                val altDoc = firestore.collection("gymProfiles").document(gymId).get().await()
                if (altDoc.exists()) {
                    val gym = altDoc.toObject(FirebaseGym::class.java)?.apply { id = altDoc.id }
                    Result.success(gym)
                } else {
                    Result.success(null)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveGymProfile(gymId: String, gym: FirebaseGym): Result<FirebaseGym> = withContext(Dispatchers.IO) {
        try {
            gym.id = gymId
            if (gym.createdAt == null) gym.createdAt = Timestamp.now()
            gym.updatedAt = Timestamp.now()

            firestore.collection("gyms").document(gymId).set(gym, SetOptions.merge()).await()
            firestore.collection("gymProfiles").document(gymId).set(gym, SetOptions.merge()).await()

            FirebaseConfig.saveGymId(context, gymId)
            Result.success(gym)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun listenToGymProfile(gymId: String): Flow<FirebaseGym?> = callbackFlow {
        var listener: ListenerRegistration? = null
        try {
            listener = firestore.collection("gyms").document(gymId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    if (snapshot != null && snapshot.exists()) {
                        val gym = snapshot.toObject(FirebaseGym::class.java)?.apply { id = snapshot.id }
                        trySend(gym)
                    }
                }
        } catch (_: Exception) {}

        awaitClose { listener?.remove() }
    }
}
