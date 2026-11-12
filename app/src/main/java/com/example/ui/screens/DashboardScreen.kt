package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PeopleAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AiFutureUiState
import com.example.data.model.BusinessProfile
import com.example.data.model.PhotoTransaction
import com.example.ui.components.CashVsBankComparisonBar
import com.example.ui.components.CumulativeBalanceLineChart
import com.example.ui.components.SegmentedPillTabs
import com.example.ui.components.TransactionItemRow
import com.example.ui.components.WeeklyEarningsBarChart
import com.example.ui.theme.NasAmber
import com.example.ui.theme.NasAmberSoft
import com.example.ui.theme.NasBorder
import com.example.ui.theme.NasCrimson
import com.example.ui.theme.NasCrimsonSoft
import com.example.ui.theme.NasEmerald
import com.example.ui.theme.NasEmeraldSoft
import com.example.ui.theme.NasIndigo
import com.example.ui.theme.NasIndigoDark
import com.example.ui.theme.NasIndigoSoft
import com.example.ui.theme.NasSlate600
import com.example.ui.theme.NasSlate800
import com.example.ui.theme.NasSurfaceVariant
import com.example.ui.theme.NasWhite
import com.example.ui.viewmodel.StudioUiState
import com.example.ui.viewmodel.StudioViewModel

