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
 * Service managing membership plans in Firestore `membershipPlans` collection.
 */
class FirebaseMembershipPlanService(private val context: Context) {

    private val firestore: FirebaseFirestore by lazy { FirebaseConfig.getFirestore(context) }

    suspend fun getPlans(gymId: String): Result<List<FirebaseMembershipPlan>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("membershipPlans")
                .whereEqualTo("gymId", gymId)
                .get()
                .await()
            val list = snapshot.documents.mapNotNull { doc ->
                doc.toObject(FirebaseMembershipPlan::class.java)?.apply { id = doc.id }
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun savePlan(gymId: String, plan: FirebaseMembershipPlan): Result<FirebaseMembershipPlan> = withContext(Dispatchers.IO) {
        try {
            val collection = firestore.collection("membershipPlans")
            val docRef = if (!plan.id.isNullOrBlank()) {
                collection.document(plan.id!!)
            } else {
                collection.document()
            }

            plan.id = docRef.id
            plan.gymId = gymId
            if (plan.createdAt == null) plan.createdAt = Timestamp.now()
            plan.updatedAt = Timestamp.now()

            docRef.set(plan, SetOptions.merge()).await()
            Result.success(plan)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun savePlansBatch(gymId: String, plans: List<FirebaseMembershipPlan>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val batch = firestore.batch()
            val collection = firestore.collection("membershipPlans")
            val now = Timestamp.now()

            for (p in plans) {
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

    suspend fun deletePlan(gymId: String, planId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firestore.collection("membershipPlans").document(planId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun listenToPlans(gymId: String): Flow<List<FirebaseMembershipPlan>> = callbackFlow {
        var listener: ListenerRegistration? = null
        try {
            listener = firestore.collection("membershipPlans")
                .whereEqualTo("gymId", gymId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    if (snapshot != null) {
                        val items = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(FirebaseMembershipPlan::class.java)?.apply { id = doc.id }
                        }
                        trySend(items)
                    }
                }
        } catch (_: Exception) {}

        awaitClose { listener?.remove() }
    }
}
