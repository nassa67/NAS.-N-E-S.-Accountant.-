package com.example.ui.components

import android.app.DatePickerDialog
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.BusinessProfile
import com.example.data.model.ClientProfile
import com.example.data.model.PhotoTransaction
import com.example.data.model.WeeklyBucket
import com.example.ui.theme.NasAmber
import com.example.ui.theme.NasAmberSoft
import com.example.ui.theme.NasBorder
import com.example.ui.theme.NasCrimson
import com.example.ui.theme.NasEmerald
import com.example.ui.theme.NasEmeraldSoft
import com.example.ui.theme.NasIndigo
import com.example.ui.theme.NasIndigoSoft
import com.example.ui.theme.NasSlate400
import com.example.ui.theme.NasSlate600
import com.example.ui.theme.NasSlate800
import com.example.ui.theme.NasSurfaceVariant
import com.example.ui.theme.NasWhite
import com.example.ui.theme.StudioBadgeColors
import com.example.ui.viewmodel.StudioViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun parseHexColor(hex: String, fallback: Color = NasIndigo): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        fallback
    }
}

@Composable
fun TopStudioHeader(
    activeBusiness: BusinessProfile?,
    businessCount: Int,
    onOpenBusinessSwitcher: () -> Unit,
    onSignOutClick: () -> Unit
) {
    val badgeColor = remember(activeBusiness?.accentColorHex) {
        parseHexColor(activeBusiness?.accentColorHex ?: "#6366F1")
    }

    Surface(
        color = NasWhite,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Business Switcher Pill (matching uploaded reference "My Account v")
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clip(RoundedCornerShape(50))
                    .background(NasSurfaceVariant)
                    .clickable { onOpenBusinessSwitcher() }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("business_switcher_button")
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(badgeColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Studio Profile Avatar",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = activeBusiness?.name ?: "Select Studio",
                            style = MaterialTheme.typography.titleMedium,
                            color = NasSlate800,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Switch Business Profile",
                            tint = NasSlate800,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = if (activeBusiness != null) {
                            "${activeBusiness.category} • ${StudioViewModel.formatCurrency(activeBusiness.defaultPricePerPicture)}/pic"
                        } else {
                            "$businessCount Studios • Tap to manage"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = NasSlate600,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right side: Cloud Sync Security Badge + Sign Out
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = NasEmeraldSoft,
                    shape = RoundedCornerShape(50)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = "Cloud Synced",
                            tint = NasEmerald,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "NAS Sync",
                            style = MaterialTheme.typography.labelSmall,
                            color = NasEmerald,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                IconButton(
                    onClick = onSignOutClick,
                    modifier = Modifier.testTag("sign_out_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = "Sign Out",
                        tint = NasSlate600
                    )
                }
            }
        }
    }
}