@Composable
fun DashboardScreen(
    uiState: StudioUiState,
    aiFutureState: AiFutureUiState = AiFutureUiState.Idle,
    onGenerateAiFuture: (String) -> Unit = {},
    onApplyAiRecommendations: (Double, Double) -> Unit = { _, _ -> },
    onOpenBusinessSwitcher: () -> Unit,
    onOpenRecordTransaction: () -> Unit,
    onEditTransaction: (PhotoTransaction) -> Unit = {},
    onNavigateToReports: () -> Unit,
    onNavigateToCrm: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onDeleteTransaction: (String) -> Unit,
    onSeedSampleStudios: () -> Unit
) {
    var selectedSegmentIndex by remember { mutableIntStateOf(0) } // 0: Balance, 1: Breakdown, 2: Target
    var selectedBucketIndex by remember { mutableIntStateOf(1) }
    var visibleStage by remember { mutableIntStateOf(4) }
    var showAiFutureSheet by remember { mutableStateOf(false) }

    val report = uiState.monthlyReport
    val activeBusiness = uiState.activeBusiness

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (uiState.businesses.isEmpty()) {
            item {
                EmptyStudiosWelcomeCard(
                    isSeeding = uiState.isSeeding,
                    onCreateStudio = onOpenBusinessSwitcher,
                    onSeedSampleStudios = onSeedSampleStudios
                )
            }
            return@LazyColumn
        }

        // 1. Segmented Pill Tabs (matching uploaded reference: Balance | Breakdown | Target)
        item {
            AnimatedVisibility(
                visible = visibleStage >= 1,
                enter = fadeIn(tween(450)) + slideInVertically(tween(450)) { 20 }
            ) {
                SegmentedPillTabs(
                    options = listOf("Balance", "Breakdown", "Target"),
                    selectedIndex = selectedSegmentIndex,
                    onSelect = { selectedSegmentIndex = it }
                )
            }
        }

        // 2. Main "This month" Interactive Trends Card
        item {
            AnimatedVisibility(
                visible = visibleStage >= 1,
                enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { 24 }
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = NasWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onNavigateToReports() }
                                    .padding(vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = "Month",
                                    tint = NasSlate600,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = report.displayMonthName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = NasSlate800
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = StudioViewModel.formatCurrency(report.totalEarnings),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = NasSlate800,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = when (selectedSegmentIndex) {
                                        0 -> "Monthly Balance (${report.totalPictures} pics)"
                                        1 -> "Total Earned"
                                        else -> "of ${StudioViewModel.formatCurrency(report.monthlyTarget)} Target"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NasSlate600
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        AnimatedContent(
                            targetState = selectedSegmentIndex,
                            transitionSpec = {
                                fadeIn(tween(220)) togetherWith fadeOut(tween(180))
                            },
                            label = "dashboard_chart_mode"
                        ) { mode ->
                            when (mode) {
                                0 -> {
                                    CumulativeBalanceLineChart(
                                        buckets = report.weeklyBuckets,
                                        monthlyTarget = report.monthlyTarget
                                    )
                                }
                                1 -> {
                                    Column {
                                        WeeklyEarningsBarChart(
                                            buckets = report.weeklyBuckets,
                                            selectedBucketIndex = selectedBucketIndex,
                                            onSelectBucket = { selectedBucketIndex = it }
                                        )
                                        val bucket = report.weeklyBuckets.getOrNull(selectedBucketIndex)
                                        if (bucket != null) {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Surface(
                                                color = NasSurfaceVariant,
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = "Days ${bucket.label}: ${bucket.pictureCount} pictures",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = NasSlate600
                                                    )
                                                    Text(
                                                        text = StudioViewModel.formatCurrency(bucket.totalAmount),
                                                        style = MaterialTheme.typography.labelLarge,
                                                        color = NasIndigo
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                else -> {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        val pct = (report.targetProgressFraction * 100).toInt()
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "$pct% of Monthly Goal Reached",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = NasSlate800
                                            )
                                            Text(
                                                text = StudioViewModel.formatCurrency(
                                                    (report.monthlyTarget - report.totalEarnings).coerceAtLeast(0.0)
                                                ) + " remaining",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = NasEmerald
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        LinearProgressIndicator(
                                            progress = { report.targetProgressFraction.coerceIn(0f, 1f) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(14.dp)
                                                .clip(RoundedCornerShape(50)),
                                            color = NasEmerald,
                                            trackColor = NasSurfaceVariant,
                                            strokeCap = StrokeCap.Round
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        CashVsBankComparisonBar(
                                            cashAmount = report.cashEarnings,
                                            bankAmount = report.bankTransferEarnings,
                                            cashFraction = report.cashFraction,
                                            bankFraction = report.bankTransferFraction
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Split Action & Target CTA Card (with fixed single-line button labels + AI Future Hub CTA)
        item {
            AnimatedVisibility(
                visible = visibleStage >= 2,
                enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { 24 }
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = NasWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(NasIndigoSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                    contentDescription = "Quick Log",
                                    tint = NasIndigo,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Scan receipt or record shoot in seconds",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = NasSlate800
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Use AI Receipt Scanner to pull customer name, amount paid & date from invoices, or log shoots at ${StudioViewModel.formatCurrency(activeBusiness?.defaultPricePerPicture ?: 15.0)}/pic.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NasSlate600
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onOpenRecordTransaction,
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(containerColor = NasEmerald),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("dashboard_record_transaction_button")
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Scan / Log Shoot",
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            OutlinedButton(
                                onClick = onOpenBusinessSwitcher,
                                shape = RoundedCornerShape(50),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Studio Target",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. AI Future Forecast & Studio Growth Advisor Card
        item {
            AnimatedVisibility(
                visible = visibleStage >= 2,
                enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { 24 }
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAiFutureSheet = true }
                        .testTag("ai_future_dashboard_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = NasWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(NasIndigo, NasIndigoDark)
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White.copy(alpha = 0.16f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "AI Future",
                                    tint = NasEmerald,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "AI FUTURE • GEMINI FORECAST",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Open AI Future",
                                tint = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "AI Future Revenue & Pricing Strategist",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Project next month's studio earnings, optimize your per-picture pricing, and generate AI client follow-up scripts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = { showAiFutureSheet = true },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NasEmerald,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("open_ai_future_button")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Launch AI Future Studio Forecast",
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // 5. Live Stats Grid (Pictures Delivered, Avg Price/Pic, Active CRM Clients)
        item {
            AnimatedVisibility(
                visible = visibleStage >= 3,
                enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { 24 }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LiveStatMiniCard(
                        icon = Icons.Default.PhotoLibrary,
                        iconTint = NasIndigo,
                        iconBg = NasIndigoSoft,
                        value = "${report.totalPictures}",
                        label = "Pictures Sold",
                        onClick = onNavigateToTransactions,
                        modifier = Modifier.weight(1f)
                    )
                    LiveStatMiniCard(
                        icon = Icons.Default.PeopleAlt,
                        iconTint = NasAmber,
                        iconBg = NasAmberSoft,
                        value = "${uiState.clientsForBusiness.size}",
                        label = "CRM Clients",
                        onClick = onNavigateToCrm,
                        modifier = Modifier.weight(1f)
                    )
                    LiveStatMiniCard(
                        icon = Icons.Default.CameraAlt,
                        iconTint = NasEmerald,
                        iconBg = NasEmeraldSoft,
                        value = StudioViewModel.formatCurrency(report.averagePricePerPicture),
                        label = "Avg / Picture",
                        onClick = onNavigateToReports,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 6. Payment Method Breakdown Card
        item {
            AnimatedVisibility(
                visible = visibleStage >= 3,
                enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { 24 }
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = NasWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Payment Breakdown",
                                style = MaterialTheme.typography.titleMedium,
                                color = NasSlate800
                            )
                            TextButton(onClick = onNavigateToReports) {
                                Text("Full Monthly Report", color = NasIndigo)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        PaymentBreakdownRow(
                            icon = Icons.Default.AccountBalance,
                            iconBg = NasEmeraldSoft,
                            iconTint = NasEmerald,
                            title = "Bank Transfer",
                            subtitle = "${report.bankTransferTransactionCount} transactions (${(report.bankTransferFraction * 100).toInt()}%)",
                            amount = StudioViewModel.formatCurrency(report.bankTransferEarnings),
                            onClick = onNavigateToReports
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        PaymentBreakdownRow(
                            icon = Icons.Default.Payments,
                            iconBg = NasAmberSoft,
                            iconTint = NasAmber,
                            title = "Cash Payments",
                            subtitle = "${report.cashTransactionCount} transactions (${(report.cashFraction * 100).toInt()}%)",
                            amount = StudioViewModel.formatCurrency(report.cashEarnings),
                            onClick = onNavigateToReports
                        )
                    }
                }
            }
        }

        // 7. Recent Transactions Header + List
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Shoot Transactions",
                    style = MaterialTheme.typography.titleMedium,
                    color = NasSlate800
                )
                if (uiState.transactionsForBusiness.isNotEmpty()) {
                    TextButton(onClick = onNavigateToTransactions) {
                        Text("See All (${uiState.transactionsForBusiness.size})", color = NasIndigo)
                    }
                }
            }
        }

        if (uiState.transactionsForBusiness.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = NasWhite)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "No transactions yet",
                            tint = NasIndigo,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No transactions logged for ${activeBusiness?.name ?: "this studio"} yet",
                            style = MaterialTheme.typography.titleSmall,
                            color = NasSlate800,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap 'Scan / Log Shoot' to scan an invoice receipt with AI or record customer phone, pictures, and payment method.",
                            style = MaterialTheme.typography.bodySmall,
                            color = NasSlate600,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(
                items = uiState.transactionsForBusiness.take(5),
                key = { it.id }
            ) { tx ->
                TransactionItemRow(
                    transaction = tx,
                    onEdit = { onEditTransaction(tx) },
                    onDelete = { onDeleteTransaction(tx.id) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (showAiFutureSheet) {
        AiFutureAdvisorBottomSheet(
            activeBusiness = activeBusiness,
            aiFutureState = aiFutureState,
            onGenerateForecast = onGenerateAiFuture,
            onApplyRecommendations = { recPrice, recTarget ->
                onApplyAiRecommendations(recPrice, recTarget)
                showAiFutureSheet = false
            },
            onDismiss = { showAiFutureSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiFutureAdvisorBottomSheet(
    activeBusiness: BusinessProfile?,
    aiFutureState: AiFutureUiState,
    onGenerateForecast: (String) -> Unit,
    onApplyRecommendations: (Double, Double) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var customScenarioInput by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        if (aiFutureState is AiFutureUiState.Idle) {
            onGenerateForecast("")
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = NasWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .testTag("ai_future_bottom_sheet")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(NasIndigo),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Future",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "AI Future Studio Strategist",
                            style = MaterialTheme.typography.titleLarge,
                            color = NasSlate800
                        )
                        Text(
                            text = "${activeBusiness?.name ?: "Studio"} • Powered by Gemini 3.8 Flash",
                            style = MaterialTheme.typography.labelMedium,
                            color = NasIndigo
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Custom Goal / What-If Scenario Input
            OutlinedTextField(
                value = customScenarioInput,
                onValueChange = { customScenarioInput = it },
                label = { Text("Ask AI Future or Test a Pricing Goal (Optional)") },
                placeholder = { Text("e.g., How do I double my wedding bookings next month?") },
                leadingIcon = { Icon(Icons.Default.Lightbulb, contentDescription = null, tint = NasAmber) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_future_prompt_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = { onGenerateForecast(customScenarioInput) },
                enabled = aiFutureState !is AiFutureUiState.Analyzing,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NasIndigo),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("generate_ai_future_button")
            ) {
                if (aiFutureState is AiFutureUiState.Analyzing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Analyzing Studio Ledger & CRM with Gemini...")
                } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate AI Future Forecast", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (aiFutureState) {
                is AiFutureUiState.Analyzing -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = NasIndigoSoft)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = NasIndigo)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Building 30-Day & 90-Day Revenue Projections...",
                                style = MaterialTheme.typography.titleSmall,
                                color = NasSlate800,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Evaluating per-picture rates, Cash vs. Bank Transfer mix, and client repeat potential.",
                                style = MaterialTheme.typography.bodySmall,
                                color = NasSlate600,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                is AiFutureUiState.Error -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = NasCrimsonSoft)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = "AI Error",
                                tint = NasCrimson,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = aiFutureState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = NasCrimson,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                is AiFutureUiState.Success -> {
                    val forecast = aiFutureState.forecast

                    // 1. Revenue Projections Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = NasEmeraldSoft)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "NEXT MONTH FORECAST",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NasEmerald,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = StudioViewModel.formatCurrency(forecast.nextMonthProjectedRevenue),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = NasSlate800,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "30-Day AI Projection",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NasSlate600
                                )
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = NasIndigoSoft)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "3-MONTH OUTLOOK",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NasIndigo,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = StudioViewModel.formatCurrency(forecast.threeMonthProjectedRevenue),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = NasSlate800,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "90-Day Cumulative",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NasSlate600
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 2. Recommended Pricing & Target Card with 1-Click Apply
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, NasIndigo.copy(alpha = 0.25f), RoundedCornerShape(18.dp)),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = NasWhite)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "AI Recommended Pricing & Studio Target",
                                style = MaterialTheme.typography.titleSmall,
                                color = NasSlate800,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Suggested Price / Pic", style = MaterialTheme.typography.labelSmall, color = NasSlate600)
                                    Text(
                                        text = StudioViewModel.formatCurrency(forecast.recommendedPricePerPicture),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = NasIndigo,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Suggested Monthly Target", style = MaterialTheme.typography.labelSmall, color = NasSlate600)
                                    Text(
                                        text = StudioViewModel.formatCurrency(forecast.recommendedMonthlyTarget),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = NasEmerald,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = {
                                    onApplyRecommendations(
                                        forecast.recommendedPricePerPicture,
                                        forecast.recommendedMonthlyTarget
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("apply_ai_recommendations_button")
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NasEmerald, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Apply AI Rate & Target to ${activeBusiness?.name ?: "Studio"}", color = NasSlate800)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3. Growth Outlook & Cash Flow Tip
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = NasSurfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Executive Growth Summary",
                                style = MaterialTheme.typography.titleSmall,
                                color = NasSlate800,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = forecast.growthOutlookSummary,
                                style = MaterialTheme.typography.bodySmall,
                                color = NasSlate800
                            )
                            if (forecast.cashFlowOptimizationTip.isNotBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = NasBorder)
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Cash vs. Bank Transfer Strategy",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = NasIndigo,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = forecast.cashFlowOptimizationTip,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NasSlate600
                                )
                            }
                        }
                    }

                    if (forecast.actionPlanSteps.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = NasWhite),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Action Plan for Next Month",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = NasSlate800,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                forecast.actionPlanSteps.forEachIndexed { idx, step ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(NasIndigoSoft),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${idx + 1}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = NasIndigo,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = step,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = NasSlate800,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (forecast.clientFollowUpMessageDraft.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = NasAmberSoft)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Sms,
                                            contentDescription = null,
                                            tint = NasAmber,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "AI Client Re-Booking Script",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = NasSlate800,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, forecast.clientFollowUpMessageDraft)
                                            }
                                            context.startActivity(Intent.createChooser(sendIntent, "Send Client Follow-Up"))
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.IosShare,
                                            contentDescription = "Share Script",
                                            tint = NasSlate800,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = forecast.clientFollowUpMessageDraft,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NasSlate800
                                )
                            }
                        }
                    }
                }
                else -> Unit
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun EmptyStudiosWelcomeCard(
    isSeeding: Boolean,
    onCreateStudio: () -> Unit,
    onSeedSampleStudios: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = NasWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(NasIndigoSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Create First Studio",
                    tint = NasIndigo,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Welcome to NAS Finance",
                style = MaterialTheme.typography.headlineMedium,
                color = NasSlate800,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Create your first photography business profile or load two sample studios (NAS Wedding Studio & NAS Portrait Lab) to explore CRM logs, per-picture accounting, and AI Future reports.",
                style = MaterialTheme.typography.bodyMedium,
                color = NasSlate600,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onCreateStudio,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NasEmerald),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("empty_create_studio_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Photography Business Profile", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onSeedSampleStudios,
                enabled = !isSeeding,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("seed_sample_studios_button")
            ) {
                if (isSeeding) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Setting up sample studios...")
                } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = NasIndigo)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Load Multi-Business Demo Data", color = NasIndigo)
                }
            }
        }
    }
}

@Composable
private fun LiveStatMiniCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    iconBg: Color,
    value: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NasWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = NasSlate800,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = NasSlate600,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun PaymentBreakdownRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    amount: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = NasSlate800
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = NasSlate600
            )
        }
        Text(
            text = amount,
            style = MaterialTheme.typography.titleMedium,
            color = NasSlate800,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = NasBorder,
            modifier = Modifier.size(18.dp)
        )
    }
}
