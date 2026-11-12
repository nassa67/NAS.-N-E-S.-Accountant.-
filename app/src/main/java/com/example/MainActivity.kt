package com.example

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PeopleAlt
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PeopleAlt
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.model.ClientProfile
import com.example.data.model.PhotoTransaction
import com.example.data.repository.StudioRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.PendingRegistrationStore
import com.example.ui.auth.authStateFlow
import com.example.ui.auth.signOut
import com.example.ui.components.BusinessSwitcherBottomSheet
import com.example.ui.components.RecordTransactionBottomSheet
import com.example.ui.components.TopStudioHeader
import com.example.ui.screens.CrmClientsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.MonthlyReportsScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.NasCrimson
import com.example.ui.theme.NasCrimsonSoft
import com.example.ui.theme.NasEmerald
import com.example.ui.theme.NasEmeraldSoft
import com.example.ui.theme.NasFinanceTheme
import com.example.ui.theme.NasIndigo
import com.example.ui.theme.NasIndigoSoft
import com.example.ui.theme.NasSlate600
import com.example.ui.theme.NasWhite
import com.example.ui.viewmodel.StudioViewModel
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

private const val TAG = "AppCheckInit"
private const val DEBUG_TOKEN_EXTRA = "FIREBASE_APPCHECK_DEBUG_TOKEN"
private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
private const val RECAPTCHA_SITE_KEY_RES = "recaptcha_site_key"

fun configureAppCheck(context: Context, intent: Intent?) {
    try {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }

        if (BuildConfig.DEBUG) {
            val debugToken = intent?.getStringExtra(DEBUG_TOKEN_EXTRA)

            if (!debugToken.isNullOrEmpty()) {
                val persistenceKey = FirebaseApp.getInstance().persistenceKey
                val prefs = context.getSharedPreferences(
                    "$DEBUG_STORE_PREFIX$persistenceKey",
                    Context.MODE_PRIVATE
                )
                prefs.edit()
                    .putString(DEBUG_SECRET_KEY, debugToken)
                    .commit()
                Log.d(TAG, "App Check debug secret configured successfully.")
            } else {
                Log.w(TAG, "No debug token found in Intent extra.")
            }

            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
            Log.d(TAG, "DebugAppCheckProviderFactory installed.")
        } else {
            val resId = context.resources.getIdentifier(RECAPTCHA_SITE_KEY_RES, "string", context.packageName)
            if (resId != 0) {
                val siteKey = context.getString(resId)
                if (siteKey.isNotBlank()) {
                    FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                        RecaptchaAppCheckProviderFactory.getInstance(siteKey)
                    )
                    Log.d(TAG, "RecaptchaAppCheckProviderFactory installed.")
                } else {
                    Log.e(TAG, "recaptcha_site_key resource string is blank.")
                }
            } else {
                Log.e(TAG, "recaptcha_site_key string resource not found in package.")
            }
        }
    } catch (t: Throwable) {
        Log.e(TAG, "Failed to initialize Firebase App Check", t)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            NasFinanceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NasFinanceAppRoot()
                }
            }
        }
    }
}

@Composable
fun NasFinanceAppRoot(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    val user = currentUser
    if (user == null) {
        AuthScreen(
            onAuthSuccess = { /* authStateFlow automatically emits the signed-in user */ }
        )
    } else {
        key(user.uid) {
            AuthenticatedStudioShell(currentUserId = user.uid)
        }
    }
}