@Composable
fun SegmentedPillTabs(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = NasWhite,
        shape = RoundedCornerShape(50),
        shadowElevation = 1.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            options.forEachIndexed { index, label ->
                val isSelected = selectedIndex == index
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) NasSlate800 else Color.Transparent,
                    animationSpec = tween(220),
                    label = "pillBg"
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else NasSlate600,
                    animationSpec = tween(220),
                    label = "pillText"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(bgColor)
                        .clickable { onSelect(index) }
                        .padding(vertical = 10.dp)
                        .testTag("segmented_tab_$index"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = textColor,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun WeeklyEarningsBarChart(
    buckets: List<WeeklyBucket>,
    selectedBucketIndex: Int,
    onSelectBucket: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val maxAmount = remember(buckets) {
        (buckets.maxOfOrNull { it.totalAmount } ?: 0.0).coerceAtLeast(100.0)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .padding(top = 8.dp, bottom = 8.dp)
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val width = size.width
                val height = size.height
                val gridLines = 3
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

                for (i in 0..gridLines) {
                    val y = height * (i.toFloat() / gridLines)
                    drawLine(
                        color = NasBorder,
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = dashEffect
                    )
                }
            }

            Row(
                modifier = Modifier
                    .matchParentSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                buckets.forEachIndexed { index, bucket ->
                    val isSelected = index == selectedBucketIndex
                    val fraction = (bucket.totalAmount / maxAmount).toFloat().coerceIn(0.08f, 1f)
                    val animatedFrac by animateFloatAsState(
                        targetValue = fraction,
                        animationSpec = tween(500),
                        label = "barHeight_$index"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelectBucket(index) }
                    ) {
                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .height((135 * animatedFrac).dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    when {
                                        bucket.totalAmount <= 0.0 -> NasIndigo.copy(alpha = 0.18f)
                                        isSelected -> NasIndigo
                                        else -> NasIndigo.copy(alpha = 0.45f)
                                    }
                                )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            buckets.forEachIndexed { index, bucket ->
                val isSelected = index == selectedBucketIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(if (isSelected) NasSlate800 else Color.Transparent)
                        .clickable { onSelectBucket(index) }
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = bucket.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) Color.White else NasSlate600,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun CumulativeBalanceLineChart(
    buckets: List<WeeklyBucket>,
    monthlyTarget: Double,
    modifier: Modifier = Modifier
) {
    val cumulativePoints = remember(buckets) {
        var running = 0.0
        buckets.map {
            running += it.totalAmount
            running
        }
    }
    val maxVal = remember(cumulativePoints, monthlyTarget) {
        maxOf(cumulativePoints.maxOrNull() ?: 0.0, monthlyTarget, 100.0)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            val w = size.width
            val h = size.height
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

            // Target line
            if (monthlyTarget > 0.0) {
                val targetY = h - ((monthlyTarget / maxVal).toFloat().coerceIn(0f, 1f) * h)
                drawLine(
                    color = NasEmerald.copy(alpha = 0.65f),
                    start = Offset(0f, targetY),
                    end = Offset(w, targetY),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = dashEffect
                )
            }

            if (cumulativePoints.isNotEmpty()) {
                val stepX = if (cumulativePoints.size > 1) w / (cumulativePoints.size - 1) else w
                val path = Path()
                val points = cumulativePoints.mapIndexed { idx, value ->
                    val x = idx * stepX
                    val y = h - ((value / maxVal).toFloat().coerceIn(0f, 1f) * h)
                    Offset(x, y)
                }
                points.forEachIndexed { idx, pt ->
                    if (idx == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
                }
                drawPath(
                    path = path,
                    color = NasIndigo,
                    style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                )
                points.forEach { pt ->
                    drawCircle(
                        color = NasWhite,
                        radius = 6.dp.toPx(),
                        center = pt
                    )
                    drawCircle(
                        color = NasIndigo,
                        radius = 4.dp.toPx(),
                        center = pt,
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            buckets.forEach { bucket ->
                Text(
                    text = bucket.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = NasSlate600
                )
            }
        }
    }
}

@Composable
fun CashVsBankComparisonBar(
    cashAmount: Double,
    bankAmount: Double,
    cashFraction: Float,
    bankFraction: Float,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(NasAmber)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Cash (${(cashFraction * 100).toInt()}%)",
                    style = MaterialTheme.typography.labelMedium,
                    color = NasSlate800
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(NasEmerald)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Bank Transfer (${(bankFraction * 100).toInt()}%)",
                    style = MaterialTheme.typography.labelMedium,
                    color = NasSlate800
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .clip(RoundedCornerShape(50))
                .background(NasSurfaceVariant)
        ) {
            val w = size.width
            val h = size.height
            if (cashAmount + bankAmount > 0.0) {
                val cashWidth = w * cashFraction.coerceIn(0f, 1f)
                val bankWidth = w - cashWidth
                if (cashWidth > 0f) {
                    drawRoundRect(
                        color = NasAmber,
                        topLeft = Offset(0f, 0f),
                        size = Size(cashWidth, h),
                        cornerRadius = CornerRadius(h / 2, h / 2)
                    )
                }
                if (bankWidth > 0f) {
                    drawRoundRect(
                        color = NasEmerald,
                        topLeft = Offset(cashWidth, 0f),
                        size = Size(bankWidth, h),
                        cornerRadius = CornerRadius(h / 2, h / 2)
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionItemRow(
    transaction: PhotoTransaction,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val isBank = transaction.paymentMethod == "BANK_TRANSFER"
    val dateStr = remember(transaction.transactionDateMillis) {
        SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(transaction.transactionDateMillis))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onEdit != null) {
                    Modifier.clickable { onEdit() }
                } else {
                    Modifier
                }
            )
            .testTag("transaction_card_${transaction.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = NasWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isBank) NasEmeraldSoft else NasAmberSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isBank) Icons.Default.AccountBalance else Icons.Default.Payments,
                    contentDescription = transaction.paymentMethod,
                    tint = if (isBank) NasEmerald else NasAmber,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.clientName,
                    style = MaterialTheme.typography.titleSmall,
                    color = NasSlate800,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${transaction.customerPhone} • $dateStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = NasSlate600
                )
                if (transaction.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = transaction.notes,
                        style = MaterialTheme.typography.labelSmall,
                        color = NasSlate600,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = NasIndigoSoft,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${transaction.pictureCount} pics × ${StudioViewModel.formatCurrency(transaction.pricePerPicture)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = NasIndigo,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Surface(
                        color = if (isBank) NasEmeraldSoft else NasAmberSoft,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (isBank) "Bank Transfer" else "Cash",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isBank) NasEmerald else NasAmber,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = StudioViewModel.formatCurrency(transaction.totalAmount),
                    style = MaterialTheme.typography.titleMedium,
                    color = NasSlate800,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (onEdit != null) {
                        Surface(
                            color = NasIndigoSoft,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onEdit() }
                                .testTag("edit_transaction_${transaction.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit or Adjust Price",
                                    tint = NasIndigo,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Edit",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NasIndigo,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    if (onDelete != null) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("delete_transaction_${transaction.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete Transaction",
                                tint = NasSlate400,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BusinessSwitcherBottomSheet(
    businesses: List<BusinessProfile>,
    activeBusiness: BusinessProfile?,
    onSelectBusiness: (String) -> Unit,
    onCreateBusiness: (String, String, Double, Double, String) -> Unit,
    onUpdateBusiness: (BusinessProfile, String, String, Double, Double, String) -> Unit,
    onDeleteBusiness: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isEditingOrCreating by remember { mutableStateOf(businesses.isEmpty()) }
    var editingTarget by remember { mutableStateOf<BusinessProfile?>(null) }

    var nameInput by remember { mutableStateOf("") }
    var categoryInput by remember { mutableStateOf("") }
    var defaultPriceInput by remember { mutableStateOf("15.0") }
    var monthlyTargetInput by remember { mutableStateOf("3500.0") }
    var selectedColorHex by remember { mutableStateOf(StudioBadgeColors.first()) }

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
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEditingOrCreating) {
                        if (editingTarget != null) "Edit Studio Profile" else "New Photography Business"
                    } else {
                        "Switch Photography Business"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = NasSlate800
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!isEditingOrCreating) {
                businesses.forEach { biz ->
                    val isSelected = biz.id == activeBusiness?.id
                    val accent = parseHexColor(biz.accentColorHex)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable {
                                onSelectBusiness(biz.id)
                                onDismiss()
                            },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) NasIndigoSoft else NasSurfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(accent),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = biz.name,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = biz.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = NasSlate800
                                )
                                Text(
                                    text = "${biz.category} • Default ${StudioViewModel.formatCurrency(biz.defaultPricePerPicture)}/pic",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NasSlate600
                                )
                                Text(
                                    text = "Monthly Target: ${StudioViewModel.formatCurrency(biz.monthlyTarget)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NasIndigo
                                )
                            }
                            IconButton(
                                onClick = {
                                    editingTarget = biz
                                    nameInput = biz.name
                                    categoryInput = biz.category
                                    defaultPriceInput = biz.defaultPricePerPicture.toString()
                                    monthlyTargetInput = biz.monthlyTarget.toString()
                                    selectedColorHex = biz.accentColorHex
                                    isEditingOrCreating = true
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Studio",
                                    tint = NasSlate600
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Active Studio",
                                    tint = NasIndigo
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        editingTarget = null
                        nameInput = ""
                        categoryInput = "Weddings & Portraits"
                        defaultPriceInput = "15.0"
                        monthlyTargetInput = "3500.0"
                        selectedColorHex = StudioBadgeColors[businesses.size % StudioBadgeColors.size]
                        isEditingOrCreating = true
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NasIndigo),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("add_new_business_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Another Photography Business")
                }
            } else {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Business / Studio Name *") },
                    placeholder = { Text("e.g., NAS Wedding Studio") },
                    leadingIcon = { Icon(Icons.Default.BusinessCenter, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("business_name_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = categoryInput,
                    onValueChange = { categoryInput = it },
                    label = { Text("Photography Specialty") },
                    placeholder = { Text("e.g., Weddings, Portraits, Commercial") },
                    leadingIcon = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = defaultPriceInput,
                        onValueChange = { defaultPriceInput = it },
                        label = { Text("Default Price / Pic ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("business_default_price_input")
                    )

                    OutlinedTextField(
                        value = monthlyTargetInput,
                        onValueChange = { monthlyTargetInput = it },
                        label = { Text("Monthly Target ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("business_monthly_target_input")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Studio Badge Color",
                    style = MaterialTheme.typography.labelLarge,
                    color = NasSlate800
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StudioBadgeColors.forEach { hex ->
                        val color = parseHexColor(hex)
                        val isPicked = hex.equals(selectedColorHex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isPicked) 3.dp else 0.dp,
                                    color = if (isPicked) NasSlate800 else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColorHex = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isPicked) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected Color",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (businesses.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { isEditingOrCreating = false },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                        ) {
                            Text("Back")
                        }
                    }
                    Button(
                        onClick = {
                            val defaultPrice = defaultPriceInput.toDoubleOrNull() ?: 15.0
                            val target = monthlyTargetInput.toDoubleOrNull() ?: 3000.0
                            val targetBiz = editingTarget
                            if (targetBiz != null) {
                                onUpdateBusiness(
                                    targetBiz,
                                    nameInput,
                                    categoryInput,
                                    defaultPrice,
                                    target,
                                    selectedColorHex
                                )
                            } else {
                                onCreateBusiness(
                                    nameInput,
                                    categoryInput,
                                    defaultPrice,
                                    target,
                                    selectedColorHex
                                )
                            }
                            onDismiss()
                        },
                        enabled = nameInput.isNotBlank(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NasEmerald),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("save_business_button")
                    ) {
                        Text(if (editingTarget != null) "Save Changes" else "Create Studio")
                    }
                }

                val deletable = editingTarget
                if (deletable != null && businesses.size > 1) {
                    Spacer(modifier = Modifier.height(10.dp))
                    TextButton(
                        onClick = {
                            onDeleteBusiness(deletable.id)
                            onDismiss()
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Delete Studio Profile", color = NasCrimson)
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

