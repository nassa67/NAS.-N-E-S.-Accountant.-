package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserAccount(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val studioRole: String = "Studio Owner & Photographer",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class BusinessProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val category: String = "",
    val defaultPricePerPicture: Double = 0.0,
    val monthlyTarget: Double = 0.0,
    val accentColorHex: String = "#6366F1",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ClientProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val businessId: String = "",
    val name: String = "",
    val phoneNumber: String = "",
    val email: String = "",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ClientInteraction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val businessId: String = "",
    val clientId: String = "",
    val clientName: String = "",
    val type: String = "CALL", // CALL, SHOOT, DELIVERY, FOLLOW_UP, INQUIRY, NOTE
    val summary: String = "",
    val interactionDateMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PhotoTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val businessId: String = "",
    val clientId: String = "",
    val clientName: String = "",
    val customerPhone: String = "",
    val transactionDateMillis: Long = 0L,
    val yearMonth: String = "", // YYYY-MM
    val pictureCount: Long = 1L,
    val pricePerPicture: Double = 0.0,
    val totalAmount: Double = 0.0,
    val paymentMethod: String = "CASH", // CASH or BANK_TRANSFER
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class ScannedReceiptData(
    val clientName: String = "",
    val customerPhone: String = "",
    val amountPaid: Double = 0.0,
    val transactionDateMillis: Long = System.currentTimeMillis(),
    val dateIso: String = "",
    val pictureCount: Long = 1L,
    val pricePerPicture: Double = 0.0,
    val paymentMethod: String = "CASH",
    val invoiceDetails: String = ""
)

sealed interface ReceiptScanUiState {
    data object Idle : ReceiptScanUiState
    data object Scanning : ReceiptScanUiState
    data class Success(val data: ScannedReceiptData) : ReceiptScanUiState
    data class Error(val message: String) : ReceiptScanUiState
}

data class AiFutureForecastData(
    val nextMonthProjectedRevenue: Double = 0.0,
    val threeMonthProjectedRevenue: Double = 0.0,
    val recommendedPricePerPicture: Double = 0.0,
    val recommendedMonthlyTarget: Double = 0.0,
    val growthOutlookSummary: String = "",
    val cashFlowOptimizationTip: String = "",
    val actionPlanSteps: List<String> = emptyList(),
    val clientFollowUpMessageDraft: String = ""
)

sealed interface AiFutureUiState {
    data object Idle : AiFutureUiState
    data object Analyzing : AiFutureUiState
    data class Success(val forecast: AiFutureForecastData) : AiFutureUiState
    data class Error(val message: String) : AiFutureUiState
}

data class WeeklyBucket(
    val label: String,
    val cashAmount: Double,
    val bankTransferAmount: Double,
    val totalAmount: Double,
    val pictureCount: Long
)

data class MonthlyReportSummary(
    val yearMonth: String,
    val displayMonthName: String,
    val totalEarnings: Double,
    val cashEarnings: Double,
    val bankTransferEarnings: Double,
    val cashTransactionCount: Int,
    val bankTransferTransactionCount: Int,
    val totalTransactions: Int,
    val totalPictures: Long,
    val averagePricePerPicture: Double,
    val monthlyTarget: Double,
    val targetProgressFraction: Float,
    val cashFraction: Float,
    val bankTransferFraction: Float,
    val weeklyBuckets: List<WeeklyBucket>,
    val transactions: List<PhotoTransaction>
)
