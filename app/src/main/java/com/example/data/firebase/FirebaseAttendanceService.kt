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
 * Service managing attendance records in Firestore `attendance` collection.
 */
class FirebaseAttendanceService(private val context: Context) {

    private val firestore: FirebaseFirestore by lazy { FirebaseConfig.getFirestore(context) }

    suspend fun getAttendance(gymId: String): Result<List<FirebaseAttendance>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("attendance")
                .whereEqualTo("gymId", gymId)
                .get()
                .await()
            val list = snapshot.documents.mapNotNull { doc ->
                doc.toObject(FirebaseAttendance::class.java)?.apply { id = doc.id }
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveAttendance(gymId: String, attendance: FirebaseAttendance): Result<FirebaseAttendance> = withContext(Dispatchers.IO) {
        try {
            val collection = firestore.collection("attendance")
            val docRef = if (!attendance.id.isNullOrBlank()) {
                collection.document(attendance.id!!)
            } else {
                collection.document()
            }

            attendance.id = docRef.id
            attendance.gymId = gymId
            if (attendance.createdAt == null) attendance.createdAt = Timestamp.now()
            attendance.updatedAt = Timestamp.now()

            docRef.set(attendance, SetOptions.merge()).await()
            Result.success(attendance)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveAttendanceBatch(gymId: String, attendanceList: List<FirebaseAttendance>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val batch = firestore.batch()
            val collection = firestore.collection("attendance")
            val now = Timestamp.now()

            for (a in attendanceList) {
                val docRef = if (!a.id.isNullOrBlank()) collection.document(a.id!!) else collection.document()
                a.id = docRef.id
                a.gymId = gymId
                if (a.createdAt == null) a.createdAt = now
                a.updatedAt = now
                batch.set(docRef, a, SetOptions.merge())
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAttendance(gymId: String, attendanceId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firestore.collection("attendance").document(attendanceId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun listenToAttendance(gymId: String): Flow<List<FirebaseAttendance>> = callbackFlow {
        var listener: ListenerRegistration? = null
        try {
            listener = firestore.collection("attendance")
                .whereEqualTo("gymId", gymId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    if (snapshot != null) {
                        val items = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(FirebaseAttendance::class.java)?.apply { id = doc.id }
                        }
                        trySend(items)
                    }
                }
        } catch (_: Exception) {}

        awaitClose { listener?.remove() }
    }
}
