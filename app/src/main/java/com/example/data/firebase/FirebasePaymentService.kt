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
 * Service managing payment records in Firestore `payments` collection.
 */
class FirebasePaymentService(private val context: Context) {

    private val firestore: FirebaseFirestore by lazy { FirebaseConfig.getFirestore(context) }

    suspend fun getPayments(gymId: String): Result<List<FirebasePayment>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("payments")
                .whereEqualTo("gymId", gymId)
                .get()
                .await()
            val list = snapshot.documents.mapNotNull { doc ->
                doc.toObject(FirebasePayment::class.java)?.apply { id = doc.id }
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun savePayment(gymId: String, payment: FirebasePayment): Result<FirebasePayment> = withContext(Dispatchers.IO) {
        try {
            val collection = firestore.collection("payments")
            val docRef = if (!payment.id.isNullOrBlank()) {
                collection.document(payment.id!!)
            } else {
                collection.document()
            }

            payment.id = docRef.id
            payment.gymId = gymId
            if (payment.createdAt == null) payment.createdAt = Timestamp.now()
            payment.updatedAt = Timestamp.now()

            docRef.set(payment, SetOptions.merge()).await()
            Result.success(payment)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun savePaymentsBatch(gymId: String, payments: List<FirebasePayment>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val batch = firestore.batch()
            val collection = firestore.collection("payments")
            val now = Timestamp.now()

            for (p in payments) {
                val docRef = if (!p.id.isNullOrBlank()) collection.document(p.id!!) else collection.document()
                p.id = docRef.id
                p.gymId = gymId
                if (p.createdAt == null) p.createdAt = now
                p.updatedAt = now
                batch.set(docRef, p, SetOptions.merge())
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePayment(gymId: String, paymentId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firestore.collection("payments").document(paymentId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun listenToPayments(gymId: String): Flow<List<FirebasePayment>> = callbackFlow {
        var listener: ListenerRegistration? = null
        try {
            listener = firestore.collection("payments")
                .whereEqualTo("gymId", gymId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    if (snapshot != null) {
                        val items = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(FirebasePayment::class.java)?.apply { id = doc.id }
                        }
                        trySend(items)
                    }
                }
        } catch (_: Exception) {}

        awaitClose { listener?.remove() }
    }
}
