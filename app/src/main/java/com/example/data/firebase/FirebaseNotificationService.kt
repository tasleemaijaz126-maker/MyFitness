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
 * Service managing notifications in Firestore `notifications` collection.
 */
class FirebaseNotificationService(private val context: Context) {

    private val firestore: FirebaseFirestore by lazy { FirebaseConfig.getFirestore(context) }

    suspend fun getNotifications(gymId: String): Result<List<FirebaseNotification>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("notifications")
                .whereEqualTo("gymId", gymId)
                .get()
                .await()
            val list = snapshot.documents.mapNotNull { doc ->
                doc.toObject(FirebaseNotification::class.java)?.apply { id = doc.id }
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveNotification(gymId: String, notification: FirebaseNotification): Result<FirebaseNotification> = withContext(Dispatchers.IO) {
        try {
            val collection = firestore.collection("notifications")
            val docRef = if (!notification.id.isNullOrBlank()) {
                collection.document(notification.id!!)
            } else {
                collection.document()
            }

            notification.id = docRef.id
            notification.gymId = gymId
            if (notification.createdAt == null) notification.createdAt = Timestamp.now()
            notification.updatedAt = Timestamp.now()

            docRef.set(notification, SetOptions.merge()).await()
            Result.success(notification)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveNotificationsBatch(gymId: String, notifications: List<FirebaseNotification>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val batch = firestore.batch()
            val collection = firestore.collection("notifications")
            val now = Timestamp.now()

            for (n in notifications) {
                val docRef = if (!n.id.isNullOrBlank()) collection.document(n.id!!) else collection.document()
                n.id = docRef.id
                n.gymId = gymId
                if (n.createdAt == null) n.createdAt = now
                n.updatedAt = now
                batch.set(docRef, n, SetOptions.merge())
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteNotification(gymId: String, notificationId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firestore.collection("notifications").document(notificationId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun listenToNotifications(gymId: String): Flow<List<FirebaseNotification>> = callbackFlow {
        var listener: ListenerRegistration? = null
        try {
            listener = firestore.collection("notifications")
                .whereEqualTo("gymId", gymId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    if (snapshot != null) {
                        val items = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(FirebaseNotification::class.java)?.apply { id = doc.id }
                        }
                        trySend(items)
                    }
                }
        } catch (_: Exception) {}

        awaitClose { listener?.remove() }
    }
}