@Composable
fun AuthenticatedStudioShell(currentUserId: String) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val viewModel: StudioViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) as Application
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                StudioViewModel(StudioRepository(db))
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val receiptScanState by viewModel.receiptScanState.collectAsStateWithLifecycle()
    val aiFutureState by viewModel.aiFutureState.collectAsStateWithLifecycle()

    LaunchedEffect(currentUserId) {
        val pendingName = PendingRegistrationStore.pendingDisplayName
        val pendingRole = PendingRegistrationStore.pendingStudioRole
        val pendingStudio = PendingRegistrationStore.pendingInitialStudioName
        if (pendingName != null || pendingRole != null) {
            viewModel.syncAuthenticatedUserAccount(
                customDisplayName = pendingName,
                customStudioRole = pendingRole
            )
            PendingRegistrationStore.pendingDisplayName = null
            PendingRegistrationStore.pendingStudioRole = null
        }
        if (!pendingStudio.isNullOrBlank()) {
            viewModel.createBusinessProfile(
                name = pendingStudio,
                category = "Photography Studio",
                defaultPricePerPicture = 15.0,
                monthlyTarget = 3500.0,
                accentColorHex = "#10B981"
            )
            PendingRegistrationStore.pendingInitialStudioName = null
        }
    }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Home, 1: Trends/Reports, 2: Ledger, 3: CRM
    var showBusinessSwitcher by remember { mutableStateOf(false) }
    var showRecordTransactionSheet by remember { mutableStateOf(false) }
    var preselectedClientForTransaction by remember { mutableStateOf<ClientProfile?>(null) }
    var editingTransaction by remember { mutableStateOf<PhotoTransaction?>(null) }

    if (selectedTab != 0) {
        BackHandler {
            selectedTab = 0
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        topBar = {
            Column {
                TopStudioHeader(
                    activeBusiness = uiState.activeBusiness,
                    businessCount = uiState.businesses.size,
                    onOpenBusinessSwitcher = { showBusinessSwitcher = true },
                    onSignOutClick = {
                        signOut(
                            credentialManager = credentialManager,
                            scope = coroutineScope
                        )
                    }
                )

                val bannerMsg = uiState.errorMessage ?: uiState.statusBannerMessage
                if (bannerMsg != null) {
                    val isError = uiState.errorMessage != null
                    Surface(
                        color = if (isError) NasCrimsonSoft else NasEmeraldSoft,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = bannerMsg,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isError) NasCrimson else NasEmerald,
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .padding(end = 28.dp)
                            )
                            IconButton(
                                onClick = { viewModel.clearBanner() },
                                modifier = Modifier.align(Alignment.CenterEnd)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss Banner",
                                    tint = if (isError) NasCrimson else NasEmerald
                                )
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (uiState.businesses.isNotEmpty()) {
                FloatingActionButton(
                    onClick = {
                        editingTransaction = null
                        preselectedClientForTransaction = null
                        viewModel.resetReceiptScanState()
                        showRecordTransactionSheet = true
                    },
                    containerColor = NasEmerald,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.testTag("fab_record_transaction")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Record Transaction")
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = NasWhite,
                tonalElevation = 4.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 0) Icons.Filled.Home else Icons.Outlined.Home,
                            contentDescription = "Home"
                        )
                    },
                    label = { Text("Home") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NasIndigo,
                        selectedTextColor = NasIndigo,
                        indicatorColor = NasIndigoSoft,
                        unselectedIconColor = NasSlate600,
                        unselectedTextColor = NasSlate600
                    ),
                    modifier = Modifier.testTag("nav_home")
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 1) Icons.Filled.Analytics else Icons.Outlined.Analytics,
                            contentDescription = "Reports"
                        )
                    },
                    label = { Text("Reports") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NasIndigo,
                        selectedTextColor = NasIndigo,
                        indicatorColor = NasIndigoSoft,
                        unselectedIconColor = NasSlate600,
                        unselectedTextColor = NasSlate600
                    ),
                    modifier = Modifier.testTag("nav_reports")
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 2) Icons.AutoMirrored.Filled.ReceiptLong else Icons.AutoMirrored.Outlined.ReceiptLong,
                            contentDescription = "Payments"
                        )
                    },
                    label = { Text("Payments") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NasIndigo,
                        selectedTextColor = NasIndigo,
                        indicatorColor = NasIndigoSoft,
                        unselectedIconColor = NasSlate600,
                        unselectedTextColor = NasSlate600
                    ),
                    modifier = Modifier.testTag("nav_payments")
                )

                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 3) Icons.Filled.PeopleAlt else Icons.Outlined.PeopleAlt,
                            contentDescription = "CRM"
                        )
                    },
                    label = { Text("CRM") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NasIndigo,
                        selectedTextColor = NasIndigo,
                        indicatorColor = NasIndigoSoft,
                        unselectedIconColor = NasSlate600,
                        unselectedTextColor = NasSlate600
                    ),
                    modifier = Modifier.testTag("nav_crm")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    (fadeIn(tween(220)) + slideInHorizontally(tween(260)) { fullWidth ->
                        if (targetState > initialState) fullWidth / 6 else -fullWidth / 6
                    }) togetherWith (fadeOut(tween(180)) + slideOutHorizontally(tween(220)) { fullWidth ->
                        if (targetState > initialState) -fullWidth / 6 else fullWidth / 6
                    })
                },
                label = "main_tab_transition"
            ) { tab ->
                when (tab) {
                    0 -> DashboardScreen(
                        uiState = uiState,
                        aiFutureState = aiFutureState,
                        onGenerateAiFuture = { prompt -> viewModel.generateAiFutureForecast(prompt) },
                        onApplyAiRecommendations = { recPrice, recTarget ->
                            viewModel.applyAiRecommendedPricingAndTarget(recPrice, recTarget)
                        },
                        onOpenBusinessSwitcher = { showBusinessSwitcher = true },
                        onOpenRecordTransaction = {
                            editingTransaction = null
                            preselectedClientForTransaction = null
                            viewModel.resetReceiptScanState()
                            showRecordTransactionSheet = true
                        },
                        onEditTransaction = { tx ->
                            editingTransaction = tx
                            preselectedClientForTransaction = null
                            viewModel.resetReceiptScanState()
                            showRecordTransactionSheet = true
                        },
                        onNavigateToReports = { selectedTab = 1 },
                        onNavigateToTransactions = { selectedTab = 2 },
                        onNavigateToCrm = { selectedTab = 3 },
                        onDeleteTransaction = { viewModel.deleteTransaction(it) },
                        onSeedSampleStudios = { viewModel.seedStarterStudios() }
                    )
                    1 -> MonthlyReportsScreen(
                        uiState = uiState,
                        aiFutureState = aiFutureState,
                        onGenerateAiFuture = { prompt -> viewModel.generateAiFutureForecast(prompt) },
                        onApplyAiRecommendations = { recPrice, recTarget ->
                            viewModel.applyAiRecommendedPricingAndTarget(recPrice, recTarget)
                        },
                        onStepMonth = { viewModel.stepMonth(it) },
                        onSelectYearMonth = { viewModel.selectYearMonth(it) },
                        onShareReportText = {
                            viewModel.formatShareableMonthlyReport(
                                business = uiState.activeBusiness,
                                report = uiState.monthlyReport
                            )
                        }
                    )
                    2 -> TransactionsScreen(
                        uiState = uiState,
                        onOpenRecordTransaction = {
                            editingTransaction = null
                            preselectedClientForTransaction = null
                            viewModel.resetReceiptScanState()
                            showRecordTransactionSheet = true
                        },
                        onEditTransaction = { tx ->
                            editingTransaction = tx
                            preselectedClientForTransaction = null
                            viewModel.resetReceiptScanState()
                            showRecordTransactionSheet = true
                        },
                        onDeleteTransaction = { viewModel.deleteTransaction(it) }
                    )
                    3 -> CrmClientsScreen(
                        uiState = uiState,
                        onCreateClient = { name, phone, email, notes, firstLog ->
                            viewModel.createClientProfile(name, phone, email, notes, firstLog)
                        },
                        onUpdateClient = { client, name, phone, email, notes ->
                            viewModel.updateClientProfile(client, name, phone, email, notes)
                        },
                        onDeleteClient = { viewModel.deleteClientProfile(it) },
                        onLogInteraction = { client, type, summary ->
                            viewModel.logClientInteraction(client, type, summary)
                        },
                        onDeleteInteraction = { viewModel.deleteClientInteraction(it) },
                        onRecordTransactionForClient = { client ->
                            editingTransaction = null
                            preselectedClientForTransaction = client
                            viewModel.resetReceiptScanState()
                            showRecordTransactionSheet = true
                        }
                    )
                }
            }
        }
    }

    if (showBusinessSwitcher) {
        BusinessSwitcherBottomSheet(
            businesses = uiState.businesses,
            activeBusiness = uiState.activeBusiness,
            onSelectBusiness = { viewModel.selectBusiness(it) },
            onCreateBusiness = { name, category, defaultPrice, target, colorHex ->
                viewModel.createBusinessProfile(name, category, defaultPrice, target, colorHex)
            },
            onUpdateBusiness = { biz, name, category, defaultPrice, target, colorHex ->
                viewModel.updateBusinessProfile(biz, name, category, defaultPrice, target, colorHex)
            },
            onDeleteBusiness = { viewModel.deleteBusinessProfile(it) },
            onDismiss = { showBusinessSwitcher = false }
        )
    }

    if (showRecordTransactionSheet) {
        RecordTransactionBottomSheet(
            activeBusiness = uiState.activeBusiness,
            clients = uiState.clientsForBusiness,
            preselectedClient = preselectedClientForTransaction,
            existingTransaction = editingTransaction,
            receiptScanState = receiptScanState,
            onScanReceiptBitmap = { bmp -> viewModel.scanReceiptOrInvoice(bmp) },
            onResetReceiptScan = { viewModel.resetReceiptScanState() },
            onSaveTransaction = { phone, name, clientId, dateMillis, picCount, pricePerPic, totalAmt, method, notes, saveToCrm ->
                viewModel.recordTransaction(
                    customerPhone = phone,
                    clientName = name,
                    selectedClientId = clientId,
                    transactionDateMillis = dateMillis,
                    pictureCount = picCount,
                    pricePerPicture = pricePerPic,
                    paymentMethod = method,
                    notes = notes,
                    saveNewClientToCrm = saveToCrm,
                    totalAmountOverride = totalAmt
                )
            },
            onUpdateTransaction = { existingTx, phone, name, clientId, dateMillis, picCount, pricePerPic, totalAmt, method, notes ->
                viewModel.updateTransaction(
                    existingTransaction = existingTx,
                    customerPhone = phone,
                    clientName = name,
                    selectedClientId = clientId,
                    transactionDateMillis = dateMillis,
                    pictureCount = picCount,
                    pricePerPicture = pricePerPic,
                    totalAmount = totalAmt,
                    paymentMethod = method,
                    notes = notes
                )
            },
            onDismiss = {
                editingTransaction = null
                showRecordTransactionSheet = false
            }
        )
    }
}
