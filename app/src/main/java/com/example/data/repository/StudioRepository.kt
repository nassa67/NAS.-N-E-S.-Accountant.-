package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.BusinessProfile
import com.example.data.model.ClientInteraction
import com.example.data.model.ClientProfile
import com.example.data.model.PhotoTransaction
import com.example.data.model.UserAccount
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class StudioRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeUserAccount(): Flow<UserAccount?> = flow {
        val uid = requireUserId()
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserAccount::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun ensureOrSaveUserAccount(
        displayName: String,
        email: String,
        studioRole: String = "Studio Owner & Photographer"
    ): Result<UserAccount> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val safeName = displayName.trim().ifEmpty {
            auth.currentUser?.displayName?.trim().orEmpty().ifEmpty { "Studio Owner" }
        }.take(120)
        val safeEmail = email.trim().ifEmpty {
            auth.currentUser?.email?.trim().orEmpty()
        }.take(180)
        val safeRole = studioRole.trim().ifEmpty { "Studio Owner & Photographer" }.take(100)

        try {
            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                val existingCreatedAt = existingSnap.getTimestamp("createdAt")
                if (existingCreatedAt != null) {
                    val updatePayload = mapOf(
                        "userId" to uid,
                        "displayName" to safeName,
                        "email" to safeEmail,
                        "studioRole" to safeRole,
                        "createdAt" to existingCreatedAt,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    docRef.set(updatePayload).await()
                }
            } else {
                val createPayload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "email" to safeEmail,
                    "studioRole" to safeRole,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(createPayload).await()
            }
            UserAccount(
                id = uid,
                userId = uid,
                displayName = safeName,
                email = safeEmail,
                studioRole = safeRole
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    fun observeBusinesses(): Flow<List<BusinessProfile>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/businesses"
        emitAll(
            db.collection("users").document(uid).collection("businesses")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        BusinessProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeClients(): Flow<List<ClientProfile>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/clients"
        emitAll(
            db.collection("users").document(uid).collection("clients")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ClientProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeInteractions(): Flow<List<ClientInteraction>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/interactions"
        emitAll(
            db.collection("users").document(uid).collection("interactions")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ClientInteraction::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.interactionDateMillis }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeTransactions(): Flow<List<PhotoTransaction>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/transactions"
        emitAll(
            db.collection("users").document(uid).collection("transactions")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PhotoTransaction::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.transactionDateMillis }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createBusiness(
        name: String,
        category: String,
        defaultPricePerPicture: Double,
        monthlyTarget: Double,
        accentColorHex: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("businesses").document()
        val payload = mapOf(
            "userId" to uid,
            "name" to name.trim().ifEmpty { "My Studio" }.take(120),
            "category" to category.trim().ifEmpty { "Photography Studio" }.take(100),
            "defaultPricePerPicture" to defaultPricePerPicture.coerceIn(0.0, 1_000_000_000.0),
            "monthlyTarget" to monthlyTarget.coerceIn(0.0, 10_000_000_000.0),
            "accentColorHex" to accentColorHex.ifBlank { "#6366F1" }.take(32),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateBusiness(business: BusinessProfile): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(business.id.isNotBlank()) { "Business ID must not be blank" }
        requireNotNull(business.createdAt) { "Business createdAt must not be null for update" }
        val docRef = db.collection("users").document(uid).collection("businesses").document(business.id)
        try {
            docRef.set(
                business.copy(
                    userId = uid,
                    name = business.name.trim().ifEmpty { "My Studio" }.take(120),
                    category = business.category.trim().ifEmpty { "Photography Studio" }.take(100),
                    defaultPricePerPicture = business.defaultPricePerPicture.coerceIn(0.0, 1_000_000_000.0),
                    monthlyTarget = business.monthlyTarget.coerceIn(0.0, 10_000_000_000.0),
                    accentColorHex = business.accentColorHex.ifBlank { "#6366F1" }.take(32),
                    updatedAt = null
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteBusiness(businessId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("businesses").document(businessId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun createClient(
        businessId: String,
        name: String,
        phoneNumber: String,
        email: String,
        notes: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("clients").document()
        val payload = mapOf(
            "userId" to uid,
            "businessId" to businessId,
            "name" to name.trim().ifEmpty { "Client" }.take(150),
            "phoneNumber" to phoneNumber.trim().ifEmpty { "No Phone" }.take(60),
            "email" to email.trim().take(180),
            "notes" to notes.trim().take(2000),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateClient(client: ClientProfile): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(client.id.isNotBlank()) { "Client ID must not be blank" }
        requireNotNull(client.createdAt) { "Client createdAt must not be null for update" }
        val docRef = db.collection("users").document(uid).collection("clients").document(client.id)
        try {
            docRef.set(
                client.copy(
                    userId = uid,
                    name = client.name.trim().ifEmpty { "Client" }.take(150),
                    phoneNumber = client.phoneNumber.trim().ifEmpty { "No Phone" }.take(60),
                    email = client.email.trim().take(180),
                    notes = client.notes.trim().take(2000),
                    updatedAt = null
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteClient(clientId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("clients").document(clientId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun createInteraction(
        businessId: String,
        clientId: String,
        clientName: String,
        type: String,
        summary: String,
        interactionDateMillis: Long = System.currentTimeMillis()
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("interactions").document()
        val payload = mapOf(
            "userId" to uid,
            "businessId" to businessId,
            "clientId" to clientId,
            "clientName" to clientName.trim().ifEmpty { "Client" }.take(150),
            "type" to type,
            "summary" to summary.trim().ifEmpty { "Interaction logged" }.take(2000),
            "interactionDateMillis" to interactionDateMillis.coerceAtLeast(1L),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteInteraction(interactionId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("interactions").document(interactionId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun createTransaction(
        businessId: String,
        clientId: String,
        clientName: String,
        customerPhone: String,
        transactionDateMillis: Long,
        pictureCount: Long,
        pricePerPicture: Double,
        paymentMethod: String,
        notes: String,
        totalAmountOverride: Double? = null
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val safeCount = pictureCount.coerceIn(1L, 10_000_000L)
        val safePrice = pricePerPicture.coerceIn(0.0, 10_000_000_000.0)
        val total = (totalAmountOverride ?: (safeCount * safePrice)).coerceIn(0.0, 100_000_000_000.0)
        val yearMonthFormat = SimpleDateFormat("yyyy-MM", Locale.US)
        val yearMonth = yearMonthFormat.format(Date(transactionDateMillis))
        val safePhone = customerPhone.trim().ifEmpty { "No Phone" }.take(60)
        val safeName = clientName.trim().ifEmpty { safePhone }.take(150)

        val docRef = db.collection("users").document(uid).collection("transactions").document()
        val payload = mapOf(
            "userId" to uid,
            "businessId" to businessId,
            "clientId" to clientId.trim().take(128),
            "clientName" to safeName,
            "customerPhone" to safePhone,
            "transactionDateMillis" to transactionDateMillis.coerceAtLeast(1L),
            "yearMonth" to yearMonth,
            "pictureCount" to safeCount,
            "pricePerPicture" to safePrice,
            "totalAmount" to total,
            "paymentMethod" to paymentMethod,
            "notes" to notes.trim().take(2000),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateTransaction(transaction: PhotoTransaction): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(transaction.id.isNotBlank()) { "Transaction ID must not be blank for update" }
        requireNotNull(transaction.createdAt) { "Transaction createdAt must not be null for update" }
        val safeCount = transaction.pictureCount.coerceIn(1L, 10_000_000L)
        val safePrice = transaction.pricePerPicture.coerceIn(0.0, 10_000_000_000.0)
        val safeTotal = transaction.totalAmount.coerceIn(0.0, 100_000_000_000.0)
        val safeDate = transaction.transactionDateMillis.coerceAtLeast(1L)
        val yearMonthFormat = SimpleDateFormat("yyyy-MM", Locale.US)
        val yearMonth = yearMonthFormat.format(Date(safeDate))
        val safePhone = transaction.customerPhone.trim().ifEmpty { "No Phone" }.take(60)
        val safeName = transaction.clientName.trim().ifEmpty { safePhone }.take(150)

        val docRef = db.collection("users").document(uid).collection("transactions").document(transaction.id)
        val updated = transaction.copy(
            userId = uid,
            clientId = transaction.clientId.trim().take(128),
            clientName = safeName,
            customerPhone = safePhone,
            transactionDateMillis = safeDate,
            yearMonth = yearMonth,
            pictureCount = safeCount,
            pricePerPicture = safePrice,
            totalAmount = safeTotal,
            notes = transaction.notes.trim().take(2000),
            updatedAt = null
        )
        try {
            docRef.set(updated).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteTransaction(transactionId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("transactions").document(transactionId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun getBusinessById(ownerUid: String, businessId: String): Result<BusinessProfile?> = runCatching {
        val docRef = db.collection("users").document(ownerUid).collection("businesses").document(businessId)
        try {
            val snapshot = docRef.get().await()
            snapshot.toObject(BusinessProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }
}
