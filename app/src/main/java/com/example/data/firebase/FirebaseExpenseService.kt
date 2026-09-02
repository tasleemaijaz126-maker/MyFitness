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
 * Service managing expense records in Firestore `expenses` collection.
 */
class FirebaseExpenseService(private val context: Context) {

    private val firestore: FirebaseFirestore by lazy { FirebaseConfig.getFirestore(context) }

    suspend fun getExpenses(gymId: String): Result<List<FirebaseExpense>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("expenses")
                .whereEqualTo("gymId", gymId)
                .get()
                .await()
            val list = snapshot.documents.mapNotNull { doc ->
                doc.toObject(FirebaseExpense::class.java)?.apply { id = doc.id }
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveExpense(gymId: String, expense: FirebaseExpense): Result<FirebaseExpense> = withContext(Dispatchers.IO) {
        try {
            val collection = firestore.collection("expenses")
            val docRef = if (!expense.id.isNullOrBlank()) {
                collection.document(expense.id!!)
            } else {
                collection.document()
            }

            expense.id = docRef.id
            expense.gymId = gymId
            if (expense.createdAt == null) expense.createdAt = Timestamp.now()
            expense.updatedAt = Timestamp.now()

            docRef.set(expense, SetOptions.merge()).await()
            Result.success(expense)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveExpensesBatch(gymId: String, expenses: List<FirebaseExpense>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val batch = firestore.batch()
            val collection = firestore.collection("expenses")
            val now = Timestamp.now()

            for (exp in expenses) {
                val docRef = if (!exp.id.isNullOrBlank()) collection.document(exp.id!!) else collection.document()
                exp.id = docRef.id
                exp.gymId = gymId
                if (exp.createdAt == null) exp.createdAt = now
                exp.updatedAt = now
                batch.set(docRef, exp, SetOptions.merge())
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteExpense(gymId: String, expenseId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firestore.collection("expenses").document(expenseId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun listenToExpenses(gymId: String): Flow<List<FirebaseExpense>> = callbackFlow {
        var listener: ListenerRegistration? = null
        try {
            listener = firestore.collection("expenses")
                .whereEqualTo("gymId", gymId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    if (snapshot != null) {
                        val items = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(FirebaseExpense::class.java)?.apply { id = doc.id }
                        }
                        trySend(items)
                    }
                }
        } catch (_: Exception) {}

        awaitClose { listener?.remove() }
    }
}
