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
 * Service managing invoice records in Firestore `invoices` collection.
 */
class FirebaseInvoiceService(private val context: Context) {

    private val firestore: FirebaseFirestore by lazy { FirebaseConfig.getFirestore(context) }

    suspend fun getInvoices(gymId: String): Result<List<FirebaseInvoice>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("invoices")
                .whereEqualTo("gymId", gymId)
                .get()
                .await()
            val list = snapshot.documents.mapNotNull { doc ->
                doc.toObject(FirebaseInvoice::class.java)?.apply { id = doc.id }
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveInvoice(gymId: String, invoice: FirebaseInvoice): Result<FirebaseInvoice> = withContext(Dispatchers.IO) {
        try {
            val collection = firestore.collection("invoices")
            val docRef = if (!invoice.id.isNullOrBlank()) {
                collection.document(invoice.id!!)
            } else {
                collection.document()
            }

            invoice.id = docRef.id
            invoice.gymId = gymId
            if (invoice.createdAt == null) invoice.createdAt = Timestamp.now()
            invoice.updatedAt = Timestamp.now()

            docRef.set(invoice, SetOptions.merge()).await()
            Result.success(invoice)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveInvoicesBatch(gymId: String, invoices: List<FirebaseInvoice>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val batch = firestore.batch()
            val collection = firestore.collection("invoices")
            val now = Timestamp.now()

            for (inv in invoices) {
                val docRef = if (!inv.id.isNullOrBlank()) collection.document(inv.id!!) else collection.document()
                inv.id = docRef.id
                inv.gymId = gymId
                if (inv.createdAt == null) inv.createdAt = now
                inv.updatedAt = now
                batch.set(docRef, inv, SetOptions.merge())
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteInvoice(gymId: String, invoiceId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firestore.collection("invoices").document(invoiceId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun listenToInvoices(gymId: String): Flow<List<FirebaseInvoice>> = callbackFlow {
        var listener: ListenerRegistration? = null
        try {
            listener = firestore.collection("invoices")
                .whereEqualTo("gymId", gymId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    if (snapshot != null) {
                        val items = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(FirebaseInvoice::class.java)?.apply { id = doc.id }
                        }
                        trySend(items)
                    }
                }
        } catch (_: Exception) {}

        awaitClose { listener?.remove() }
    }
}
