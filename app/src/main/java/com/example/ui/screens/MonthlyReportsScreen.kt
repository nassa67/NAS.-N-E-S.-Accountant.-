package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AiFutureUiState
import com.example.ui.components.CashVsBankComparisonBar
import com.example.ui.components.WeeklyEarningsBarChart
import com.example.ui.theme.NasAmber
import com.example.ui.theme.NasAmberSoft
import com.example.ui.theme.NasEmerald
import com.example.ui.theme.NasEmeraldSoft
import com.example.ui.theme.NasIndigo
import com.example.ui.theme.NasIndigoSoft
import com.example.ui.theme.NasSlate600
import com.example.ui.theme.NasSlate800
import com.example.ui.theme.NasSurfaceVariant
import com.example.ui.theme.NasWhite
import com.example.ui.viewmodel.StudioUiState
import com.example.ui.viewmodel.StudioViewModel

@Composable
fun MonthlyReportsScreen(
    uiState: StudioUiState,
    aiFutureState: AiFutureUiState = AiFutureUiState.Idle,
    onGenerateAiFuture: (String) -> Unit = {},
    onApplyAiRecommendations: (Double, Double) -> Unit = { _, _ -> },
    onStepMonth: (Int) -> Unit,
    onSelectYearMonth: (String) -> Unit,
    onShareReportText: () -> String
) {
    val context = LocalContext.current
    val report = uiState.monthlyReport
    var selectedWeekIndex by remember { mutableIntStateOf(0) }
    var showAiFutureSheet by remember { mutableStateOf(false) }

    val topClientsInMonth = remember(report.transactions) {
        report.transactions
            .groupBy { it.customerPhone to it.clientName }
            .map { (key, txs) ->
                Triple(
                    key.second,
                    key.first,
                    txs.sumOf { it.totalAmount } to txs.sumOf { it.pictureCount }
                )
            }
            .sortedByDescending { it.third.first }
            .take(5)
    }

    if (showAiFutureSheet) {
        AiFutureAdvisorBottomSheet(
            activeBusiness = uiState.activeBusiness,
            aiFutureState = aiFutureState,
            onGenerateForecast = onGenerateAiFuture,
            onApplyRecommendations = { price, target ->
                onApplyAiRecommendations(price, target)
            },
            onDismiss = { showAiFutureSheet = false }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("monthly_reports_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Month Selector & Export Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = NasWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onStepMonth(-1) },
                            modifier = Modifier.testTag("prev_month_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Previous Month",
                                tint = NasSlate800
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "AUTOMATIC MONTHLY REPORT",
                                style = MaterialTheme.typography.labelSmall,
                                color = NasIndigo,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = report.displayMonthName,
                                style = MaterialTheme.typography.titleLarge,
                                color = NasSlate800
                            )
                        }

                        IconButton(
                            onClick = { onStepMonth(1) },
                            modifier = Modifier.testTag("next_month_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Next Month",
                                tint = NasSlate800
                            )
                        }
                    }

                    if (uiState.availableYearMonths.size > 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            uiState.availableYearMonths.forEach { ym ->
                                FilterChip(
                                    selected = ym == uiState.selectedYearMonth,
                                    onClick = { onSelectYearMonth(ym) },
                                    label = { Text(StudioViewModel.formatDisplayMonth(ym)) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NasIndigoSoft,
                                        selectedLabelColor = NasIndigo
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val shareText = onShareReportText()
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "NAS Finance Monthly Report — ${report.displayMonthName}")
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Monthly Report"))
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NasIndigo),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("share_monthly_report_button")
                    ) {
                        Icon(Icons.Default.IosShare, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share / Export Monthly Summary & CSV", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            showAiFutureSheet = true
                            if (aiFutureState is AiFutureUiState.Idle) {
                                onGenerateAiFuture("")
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("reports_ai_future_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = NasIndigo,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Open AI Future Revenue & Pricing Strategist",
                            color = NasIndigo,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 2. Total Monthly Earnings & Target Progress Card
        item {
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
                    Text(
                        text = "Total Monthly Earnings",
                        style = MaterialTheme.typography.labelLarge,
                        color = NasSlate600
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = StudioViewModel.formatCurrency(report.totalEarnings),
                        style = MaterialTheme.typography.displayMedium,
                        color = NasSlate800,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = NasIndigoSoft,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Pictures Sold", style = MaterialTheme.typography.labelSmall, color = NasSlate600)
                                Text(
                                    "${report.totalPictures} pics",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = NasIndigo,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Surface(
                            color = NasSurfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Avg / Picture", style = MaterialTheme.typography.labelSmall, color = NasSlate600)
                                Text(
                                    StudioViewModel.formatCurrency(report.averagePricePerPicture),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = NasSlate800,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Surface(
                            color = NasEmeraldSoft,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Transactions", style = MaterialTheme.typography.labelSmall, color = NasSlate600)
                                Text(
                                    "${report.totalTransactions} shoots",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = NasEmerald,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Monthly Target Progress",
                            style = MaterialTheme.typography.labelMedium,
                            color = NasSlate800
                        )
                        Text(
                            text = "${(report.targetProgressFraction * 100).toInt()}% of ${StudioViewModel.formatCurrency(report.monthlyTarget)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = NasEmerald,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { report.targetProgressFraction.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(50)),
                        color = NasEmerald,
                        trackColor = NasSurfaceVariant,
                        strokeCap = StrokeCap.Round
                    )
                }
            }
        }

        // 3. Payment Method Breakdown: Cash vs Bank Transfer
        item {
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
                    Text(
                        text = "Cash vs. Bank Transfer Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        color = NasSlate800
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Automatic monthly categorization by payment method",
                        style = MaterialTheme.typography.bodySmall,
                        color = NasSlate600
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    CashVsBankComparisonBar(
                        cashAmount = report.cashEarnings,
                        bankAmount = report.bankTransferEarnings,
                        cashFraction = report.cashFraction,
                        bankFraction = report.bankTransferFraction
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Cash Card
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = NasAmberSoft)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Payments,
                                        contentDescription = "Cash",
                                        tint = NasAmber,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Cash",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = NasSlate800
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = StudioViewModel.formatCurrency(report.cashEarnings),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = NasSlate800,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${report.cashTransactionCount} payments (${(report.cashFraction * 100).toInt()}%)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NasSlate600
                                )
                            }
                        }

                        // Bank Transfer Card
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = NasEmeraldSoft)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalance,
                                        contentDescription = "Bank Transfer",
                                        tint = NasEmerald,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Bank Transfer",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = NasSlate800
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = StudioViewModel.formatCurrency(report.bankTransferEarnings),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = NasSlate800,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${report.bankTransferTransactionCount} payments (${(report.bankTransferFraction * 100).toInt()}%)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NasSlate600
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "Weekly Revenue Distribution",
                        style = MaterialTheme.typography.titleSmall,
                        color = NasSlate800
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    WeeklyEarningsBarChart(
                        buckets = report.weeklyBuckets,
                        selectedBucketIndex = selectedWeekIndex,
                        onSelectBucket = { selectedWeekIndex = it }
                    )

                    val selectedBucket = report.weeklyBuckets.getOrNull(selectedWeekIndex)
                    if (selectedBucket != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = NasSurfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Days ${selectedBucket.label} (${selectedBucket.pictureCount} pics)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = NasSlate800
                                )
                                Text(
                                    text = "Cash: ${StudioViewModel.formatCurrency(selectedBucket.cashAmount)} • Bank: ${StudioViewModel.formatCurrency(selectedBucket.bankTransferAmount)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NasSlate600
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Top Clients in Month
        if (topClientsInMonth.isNotEmpty()) {
            item {
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = NasAmber,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Top Clients in ${report.displayMonthName}",
                                style = MaterialTheme.typography.titleMedium,
                                color = NasSlate800
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        topClientsInMonth.forEachIndexed { idx, (name, phone, metrics) ->
                            val (spend, pics) = metrics
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(NasIndigoSoft),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${idx + 1}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = NasIndigo,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = NasSlate800
                                    )
                                    Text(
                                        text = "$phone • $pics pictures",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = NasSlate600
                                    )
                                }
                                Text(
                                    text = StudioViewModel.formatCurrency(spend),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = NasSlate800,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
