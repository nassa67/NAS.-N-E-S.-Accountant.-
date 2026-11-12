package com.example.ui.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AiFutureForecastData
import com.example.data.model.AiFutureUiState
import com.example.data.model.BusinessProfile
import com.example.data.model.ClientInteraction
import com.example.data.model.ClientProfile
import com.example.data.model.MonthlyReportSummary
import com.example.data.model.PhotoTransaction
import com.example.data.model.ReceiptScanUiState
import com.example.data.model.ScannedReceiptData
import com.example.data.model.UserAccount
import com.example.data.model.WeeklyBucket
import com.example.data.repository.StudioRepository
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject

data class StudioUiState(
    val isLoading: Boolean = true,
    val userAccount: UserAccount? = null,
    val businesses: List<BusinessProfile> = emptyList(),
    val activeBusiness: BusinessProfile? = null,
    val clientsForBusiness: List<ClientProfile> = emptyList(),
    val interactionsForBusiness: List<ClientInteraction> = emptyList(),
    val transactionsForBusiness: List<PhotoTransaction> = emptyList(),
    val selectedYearMonth: String = StudioViewModel.currentYearMonth(),
    val availableYearMonths: List<String> = listOf(StudioViewModel.currentYearMonth()),
    val monthlyReport: MonthlyReportSummary = StudioViewModel.emptyMonthlyReport(StudioViewModel.currentYearMonth()),
    val errorMessage: String? = null,
    val statusBannerMessage: String? = null,
    val isSeeding: Boolean = false
)

