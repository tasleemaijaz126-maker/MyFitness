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
 * Service managing customer records in Firestore `customers` collection.
 */
class FirebaseCustomerService(private val context: Context) {

    private val firestore: FirebaseFirestore by lazy { FirebaseConfig.getFirestore(context) }

    suspend fun getCustomers(gymId: String): Result<List<FirebaseCustomer>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("customers")
                .whereEqualTo("gymId", gymId)
                .get()
                .await()
            val list = snapshot.documents.mapNotNull { doc ->
                doc.toObject(FirebaseCustomer::class.java)?.apply { id = doc.id }
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCustomerById(gymId: String, customerId: String): Result<FirebaseCustomer?> = withContext(Dispatchers.IO) {
        try {
            val doc = firestore.collection("customers").document(customerId).get().await()
            if (doc.exists() && doc.getString("gymId") == gymId) {
                val customer = doc.toObject(FirebaseCustomer::class.java)?.apply { id = doc.id }
                Result.success(customer)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveCustomer(gymId: String, customer: FirebaseCustomer): Result<FirebaseCustomer> = withContext(Dispatchers.IO) {
        try {
            val collection = firestore.collection("customers")
            val docRef = if (!customer.id.isNullOrBlank()) {
                collection.document(customer.id!!)
            } else {
                collection.document()
            }

            customer.id = docRef.id
            customer.gymId = gymId
            if (customer.createdAt == null) {
                customer.createdAt = Timestamp.now()
            }
            customer.updatedAt = Timestamp.now()

            docRef.set(customer, SetOptions.merge()).await()
            Result.success(customer)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveCustomersBatch(gymId: String, customers: List<FirebaseCustomer>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val batch = firestore.batch()
            val collection = firestore.collection("customers")
            val now = Timestamp.now()

            for (c in customers) {
                val docRef = if (!c.id.isNullOrBlank()) collection.document(c.id!!) else collection.document()
                c.id = docRef.id
                c.gymId = gymId
                if (c.createdAt == null) c.createdAt = now
                c.updatedAt = now
                batch.set(docRef, c, SetOptions.merge())
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteCustomer(gymId: String, customerId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firestore.collection("customers").document(customerId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Realtime Firestore snapshot flow for customers.
     */
    fun listenToCustomers(gymId: String): Flow<List<FirebaseCustomer>> = callbackFlow {
        var listener: ListenerRegistration? = null
        try {
            listener = firestore.collection("customers")
                .whereEqualTo("gymId", gymId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val items = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(FirebaseCustomer::class.java)?.apply { id = doc.id }
                        }
                        trySend(items)
                    }
                }
        } catch (_: Exception) {}

        awaitClose {
            listener?.remove()
        }
    }
}
