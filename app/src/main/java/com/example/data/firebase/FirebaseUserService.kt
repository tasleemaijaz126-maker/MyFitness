package com.example.data.firebase

import android.content.Context
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Service managing user and staff profiles in Firestore `users` collection.
 */
class FirebaseUserService(private val context: Context) {

    private val firestore: FirebaseFirestore by lazy { FirebaseConfig.getFirestore(context) }

    suspend fun getUserProfile(userId: String): Result<FirebaseProfile?> = withContext(Dispatchers.IO) {
        try {
            val doc = firestore.collection("users").document(userId).get().await()
            if (doc.exists()) {
                val profile = doc.toObject(FirebaseProfile::class.java)?.apply { id = doc.id }
                Result.success(profile)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveUserProfile(userId: String, profile: FirebaseProfile): Result<FirebaseProfile> = withContext(Dispatchers.IO) {
        try {
            profile.id = userId
            profile.userId = userId
            if (profile.createdAt == null) profile.createdAt = Timestamp.now()
            profile.updatedAt = Timestamp.now()

            firestore.collection("users").document(userId).set(profile, SetOptions.merge()).await()
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getStaffUsers(gymId: String): Result<List<FirebaseProfile>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("users")
                .whereEqualTo("gymId", gymId)
                .get()
                .await()
            val list = snapshot.documents.mapNotNull { doc ->
                doc.toObject(FirebaseProfile::class.java)?.apply { id = doc.id }
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