class StudioViewModel(
    private val repository: StudioRepository
) : ViewModel() {

    private val _selectedBusinessId = MutableStateFlow<String?>(null)
    private val _selectedYearMonth = MutableStateFlow(currentYearMonth())
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    private val _isSeeding = MutableStateFlow(false)
    val isSeeding: StateFlow<Boolean> = _isSeeding.asStateFlow()

    private val _receiptScanState = MutableStateFlow<ReceiptScanUiState>(ReceiptScanUiState.Idle)
    val receiptScanState: StateFlow<ReceiptScanUiState> = _receiptScanState.asStateFlow()

    private val _aiFutureState = MutableStateFlow<AiFutureUiState>(AiFutureUiState.Idle)
    val aiFutureState: StateFlow<AiFutureUiState> = _aiFutureState.asStateFlow()

    private val receiptSchema = Schema.obj(
        properties = mapOf(
            "clientName" to Schema.string(
                description = "Full name of the customer, client, payer, or vendor printed or handwritten on the receipt or invoice"
            ),
            "customerPhone" to Schema.string(
                description = "Customer or contact phone number if visible on the receipt or invoice, otherwise empty string"
            ),
            "amountPaid" to Schema.double(
                description = "The total amount paid or total invoice amount as a numeric value (strip currency symbols and commas)"
            ),
            "dateIso" to Schema.string(
                description = "Date shown on the receipt or invoice converted to YYYY-MM-DD format, or empty string if no date is visible"
            ),
            "pictureCount" to Schema.integer(
                description = "Number of pictures, prints, photos, or quantity of items on the receipt/invoice if specified, otherwise 1"
            ),
            "pricePerPicture" to Schema.double(
                description = "Unit price per picture or per item if itemized, otherwise equal to amountPaid"
            ),
            "paymentMethod" to Schema.enumeration(
                values = listOf("CASH", "BANK_TRANSFER"),
                description = "BANK_TRANSFER if receipt mentions transfer, bank, wire, POS, card, account, or electronic payment; otherwise CASH"
            ),
            "invoiceDetails" to Schema.string(
                description = "Concise summary of the receipt/invoice items, invoice number, package details, or expense description"
            )
        ),
        optionalProperties = listOf(
            "customerPhone",
            "dateIso",
            "pictureCount",
            "pricePerPicture",
            "invoiceDetails"
        )
    )

    private val aiFutureSchema = Schema.obj(
        properties = mapOf(
            "nextMonthProjectedRevenue" to Schema.double(
                description = "Realistic projected studio revenue for the upcoming month based on current rate per picture, recent shoots, and client pipeline"
            ),
            "threeMonthProjectedRevenue" to Schema.double(
                description = "Projected cumulative studio revenue over the next 3 months"
            ),
            "recommendedPricePerPicture" to Schema.double(
                description = "Recommended optimal price per picture to maximize revenue and reach studio target"
            ),
            "recommendedMonthlyTarget" to Schema.double(
                description = "Recommended achievable monthly target based on studio growth trajectory"
            ),
            "growthOutlookSummary" to Schema.string(
                description = "Clear 2-3 sentence executive summary of the studio's financial future, momentum, and revenue trajectory"
            ),
            "cashFlowOptimizationTip" to Schema.string(
                description = "Actionable advice on balancing Cash vs Bank Transfer payments, deposits, and per-picture upsells"
            ),
            "actionPlanSteps" to Schema.array(
                items = Schema.string(),
                description = "3 to 4 concrete, specific action steps for the photography studio owner to grow revenue next month"
            ),
            "clientFollowUpMessageDraft" to Schema.string(
                description = "Ready-to-send friendly SMS/WhatsApp message template to re-engage past photography clients for repeat shoots or extra prints"
            )
        )
    )

    private val receiptScannerModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = receiptSchema
            }
        )
    }

    private val aiFutureModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = aiFutureSchema
            }
        )
    }

    private val userAccountFlow = repository.observeUserAccount()
        .onStart { emit(null) }
        .catch { error ->
            handleStreamException("account profile", error)
            emit(null)
        }

    private val businessesFlow = repository.observeBusinesses()
        .onStart { emit(emptyList()) }
        .catch { error ->
            handleStreamException("businesses", error)
            emit(emptyList())
        }

    private val clientsFlow = repository.observeClients()
        .onStart { emit(emptyList()) }
        .catch { error ->
            handleStreamException("clients", error)
            emit(emptyList())
        }

    private val interactionsFlow = repository.observeInteractions()
        .onStart { emit(emptyList()) }
        .catch { error ->
            handleStreamException("interactions", error)
            emit(emptyList())
        }

    private val transactionsFlow = repository.observeTransactions()
        .onStart { emit(emptyList()) }
        .catch { error ->
            handleStreamException("transactions", error)
            emit(emptyList())
        }

    val uiState: StateFlow<StudioUiState> = combine(
        combine(userAccountFlow, businessesFlow) { acc, biz -> acc to biz },
        clientsFlow,
        interactionsFlow,
        transactionsFlow,
        combine(_selectedBusinessId, _selectedYearMonth, _errorMessage, _statusBannerMessage, _isSeeding) { bizId, ym, err, banner, seeding ->
            FilterMeta(bizId, ym, err, banner, seeding)
        }
    ) { (userAccount, businesses), allClients, allInteractions, allTransactions, meta ->
        val activeBusiness = businesses.find { it.id == meta.selectedBusinessId }
            ?: businesses.firstOrNull()

        val activeBizId = activeBusiness?.id ?: ""
        val clientsForBiz = allClients.filter { it.businessId == activeBizId }
        val interactionsForBiz = allInteractions.filter { it.businessId == activeBizId }
        val transactionsForBiz = allTransactions.filter { it.businessId == activeBizId }

        val currentYm = currentYearMonth()
        val distinctMonths = (transactionsForBiz.map { it.yearMonth } + currentYm + meta.selectedYearMonth)
            .filter { it.matches(Regex("^[0-9]{4}-[0-9]{2}$")) }
            .distinct()
            .sortedDescending()

        val report = buildMonthlyReport(
            yearMonth = meta.selectedYearMonth,
            monthlyTarget = activeBusiness?.monthlyTarget ?: 0.0,
            allBusinessTransactions = transactionsForBiz
        )

        StudioUiState(
            isLoading = false,
            userAccount = userAccount,
            businesses = businesses,
            activeBusiness = activeBusiness,
            clientsForBusiness = clientsForBiz,
            interactionsForBusiness = interactionsForBiz,
            transactionsForBusiness = transactionsForBiz,
            selectedYearMonth = meta.selectedYearMonth,
            availableYearMonths = distinctMonths,
            monthlyReport = report,
            errorMessage = meta.errorMessage,
            statusBannerMessage = meta.statusBannerMessage,
            isSeeding = meta.isSeeding
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = StudioUiState()
    )

    init {
        syncAuthenticatedUserAccount()
    }

    fun syncAuthenticatedUserAccount(
        customDisplayName: String? = null,
        customStudioRole: String? = null
    ) {
        val currentUser = Firebase.auth.currentUser ?: return
        viewModelScope.launch {
            val name = customDisplayName?.trim()?.takeIf { it.isNotEmpty() }
                ?: currentUser.displayName?.trim()?.takeIf { it.isNotEmpty() }
                ?: currentUser.email?.substringBefore("@")?.takeIf { it.isNotEmpty() }
                ?: "Studio Owner"
            val email = currentUser.email.orEmpty()
            val role = customStudioRole?.trim()?.takeIf { it.isNotEmpty() }
                ?: "Studio Owner & Photographer"
            repository.ensureOrSaveUserAccount(
                displayName = name,
                email = email,
                studioRole = role
            ).onSuccess {
                if (!customDisplayName.isNullOrBlank()) {
                    _statusBannerMessage.value = "Account profile saved for $name."
                }
            }.onFailure { e ->
                Log.w(TAG, "Failed to sync user account profile", e)
            }
        }
    }

    private fun handleStreamException(streamName: String, error: Throwable) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing $streamName", rootError)
        // Only surface error if user is still signed in
        if (Firebase.auth.currentUser != null) {
            _errorMessage.value = rootError.localizedMessage ?: "Failed to sync $streamName"
        }
    }

    fun selectBusiness(businessId: String) {
        _selectedBusinessId.value = businessId
    }

    fun selectYearMonth(yearMonth: String) {
        _selectedYearMonth.value = yearMonth
    }

    fun stepMonth(offset: Int) {
        val current = _selectedYearMonth.value
        val parts = current.split("-")
        if (parts.size == 2) {
            val year = parts[0].toIntOrNull() ?: return
            val month = parts[1].toIntOrNull() ?: return
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month - 1)
                set(Calendar.DAY_OF_MONTH, 1)
                add(Calendar.MONTH, offset)
            }
            val fmt = SimpleDateFormat("yyyy-MM", Locale.US)
            _selectedYearMonth.value = fmt.format(cal.time)
        }
    }

    fun clearBanner() {
        _statusBannerMessage.value = null
        _errorMessage.value = null
    }

    fun resetReceiptScanState() {
        _receiptScanState.value = ReceiptScanUiState.Idle
    }

    fun generateAiFutureForecast(customScenarioPrompt: String = "") {
        val state = uiState.value
        val activeBiz = state.activeBusiness
        val report = state.monthlyReport

        viewModelScope.launch {
            _aiFutureState.value = AiFutureUiState.Analyzing
            try {
                val studioName = activeBiz?.name ?: "Photography Studio"
                val category = activeBiz?.category ?: "Photography"
                val defaultRate = activeBiz?.defaultPricePerPicture ?: 15.0
                val monthlyTarget = activeBiz?.monthlyTarget ?: 3500.0
                val currentMonthEarnings = report.totalEarnings
                val currentPics = report.totalPictures
                val avgPrice = if (report.averagePricePerPicture > 0.0) report.averagePricePerPicture else defaultRate
                val cashPct = (report.cashFraction * 100).toInt()
                val bankPct = (report.bankTransferFraction * 100).toInt()
                val totalClients = state.clientsForBusiness.size
                val allTimeRevenue = state.transactionsForBusiness.sumOf { it.totalAmount }
                val allTimePics = state.transactionsForBusiness.sumOf { it.pictureCount }

                val prompt = buildString {
                    appendLine("You are an AI Financial Strategist and Studio Growth Advisor for a photography business.")
                    appendLine("Analyze this studio's current performance and generate a realistic AI Future Forecast & Growth Plan:")
                    appendLine("- Studio Name: $studioName ($category)")
                    appendLine("- Default Price Per Picture: ${formatCurrency(defaultRate)}")
                    appendLine("- Current Monthly Target: ${formatCurrency(monthlyTarget)}")
                    appendLine("- Selected Month (${report.displayMonthName}) Revenue: ${formatCurrency(currentMonthEarnings)} across ${report.totalTransactions} shoots ($currentPics pictures sold, Avg ${formatCurrency(avgPrice)}/pic)")
                    appendLine("- Payment Mix: $cashPct% Cash (${formatCurrency(report.cashEarnings)}) vs $bankPct% Bank Transfer (${formatCurrency(report.bankTransferEarnings)})")
                    appendLine("- Total Active CRM Clients: $totalClients")
                    appendLine("- All-Time Studio Revenue: ${formatCurrency(allTimeRevenue)} ($allTimePics total pictures)")
                    if (customScenarioPrompt.isNotBlank()) {
                        appendLine("- User's Custom Goal / Question for AI Future: \"$customScenarioPrompt\"")
                    }
                    appendLine("Provide realistic numeric projections for nextMonthProjectedRevenue, threeMonthProjectedRevenue, recommendedPricePerPicture, recommendedMonthlyTarget, a concise growthOutlookSummary, cashFlowOptimizationTip, 3-4 specific actionPlanSteps, and a ready-to-send clientFollowUpMessageDraft.")
                }

                val response = withTimeout(60_000L) {
                    aiFutureModel.generateContent(prompt)
                }

                val jsonString = response.text
                if (jsonString.isNullOrBlank()) {
                    _aiFutureState.value = AiFutureUiState.Error("AI model returned an empty forecast response.")
                    return@launch
                }

                val json = JSONObject(jsonString)
                val nextMonthRev = json.optDouble("nextMonthProjectedRevenue", (currentMonthEarnings * 1.2).coerceAtLeast(monthlyTarget))
                    .let { if (it.isNaN() || it.isInfinite()) monthlyTarget else it.coerceAtLeast(0.0) }
                val threeMonthRev = json.optDouble("threeMonthProjectedRevenue", nextMonthRev * 3.1)
                    .let { if (it.isNaN() || it.isInfinite()) nextMonthRev * 3.0 else it.coerceAtLeast(0.0) }
                val recPrice = json.optDouble("recommendedPricePerPicture", defaultRate * 1.15)
                    .let { if (it.isNaN() || it.isInfinite() || it <= 0.0) defaultRate else it }
                val recTarget = json.optDouble("recommendedMonthlyTarget", monthlyTarget * 1.2)
                    .let { if (it.isNaN() || it.isInfinite() || it <= 0.0) monthlyTarget else it }
                val outlook = json.optString("growthOutlookSummary", "").trim()
                val cashTip = json.optString("cashFlowOptimizationTip", "").trim()
                val stepsArray: JSONArray? = json.optJSONArray("actionPlanSteps")
                val steps = buildList {
                    if (stepsArray != null) {
                        for (i in 0 until stepsArray.length()) {
                            val item = stepsArray.optString(i, "").trim()
                            if (item.isNotEmpty()) add(item)
                        }
                    }
                }
                val draftMsg = json.optString("clientFollowUpMessageDraft", "").trim()

                _aiFutureState.value = AiFutureUiState.Success(
                    AiFutureForecastData(
                        nextMonthProjectedRevenue = nextMonthRev,
                        threeMonthProjectedRevenue = threeMonthRev,
                        recommendedPricePerPicture = recPrice,
                        recommendedMonthlyTarget = recTarget,
                        growthOutlookSummary = outlook,
                        cashFlowOptimizationTip = cashTip,
                        actionPlanSteps = steps,
                        clientFollowUpMessageDraft = draftMsg
                    )
                )
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI Future forecast timed out", e)
                _aiFutureState.value = AiFutureUiState.Error(
                    "AI Future analysis timed out after 60 seconds. Please tap 'Generate Forecast' to try again."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate AI Future forecast", e)
                _aiFutureState.value = AiFutureUiState.Error(
                    e.localizedMessage ?: "Failed to generate AI Future forecast."
                )
            }
        }
    }

    fun applyAiRecommendedPricingAndTarget(recommendedPricePerPic: Double, recommendedMonthlyTarget: Double) {
        val activeBiz = uiState.value.activeBusiness ?: return
        updateBusinessProfile(
            business = activeBiz,
            name = activeBiz.name,
            category = activeBiz.category,
            defaultPricePerPicture = recommendedPricePerPic,
            monthlyTarget = recommendedMonthlyTarget,
            accentColorHex = activeBiz.accentColorHex
        )
    }

    fun scanReceiptOrInvoice(bitmap: Bitmap) {
        viewModelScope.launch {
            _receiptScanState.value = ReceiptScanUiState.Scanning
            try {
                val scaledBitmap = scaleBitmapForAi(bitmap, maxDimension = 1600)
                val response = withTimeout(60_000L) {
                    receiptScannerModel.generateContent(
                        content {
                            image(scaledBitmap)
                            text(
                                "Analyze this receipt, invoice, or payment slip photo. " +
                                    "Extract the client/customer or vendor name, phone number (if present), " +
                                    "the total amount paid, the transaction date in YYYY-MM-DD format, " +
                                    "the number of pictures/items (default to 1 if not itemized), " +
                                    "unit price per picture/item, payment method (CASH or BANK_TRANSFER), " +
                                    "and a concise summary of the invoice details."
                            )
                        }
                    )
                }

                val jsonString = response.text
                if (jsonString.isNullOrBlank()) {
                    _receiptScanState.value = ReceiptScanUiState.Error("Model returned an empty response.")
                    return@launch
                }

                val json = JSONObject(jsonString)
                val scannedName = json.optString("clientName", "").trim()
                val scannedPhone = json.optString("customerPhone", "").trim()
                val amountPaid = json.optDouble("amountPaid", 0.0).let {
                    if (it.isNaN() || it.isInfinite()) 0.0 else it.coerceAtLeast(0.0)
                }
                val dateIso = json.optString("dateIso", "").trim()
                val parsedDateMillis = parseIsoDateOrToday(dateIso)
                val rawPicCount = json.optLong("pictureCount", 1L).coerceAtLeast(1L)
                val rawPricePerPic = json.optDouble("pricePerPicture", 0.0).let {
                    if (it.isNaN() || it.isInfinite()) 0.0 else it.coerceAtLeast(0.0)
                }
                val computedPricePerPic = when {
                    amountPaid > 0.0 && rawPicCount >= 1L -> amountPaid / rawPicCount.toDouble()
                    rawPricePerPic > 0.0 -> rawPricePerPic
                    else -> uiState.value.activeBusiness?.defaultPricePerPicture ?: 15.0
                }
                val computedAmountPaid = if (amountPaid > 0.0) amountPaid else (rawPicCount * computedPricePerPic)
                val methodRaw = json.optString("paymentMethod", "CASH").uppercase(Locale.US)
                val paymentMethod = if (methodRaw == "BANK_TRANSFER") "BANK_TRANSFER" else "CASH"
                val details = json.optString("invoiceDetails", "").trim()

                val data = ScannedReceiptData(
                    clientName = scannedName,
                    customerPhone = scannedPhone,
                    amountPaid = computedAmountPaid,
                    transactionDateMillis = parsedDateMillis,
                    dateIso = dateIso,
                    pictureCount = rawPicCount,
                    pricePerPicture = computedPricePerPic,
                    paymentMethod = paymentMethod,
                    invoiceDetails = details
                )
                _receiptScanState.value = ReceiptScanUiState.Success(data)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Receipt scanning timed out", e)
                _receiptScanState.value = ReceiptScanUiState.Error(
                    "Receipt scan timed out after 60 seconds. Please try again with a clearer photo."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to scan receipt with Firebase AI", e)
                _receiptScanState.value = ReceiptScanUiState.Error(
                    e.localizedMessage ?: "Failed to scan receipt."
                )
            }
        }
    }

    private fun scaleBitmapForAi(source: Bitmap, maxDimension: Int): Bitmap {
        val width = source.width
        val height = source.height
        if (width <= maxDimension && height <= maxDimension) return source
        val ratio = width.toFloat() / height.toFloat()
        val (targetWidth, targetHeight) = if (width >= height) {
            maxDimension to (maxDimension / ratio).toInt().coerceAtLeast(1)
        } else {
            (maxDimension * ratio).toInt().coerceAtLeast(1) to maxDimension
        }
        return Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
    }

    private fun parseIsoDateOrToday(dateIso: String): Long {
        if (dateIso.isBlank()) return System.currentTimeMillis()
        val formats = listOf("yyyy-MM-dd", "yyyy/MM/dd", "MM/dd/yyyy", "dd/MM/yyyy")
        for (pattern in formats) {
            try {
                val parser = SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }
                val parsed = parser.parse(dateIso)
                if (parsed != null) return parsed.time
            } catch (_: Exception) {
                // try next pattern
            }
        }
        return System.currentTimeMillis()
    }

    fun createBusinessProfile(
        name: String,
        category: String,
        defaultPricePerPicture: Double,
        monthlyTarget: Double,
        accentColorHex: String
    ) {
        if (name.isBlank()) {
            _errorMessage.value = "Please enter a business name."
            return
        }
        viewModelScope.launch {
            val result = repository.createBusiness(
                name = name,
                category = category,
                defaultPricePerPicture = defaultPricePerPicture,
                monthlyTarget = monthlyTarget,
                accentColorHex = accentColorHex
            )
            result.onSuccess { newId ->
                _selectedBusinessId.value = newId
                _statusBannerMessage.value = "Studio \"$name\" created and active."
            }.onFailure { e ->
                _errorMessage.value = e.localizedMessage ?: "Could not create business profile."
            }
        }
    }

    fun updateBusinessProfile(
        business: BusinessProfile,
        name: String,
        category: String,
        defaultPricePerPicture: Double,
        monthlyTarget: Double,
        accentColorHex: String
    ) {
        if (name.isBlank()) {
            _errorMessage.value = "Business name cannot be empty."
            return
        }
        viewModelScope.launch {
            val updated = business.copy(
                name = name.trim(),
                category = category.trim().ifEmpty { "Photography Studio" },
                defaultPricePerPicture = defaultPricePerPicture.coerceAtLeast(0.0),
                monthlyTarget = monthlyTarget.coerceAtLeast(0.0),
                accentColorHex = accentColorHex
            )
            repository.updateBusiness(updated)
                .onSuccess {
                    _statusBannerMessage.value = "Updated settings for \"${updated.name}\"."
                }
                .onFailure { e ->
                    _errorMessage.value = e.localizedMessage ?: "Failed to update studio settings."
                }
        }
    }

    fun deleteBusinessProfile(businessId: String) {
        viewModelScope.launch {
            repository.deleteBusiness(businessId)
                .onSuccess {
                    _statusBannerMessage.value = "Business profile removed."
                }
                .onFailure { e ->
                    _errorMessage.value = e.localizedMessage ?: "Could not delete business profile."
                }
        }
    }

    fun createClientProfile(
        name: String,
        phoneNumber: String,
        email: String,
        notes: String,
        initialInteractionSummary: String = ""
    ) {
        val activeBiz = uiState.value.activeBusiness
        if (activeBiz == null) {
            _errorMessage.value = "Please create or select a business profile first."
            return
        }
        if (name.isBlank() || phoneNumber.isBlank()) {
            _errorMessage.value = "Client name and phone number are required."
            return
        }
        viewModelScope.launch {
            repository.createClient(
                businessId = activeBiz.id,
                name = name,
                phoneNumber = phoneNumber,
                email = email,
                notes = notes
            ).onSuccess { clientId ->
                if (initialInteractionSummary.isNotBlank()) {
                    repository.createInteraction(
                        businessId = activeBiz.id,
                        clientId = clientId,
                        clientName = name,
                        type = "INQUIRY",
                        summary = initialInteractionSummary
                    )
                }
                _statusBannerMessage.value = "Added $name to ${activeBiz.name} CRM."
            }.onFailure { e ->
                _errorMessage.value = e.localizedMessage ?: "Failed to save client."
            }
        }
    }

    fun updateClientProfile(
        client: ClientProfile,
        name: String,
        phoneNumber: String,
        email: String,
        notes: String
    ) {
        if (name.isBlank() || phoneNumber.isBlank()) {
            _errorMessage.value = "Client name and phone number cannot be empty."
            return
        }
        viewModelScope.launch {
            repository.updateClient(
                client.copy(
                    name = name.trim(),
                    phoneNumber = phoneNumber.trim(),
                    email = email.trim(),
                    notes = notes.trim()
                )
            ).onSuccess {
                _statusBannerMessage.value = "Updated client profile for ${name.trim()}."
            }.onFailure { e ->
                _errorMessage.value = e.localizedMessage ?: "Failed to update client."
            }
        }
    }

    fun deleteClientProfile(clientId: String) {
        viewModelScope.launch {
            repository.deleteClient(clientId)
                .onSuccess {
                    _statusBannerMessage.value = "Client deleted."
                }
                .onFailure { e ->
                    _errorMessage.value = e.localizedMessage ?: "Could not delete client."
                }
        }
    }

    fun logClientInteraction(
        client: ClientProfile,
        type: String,
        summary: String,
        interactionDateMillis: Long = System.currentTimeMillis()
    ) {
        if (summary.isBlank()) {
            _errorMessage.value = "Please enter an interaction note."
            return
        }
        viewModelScope.launch {
            repository.createInteraction(
                businessId = client.businessId,
                clientId = client.id,
                clientName = client.name,
                type = type,
                summary = summary,
                interactionDateMillis = interactionDateMillis
            ).onSuccess {
                _statusBannerMessage.value = "Interaction logged for ${client.name}."
            }.onFailure { e ->
                _errorMessage.value = e.localizedMessage ?: "Failed to log interaction."
            }
        }
    }

    fun deleteClientInteraction(interactionId: String) {
        viewModelScope.launch {
            repository.deleteInteraction(interactionId)
                .onFailure { e ->
                    _errorMessage.value = e.localizedMessage ?: "Could not delete interaction log."
                }
        }
    }

    fun recordTransaction(
        customerPhone: String,
        clientName: String,
        selectedClientId: String,
        transactionDateMillis: Long,
        pictureCount: Long,
        pricePerPicture: Double,
        paymentMethod: String,
        notes: String,
        saveNewClientToCrm: Boolean,
        totalAmountOverride: Double? = null
    ) {
        val activeBiz = uiState.value.activeBusiness
        if (activeBiz == null) {
            _errorMessage.value = "Create or select a business profile before recording transactions."
            return
        }
        if (customerPhone.isBlank() && clientName.isBlank()) {
            _errorMessage.value = "Please enter a customer name or phone number."
            return
        }
        if (pictureCount < 1L) {
            _errorMessage.value = "Number of pictures must be at least 1."
            return
        }
        if (pricePerPicture < 0.0) {
            _errorMessage.value = "Price per picture cannot be negative."
            return
        }

        viewModelScope.launch {
            var resolvedClientId = selectedClientId
            val existingClient = uiState.value.clientsForBusiness.find {
                (selectedClientId.isNotBlank() && it.id == selectedClientId) ||
                    (customerPhone.isNotBlank() && it.phoneNumber.equals(customerPhone.trim(), ignoreCase = true)) ||
                    (clientName.isNotBlank() && it.name.equals(clientName.trim(), ignoreCase = true))
            }
            val finalPhone = customerPhone.trim().ifEmpty {
                existingClient?.phoneNumber ?: "No Phone"
            }
            val finalClientName = clientName.trim().ifEmpty {
                existingClient?.name ?: "Customer ($finalPhone)"
            }

            if (existingClient != null) {
                resolvedClientId = existingClient.id
            } else if (saveNewClientToCrm) {
                val createdId = repository.createClient(
                    businessId = activeBiz.id,
                    name = finalClientName,
                    phoneNumber = finalPhone,
                    email = "",
                    notes = notes.trim().ifEmpty { "Created from transaction entry" }
                ).getOrNull()
                if (createdId != null) {
                    resolvedClientId = createdId
                }
            }

            val finalTotal = (totalAmountOverride ?: (pictureCount * pricePerPicture)).coerceAtLeast(0.0)
            val effectivePricePerPic = if (pictureCount > 0L) finalTotal / pictureCount.toDouble() else pricePerPicture

            repository.createTransaction(
                businessId = activeBiz.id,
                clientId = resolvedClientId,
                clientName = finalClientName,
                customerPhone = finalPhone,
                transactionDateMillis = transactionDateMillis,
                pictureCount = pictureCount,
                pricePerPicture = effectivePricePerPic,
                paymentMethod = paymentMethod,
                notes = notes,
                totalAmountOverride = finalTotal
            ).onSuccess {
                val ym = SimpleDateFormat("yyyy-MM", Locale.US).format(Date(transactionDateMillis))
                _selectedYearMonth.value = ym
                val totalFormatted = formatCurrency(finalTotal)
                _statusBannerMessage.value = "Recorded $totalFormatted ($pictureCount pics) for $finalClientName."
            }.onFailure { e ->
                _errorMessage.value = e.localizedMessage ?: "Failed to record transaction."
            }
        }
    }

    fun updateTransaction(
        existingTransaction: PhotoTransaction,
        customerPhone: String,
        clientName: String,
        selectedClientId: String,
        transactionDateMillis: Long,
        pictureCount: Long,
        pricePerPicture: Double,
        totalAmount: Double,
        paymentMethod: String,
        notes: String
    ) {
        if (customerPhone.isBlank() && clientName.isBlank()) {
            _errorMessage.value = "Customer name or phone number cannot be empty."
            return
        }
        if (pictureCount < 1L) {
            _errorMessage.value = "Number of pictures must be at least 1."
            return
        }
        if (totalAmount < 0.0) {
            _errorMessage.value = "Total amount paid cannot be negative."
            return
        }

        viewModelScope.launch {
            val safeCount = pictureCount.coerceAtLeast(1L)
            val safeTotal = totalAmount.coerceAtLeast(0.0)
            val effectivePricePerPic = if (safeCount > 0L) {
                safeTotal / safeCount.toDouble()
            } else {
                pricePerPicture.coerceAtLeast(0.0)
            }
            val safePhone = customerPhone.trim().ifEmpty { "No Phone" }
            val safeName = clientName.trim().ifEmpty { safePhone }

            val updatedTx = existingTransaction.copy(
                clientId = selectedClientId.trim(),
                clientName = safeName,
                customerPhone = safePhone,
                transactionDateMillis = transactionDateMillis,
                pictureCount = safeCount,
                pricePerPicture = effectivePricePerPic,
                totalAmount = safeTotal,
                paymentMethod = paymentMethod,
                notes = notes.trim()
            )

            repository.updateTransaction(updatedTx)
                .onSuccess {
                    val ym = SimpleDateFormat("yyyy-MM", Locale.US).format(Date(transactionDateMillis))
                    _selectedYearMonth.value = ym
                    _statusBannerMessage.value =
                        "Adjusted transaction for $safeName — Total Paid: ${formatCurrency(safeTotal)} ($safeCount pics)."
                }
                .onFailure { e ->
                    _errorMessage.value = e.localizedMessage ?: "Failed to update transaction."
                }
        }
    }

    fun deleteTransaction(transactionId: String) {
        viewModelScope.launch {
            repository.deleteTransaction(transactionId)
                .onSuccess {
                    _statusBannerMessage.value = "Transaction removed."
                }
                .onFailure { e ->
                    _errorMessage.value = e.localizedMessage ?: "Failed to delete transaction."
                }
        }
    }

    fun seedStarterStudios() {
        if (_isSeeding.value) return
        viewModelScope.launch {
            _isSeeding.value = true
            try {
                val now = System.currentTimeMillis()
                val dayMs = 86_400_000L

                val weddingBizId = repository.createBusiness(
                    name = "NAS Wedding Studio",
                    category = "Weddings & Fine Art",
                    defaultPricePerPicture = 15.0,
                    monthlyTarget = 3500.0,
                    accentColorHex = "#6366F1"
                ).getOrThrow()

                val c1Id = repository.createClient(
                    businessId = weddingBizId,
                    name = "Amira & Tariq Al-Mansoor",
                    phoneNumber = "+1 555-392-8410",
                    email = "amira.tariq@example.com",
                    notes = "Full-day ballroom + outdoor golden hour portraits."
                ).getOrThrow()

                val c2Id = repository.createClient(
                    businessId = weddingBizId,
                    name = "Clara Vance",
                    phoneNumber = "+1 555-614-9022",
                    email = "clara.vance@example.com",
                    notes = "Engagement session & framed fine-art prints."
                ).getOrThrow()

                repository.createInteraction(
                    businessId = weddingBizId,
                    clientId = c1Id,
                    clientName = "Amira & Tariq Al-Mansoor",
                    type = "SHOOT",
                    summary = "Completed 4-hour ceremony and family portrait session. 120 selects chosen.",
                    interactionDateMillis = now - 5 * dayMs
                )
                repository.createInteraction(
                    businessId = weddingBizId,
                    clientId = c1Id,
                    clientName = "Amira & Tariq Al-Mansoor",
                    type = "DELIVERY",
                    summary = "Delivered retouched online gallery and received full bank transfer.",
                    interactionDateMillis = now - 2 * dayMs
                )
                repository.createInteraction(
                    businessId = weddingBizId,
                    clientId = c2Id,
                    clientName = "Clara Vance",
                    type = "CALL",
                    summary = "Confirmed sunset beach location and wardrobe styling.",
                    interactionDateMillis = now - 1 * dayMs
                )

                repository.createTransaction(
                    businessId = weddingBizId,
                    clientId = c1Id,
                    clientName = "Amira & Tariq Al-Mansoor",
                    customerPhone = "+1 555-392-8410",
                    transactionDateMillis = now - 5 * dayMs,
                    pictureCount = 120L,
                    pricePerPicture = 15.0,
                    paymentMethod = "BANK_TRANSFER",
                    notes = "Wedding highlight gallery (120 retouched photos)"
                )
                repository.createTransaction(
                    businessId = weddingBizId,
                    clientId = c2Id,
                    clientName = "Clara Vance",
                    customerPhone = "+1 555-614-9022",
                    transactionDateMillis = now - 1 * dayMs,
                    pictureCount = 35L,
                    pricePerPicture = 15.0,
                    paymentMethod = "CASH",
                    notes = "Engagement portrait package paid in cash"
                )

                val portraitBizId = repository.createBusiness(
                    name = "NAS Portrait Lab",
                    category = "Studio Headshots & Branding",
                    defaultPricePerPicture = 25.0,
                    monthlyTarget = 2500.0,
                    accentColorHex = "#10B981"
                ).getOrThrow()

                val c3Id = repository.createClient(
                    businessId = portraitBizId,
                    name = "Marcus Sterling",
                    phoneNumber = "+1 555-741-3309",
                    email = "m.sterling@example.com",
                    notes = "Executive team headshots & LinkedIn portraits."
                ).getOrThrow()

                repository.createInteraction(
                    businessId = portraitBizId,
                    clientId = c3Id,
                    clientName = "Marcus Sterling",
                    type = "SHOOT",
                    summary = "Studio lighting session for 24 high-res executive portraits.",
                    interactionDateMillis = now - 3 * dayMs
                )

                repository.createTransaction(
                    businessId = portraitBizId,
                    clientId = c3Id,
                    clientName = "Marcus Sterling",
                    customerPhone = "+1 555-741-3309",
                    transactionDateMillis = now - 3 * dayMs,
                    pictureCount = 24L,
                    pricePerPicture = 25.0,
                    paymentMethod = "BANK_TRANSFER",
                    notes = "Corporate portrait session"
                )

                _selectedBusinessId.value = weddingBizId
                _statusBannerMessage.value = "Loaded NAS Wedding Studio & NAS Portrait Lab sample profiles!"
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to load starter studios."
            } finally {
                _isSeeding.value = false
            }
        }
    }

    fun formatShareableMonthlyReport(
        business: BusinessProfile?,
        report: MonthlyReportSummary
    ): String {
        val studioName = business?.name ?: "NAS Finance Studio"
        val studioCategory = business?.category ?: "Photography Business"
        val dateFmt = SimpleDateFormat("MMM dd, yyyy", Locale.US)

        val sb = StringBuilder()
        sb.appendLine("========================================")
        sb.appendLine("NAS FINANCE — MONTHLY ACCOUNTING REPORT")
        sb.appendLine("========================================")
        sb.appendLine("Business Profile : $studioName ($studioCategory)")
        sb.appendLine("Report Period    : ${report.displayMonthName} (${report.yearMonth})")
        sb.appendLine("----------------------------------------")
        sb.appendLine("SUMMARY METRICS")
        sb.appendLine("• Total Earnings      : ${formatCurrency(report.totalEarnings)}")
        sb.appendLine("• Monthly Target      : ${formatCurrency(report.monthlyTarget)} (${(report.targetProgressFraction * 100).toInt()}%)")
        sb.appendLine("• Total Transactions  : ${report.totalTransactions}")
        sb.appendLine("• Total Pictures Sold : ${report.totalPictures} pics")
        sb.appendLine("• Avg Price / Picture : ${formatCurrency(report.averagePricePerPicture)}")
        sb.appendLine("----------------------------------------")
        sb.appendLine("PAYMENT METHOD BREAKDOWN")
        sb.appendLine(
            "• Cash Payments       : ${formatCurrency(report.cashEarnings)} " +
                "(${report.cashTransactionCount} tx, ${(report.cashFraction * 100).toInt()}%)"
        )
        sb.appendLine(
            "• Bank Transfers      : ${formatCurrency(report.bankTransferEarnings)} " +
                "(${report.bankTransferTransactionCount} tx, ${(report.bankTransferFraction * 100).toInt()}%)"
        )
        sb.appendLine("----------------------------------------")
        sb.appendLine("TRANSACTIONS LEDGER (CSV FORMAT)")
        sb.appendLine("Date,Customer Name,Phone,Pictures,Price/Pic,Total,Method")
        report.transactions.forEach { tx ->
            val dateStr = dateFmt.format(Date(tx.transactionDateMillis))
            val methodLabel = if (tx.paymentMethod == "BANK_TRANSFER") "Bank Transfer" else "Cash"
            sb.appendLine(
                "\"$dateStr\",\"${tx.clientName}\",\"${tx.customerPhone}\",${tx.pictureCount},${tx.pricePerPicture},${tx.totalAmount},\"$methodLabel\""
            )
        }
        sb.appendLine("========================================")
        return sb.toString()
    }

    private data class FilterMeta(
        val selectedBusinessId: String?,
        val selectedYearMonth: String,
        val errorMessage: String?,
        val statusBannerMessage: String?,
        val isSeeding: Boolean
    )

    companion object {
        private const val TAG = "StudioViewModel"

        fun currentYearMonth(): String {
            return SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
        }

        fun formatDisplayMonth(yearMonth: String): String {
            return try {
                val parser = SimpleDateFormat("yyyy-MM", Locale.US)
                val parsed = parser.parse(yearMonth) ?: Date()
                SimpleDateFormat("MMMM yyyy", Locale.US).format(parsed)
            } catch (e: Exception) {
                yearMonth
            }
        }

        fun formatCurrency(amount: Double): String {
            return NumberFormat.getCurrencyInstance(Locale.US).format(amount)
        }

        fun emptyMonthlyReport(yearMonth: String): MonthlyReportSummary {
            return MonthlyReportSummary(
                yearMonth = yearMonth,
                displayMonthName = formatDisplayMonth(yearMonth),
                totalEarnings = 0.0,
                cashEarnings = 0.0,
                bankTransferEarnings = 0.0,
                cashTransactionCount = 0,
                bankTransferTransactionCount = 0,
                totalTransactions = 0,
                totalPictures = 0L,
                averagePricePerPicture = 0.0,
                monthlyTarget = 0.0,
                targetProgressFraction = 0f,
                cashFraction = 0f,
                bankTransferFraction = 0f,
                weeklyBuckets = listOf(
                    WeeklyBucket("01-07", 0.0, 0.0, 0.0, 0L),
                    WeeklyBucket("08-14", 0.0, 0.0, 0.0, 0L),
                    WeeklyBucket("15-21", 0.0, 0.0, 0.0, 0L),
                    WeeklyBucket("22-28", 0.0, 0.0, 0.0, 0L),
                    WeeklyBucket("29-31", 0.0, 0.0, 0.0, 0L)
                ),
                transactions = emptyList()
            )
        }

        fun buildMonthlyReport(
            yearMonth: String,
            monthlyTarget: Double,
            allBusinessTransactions: List<PhotoTransaction>
        ): MonthlyReportSummary {
            val monthTxs = allBusinessTransactions
                .filter { it.yearMonth == yearMonth }
                .sortedByDescending { it.transactionDateMillis }

            val cashTxs = monthTxs.filter { it.paymentMethod == "CASH" }
            val bankTxs = monthTxs.filter { it.paymentMethod == "BANK_TRANSFER" }

            val cashTotal = cashTxs.sumOf { it.totalAmount }
            val bankTotal = bankTxs.sumOf { it.totalAmount }
            val totalEarnings = cashTotal + bankTotal
            val totalPics = monthTxs.sumOf { it.pictureCount }
            val avgPrice = if (totalPics > 0L) totalEarnings / totalPics.toDouble() else 0.0

            val targetFraction = if (monthlyTarget > 0.0) {
                (totalEarnings / monthlyTarget).toFloat().coerceIn(0f, 1.5f)
            } else {
                0f
            }

            val cashFrac = if (totalEarnings > 0.0) (cashTotal / totalEarnings).toFloat() else 0f
            val bankFrac = if (totalEarnings > 0.0) (bankTotal / totalEarnings).toFloat() else 0f

            val bucketDefs = listOf(
                Triple("01-07", 1, 7),
                Triple("08-14", 8, 14),
                Triple("15-21", 15, 21),
                Triple("22-28", 22, 28),
                Triple("29-31", 29, 31)
            )
            val cal = Calendar.getInstance()
            val buckets = bucketDefs.map { (label, minDay, maxDay) ->
                val inBucket = monthTxs.filter { tx ->
                    cal.timeInMillis = tx.transactionDateMillis
                    val day = cal.get(Calendar.DAY_OF_MONTH)
                    day in minDay..maxDay
                }
                val bCash = inBucket.filter { it.paymentMethod == "CASH" }.sumOf { it.totalAmount }
                val bBank = inBucket.filter { it.paymentMethod == "BANK_TRANSFER" }.sumOf { it.totalAmount }
                WeeklyBucket(
                    label = label,
                    cashAmount = bCash,
                    bankTransferAmount = bBank,
                    totalAmount = bCash + bBank,
                    pictureCount = inBucket.sumOf { it.pictureCount }
                )
            }

            return MonthlyReportSummary(
                yearMonth = yearMonth,
                displayMonthName = formatDisplayMonth(yearMonth),
                totalEarnings = totalEarnings,
                cashEarnings = cashTotal,
                bankTransferEarnings = bankTotal,
                cashTransactionCount = cashTxs.size,
                bankTransferTransactionCount = bankTxs.size,
                totalTransactions = monthTxs.size,
                totalPictures = totalPics,
                averagePricePerPicture = avgPrice,
                monthlyTarget = monthlyTarget,
                targetProgressFraction = targetFraction,
                cashFraction = cashFrac,
                bankTransferFraction = bankFrac,
                weeklyBuckets = buckets,
                transactions = monthTxs
            )
        }
    }
}
