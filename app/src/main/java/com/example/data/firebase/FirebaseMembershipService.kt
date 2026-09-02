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
 * Service managing membership records in Firestore `memberships` collection.
 */
class FirebaseMembershipService(private val context: Context) {

    private val firestore: FirebaseFirestore by lazy { FirebaseConfig.getFirestore(context) }

    suspend fun getMemberships(gymId: String): Result<List<FirebaseMembership>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("memberships")
                .whereEqualTo("gymId", gymId)
                .get()
                .await()
            val list = snapshot.documents.mapNotNull { doc ->
                doc.toObject(FirebaseMembership::class.java)?.apply { id = doc.id }
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveMembership(gymId: String, membership: FirebaseMembership): Result<FirebaseMembership> = withContext(Dispatchers.IO) {
        try {
            val collection = firestore.collection("memberships")
            val docRef = if (!membership.id.isNullOrBlank()) {
                collection.document(membership.id!!)
            } else {
                collection.document()
            }

            membership.id = docRef.id
            membership.gymId = gymId
            if (membership.createdAt == null) membership.createdAt = Timestamp.now()
            membership.updatedAt = Timestamp.now()

            docRef.set(membership, SetOptions.merge()).await()
            Result.success(membership)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveMembershipsBatch(gymId: String, memberships: List<FirebaseMembership>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val batch = firestore.batch()
            val collection = firestore.collection("memberships")
            val now = Timestamp.now()

            for (m in memberships) {
                val docRef = if (!m.id.isNullOrBlank()) collection.document(m.id!!) else collection.document()
                m.id = docRef.id
                m.gymId = gymId
                if (m.createdAt == null) m.createdAt = now
                m.updatedAt = now
                batch.set(docRef, m, SetOptions.merge())
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteMembership(gymId: String, membershipId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firestore.collection("memberships").document(membershipId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Executes atomic membership creation via Firestore batch write.
     * Atomically creates Customer, Membership, Payment, and Invoice documents.
     */
    suspend fun createFullMembershipTransaction(
        gymId: String,
        customer: FirebaseCustomer,
        membership: FirebaseMembership,
        payment: FirebasePayment,
        invoice: FirebaseInvoice
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val batch = firestore.batch()
            val now = Timestamp.now()

            // Customer
            val custRef = if (!customer.id.isNullOrBlank()) {
                firestore.collection("customers").document(customer.id!!)
            } else {
                firestore.collection("customers").document()
            }
            customer.id = custRef.id
            customer.gymId = gymId
            if (customer.createdAt == null) customer.createdAt = now
            customer.updatedAt = now
            batch.set(custRef, customer, SetOptions.merge())

            // Membership
            val memRef = if (!membership.id.isNullOrBlank()) {
                firestore.collection("memberships").document(membership.id!!)
            } else {
                firestore.collection("memberships").document()
            }
            membership.id = memRef.id
            membership.gymId = gymId
            membership.customerId = customer.id!!
            if (membership.createdAt == null) membership.createdAt = now
            membership.updatedAt = now
            batch.set(memRef, membership, SetOptions.merge())

            // Payment
            val payRef = if (!payment.id.isNullOrBlank()) {
                firestore.collection("payments").document(payment.id!!)
            } else {
                firestore.collection("payments").document()
            }
            payment.id = payRef.id
            payment.gymId = gymId
            payment.customerId = customer.id!!
            payment.membershipId = membership.id
            if (payment.createdAt == null) payment.createdAt = now
            payment.updatedAt = now
            batch.set(payRef, payment, SetOptions.merge())

            // Invoice
            val invRef = if (!invoice.id.isNullOrBlank()) {
                firestore.collection("invoices").document(invoice.id!!)
            } else {
                firestore.collection("invoices").document()
            }
            invoice.id = invRef.id
            invoice.gymId = gymId
            invoice.customerId = customer.id!!
            invoice.membershipId = membership.id
            if (invoice.createdAt == null) invoice.createdAt = now
            invoice.updatedAt = now
            batch.set(invRef, invoice, SetOptions.merge())

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun listenToMemberships(gymId: String): Flow<List<FirebaseMembership>> = callbackFlow {
        var listener: ListenerRegistration? = null
        try {
            listener = firestore.collection("memberships")
                .whereEqualTo("gymId", gymId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    if (snapshot != null) {
                        val items = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(FirebaseMembership::class.java)?.apply { id = doc.id }
                        }
                        trySend(items)
                    }
                }
        } catch (_: Exception) {}

        awaitClose { listener?.remove() }
    }
}
