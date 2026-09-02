package com.example.data.firebase

import android.content.Context
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Service managing asset uploads (Logos, Signatures, Customer Photos, PDFs) to Firebase Storage.
 */
class FirebaseStorageService(private val context: Context) {

    private val storage: FirebaseStorage by lazy { FirebaseConfig.getStorage(context) }

    suspend fun uploadGymLogo(gymId: String, imageUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val ref = storage.reference.child("gyms/$gymId/logo_${System.currentTimeMillis()}.jpg")
            val metadata = StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .build()

            val uploadTask = ref.putFile(imageUri, metadata).await()
            val downloadUrl = uploadTask.storage.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadGymLogoBytes(gymId: String, bytes: ByteArray): Result<String> = withContext(Dispatchers.IO) {
        try {
            val ref = storage.reference.child("gyms/$gymId/logo_${System.currentTimeMillis()}.png")
            val metadata = StorageMetadata.Builder()
                .setContentType("image/png")
                .build()

            val uploadTask = ref.putBytes(bytes, metadata).await()
            val downloadUrl = uploadTask.storage.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadSignature(gymId: String, signatureBytes: ByteArray): Result<String> = withContext(Dispatchers.IO) {
        try {
            val ref = storage.reference.child("gyms/$gymId/signatures/sig_${System.currentTimeMillis()}.png")
            val metadata = StorageMetadata.Builder()
                .setContentType("image/png")
                .build()

            val uploadTask = ref.putBytes(signatureBytes, metadata).await()
            val downloadUrl = uploadTask.storage.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadCustomerPhoto(gymId: String, customerId: String, imageUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val ref = storage.reference.child("gyms/$gymId/customers/$customerId/photo_${System.currentTimeMillis()}.jpg")
            val metadata = StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .build()

            val uploadTask = ref.putFile(imageUri, metadata).await()
            val downloadUrl = uploadTask.storage.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadInvoicePdf(gymId: String, invoiceNumber: String, pdfFile: File): Result<String> = withContext(Dispatchers.IO) {
        try {
            val ref = storage.reference.child("gyms/$gymId/invoices/inv_${invoiceNumber}.pdf")
            val metadata = StorageMetadata.Builder()
                .setContentType("application/pdf")
                .build()

            val uploadTask = ref.putFile(Uri.fromFile(pdfFile), metadata).await()
            val downloadUrl = uploadTask.storage.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadGenericFile(gymId: String, folder: String, fileName: String, bytes: ByteArray, mimeType: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val ref = storage.reference.child("gyms/$gymId/$folder/${UUID.randomUUID()}_$fileName")
            val metadata = StorageMetadata.Builder()
                .setContentType(mimeType)
                .build()

            val uploadTask = ref.putBytes(bytes, metadata).await()
            val downloadUrl = uploadTask.storage.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
