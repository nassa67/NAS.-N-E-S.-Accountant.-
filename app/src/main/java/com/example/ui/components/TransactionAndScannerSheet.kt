package com.example.ui.components

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.BusinessProfile
import com.example.data.model.ClientProfile
import com.example.data.model.PhotoTransaction
import com.example.data.model.ReceiptScanUiState
import com.example.ui.theme.NasAmber
import com.example.ui.theme.NasAmberSoft
import com.example.ui.theme.NasBorder
import com.example.ui.theme.NasCrimson
import com.example.ui.theme.NasCrimsonSoft
import com.example.ui.theme.NasEmerald
import com.example.ui.theme.NasEmeraldSoft
import com.example.ui.theme.NasIndigo
import com.example.ui.theme.NasIndigoSoft
import com.example.ui.theme.NasSlate600
import com.example.ui.theme.NasSlate800
import com.example.ui.theme.NasSurfaceVariant
import com.example.ui.theme.NasWhite
import com.example.ui.viewmodel.StudioViewModel
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private fun formatCleanNumber(value: Double): String {
    if (value.isNaN() || value.isInfinite()) return "0"
    val bd = BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros()
    return bd.toPlainString()
}

private fun decodeBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.isMutableRequired = true
            }
        } else {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        }
    } catch (_: Exception) {
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecordTransactionBottomSheet(
    activeBusiness: BusinessProfile?,
    clients: List<ClientProfile>,
    preselectedClient: ClientProfile? = null,
    existingTransaction: PhotoTransaction? = null,
    receiptScanState: ReceiptScanUiState = ReceiptScanUiState.Idle,
    onScanReceiptBitmap: (Bitmap) -> Unit = {},
    onResetReceiptScan: () -> Unit = {},
    onSaveTransaction: (
        customerPhone: String,
        clientName: String,
        selectedClientId: String,
        transactionDateMillis: Long,
        pictureCount: Long,
        pricePerPicture: Double,
        totalAmount: Double,
        paymentMethod: String,
        notes: String,
        saveNewClientToCrm: Boolean
    ) -> Unit,
    onUpdateTransaction: (
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
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _ -> },
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isEditMode = existingTransaction != null

    val initialPricePerPic = existingTransaction?.pricePerPicture
        ?: activeBusiness?.defaultPricePerPicture
        ?: 15.0
    val initialPicCount = existingTransaction?.pictureCount ?: 20L
    val initialTotal = existingTransaction?.totalAmount ?: (initialPicCount * initialPricePerPic)

    var selectedClientId by remember(existingTransaction?.id, preselectedClient?.id) {
        mutableStateOf(existingTransaction?.clientId ?: preselectedClient?.id ?: "")
    }
    var customerPhone by remember(existingTransaction?.id, preselectedClient?.id) {
        mutableStateOf(
            existingTransaction?.customerPhone?.let { if (it == "No Phone") "" else it }
                ?: preselectedClient?.phoneNumber
                ?: ""
        )
    }
    var clientName by remember(existingTransaction?.id, preselectedClient?.id) {
        mutableStateOf(existingTransaction?.clientName ?: preselectedClient?.name ?: "")
    }
    var transactionDateMillis by remember(existingTransaction?.id) {
        mutableLongStateOf(existingTransaction?.transactionDateMillis ?: System.currentTimeMillis())
    }
    var pictureCountText by remember(existingTransaction?.id) {
        mutableStateOf(initialPicCount.toString())
    }
    var pricePerPictureText by remember(existingTransaction?.id, activeBusiness?.defaultPricePerPicture) {
        mutableStateOf(formatCleanNumber(initialPricePerPic))
    }
    var totalAmountText by remember(existingTransaction?.id, activeBusiness?.defaultPricePerPicture) {
        mutableStateOf(formatCleanNumber(initialTotal))
    }
    var extraMoneyToAddText by remember { mutableStateOf("") }
    var paymentMethod by remember(existingTransaction?.id) {
        mutableStateOf(existingTransaction?.paymentMethod ?: "CASH")
    }
    var notes by remember(existingTransaction?.id) {
        mutableStateOf(existingTransaction?.notes ?: "")
    }
    var saveNewClientToCrm by remember { mutableStateOf(true) }
    var scannedPreviewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var cameraErrorHint by remember { mutableStateOf<String?>(null) }

    val pictureCount = pictureCountText.toLongOrNull()?.coerceAtLeast(1L) ?: 1L
    val pricePerPicture = pricePerPictureText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val liveTotal = totalAmountText.toDoubleOrNull()?.coerceAtLeast(0.0)
        ?: (pictureCount * pricePerPicture)

    // Populate fields automatically when AI receipt scan finishes
    LaunchedEffect(receiptScanState) {
        if (receiptScanState is ReceiptScanUiState.Success) {
            val scanned = receiptScanState.data
            if (scanned.clientName.isNotBlank()) {
                clientName = scanned.clientName
            }
            if (scanned.customerPhone.isNotBlank()) {
                customerPhone = scanned.customerPhone
            }
            // Match against existing CRM clients by phone or name
            val matchedClient = clients.find { c ->
                (scanned.customerPhone.isNotBlank() && c.phoneNumber.equals(scanned.customerPhone, ignoreCase = true)) ||
                    (scanned.clientName.isNotBlank() && c.name.equals(scanned.clientName, ignoreCase = true))
            }
            if (matchedClient != null) {
                selectedClientId = matchedClient.id
                if (customerPhone.isBlank()) customerPhone = matchedClient.phoneNumber
                if (clientName.isBlank()) clientName = matchedClient.name
            }
            transactionDateMillis = scanned.transactionDateMillis
            pictureCountText = scanned.pictureCount.coerceAtLeast(1L).toString()
            pricePerPictureText = formatCleanNumber(scanned.pricePerPicture)
            totalAmountText = formatCleanNumber(scanned.amountPaid)
            paymentMethod = scanned.paymentMethod
            if (scanned.invoiceDetails.isNotBlank()) {
                notes = scanned.invoiceDetails
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            cameraErrorHint = null
            val bmp = decodeBitmapFromUri(context, uri)
            if (bmp != null) {
                scannedPreviewBitmap = bmp
                onScanReceiptBitmap(bmp)
            } else {
                cameraErrorHint = "Could not read the selected image file."
            }
        }
    }

    val snapReceiptLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bmp: Bitmap? ->
        if (bmp != null) {
            cameraErrorHint = null
            scannedPreviewBitmap = bmp
            onScanReceiptBitmap(bmp)
        }
    }

    val dateFormatter = remember { SimpleDateFormat("EEE, MMM dd, yyyy", Locale.US) }

    ModalBottomSheet(
        onDismissRequest = {
            onResetReceiptScan()
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = NasWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isEditMode) "Edit Transaction & Adjust Price" else "Record Transaction & Invoice",
                        style = MaterialTheme.typography.titleLarge,
                        color = NasSlate800
                    )
                    Text(
                        text = if (isEditMode) {
                            "Adjust amount paid, extra pictures, or scan updated receipt"
                        } else {
                            "${activeBusiness?.name ?: "Active Studio"} • AI Smart Receipt & Invoice Scanner"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = NasIndigo
                    )
                }
                IconButton(
                    onClick = {
                        onResetReceiptScan()
                        onDismiss()
                    }
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 1. AI Smart Receipt & Invoice Scanner Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NasIndigo.copy(alpha = 0.25f), RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = NasIndigoSoft.copy(alpha = 0.55f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(NasIndigo),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "AI Receipt Scanner",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "AI Smart Receipt & Invoice Scanner",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = NasSlate800,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Auto-extracts Name, Amount Paid, Date & Invoice Details",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NasSlate600
                                )
                            }
                        }

                        val previewBmp = scannedPreviewBitmap
                        if (previewBmp != null) {
                            Image(
                                bitmap = previewBmp.asImageBitmap(),
                                contentDescription = "Scanned Receipt Preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, NasIndigo, RoundedCornerShape(10.dp))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                try {
                                    cameraErrorHint = null
                                    snapReceiptLauncher.launch(null)
                                } catch (e: Exception) {
                                    cameraErrorHint = "Camera unavailable; please use 'Scan Photo' to select a receipt image."
                                }
                            },
                            enabled = receiptScanState !is ReceiptScanUiState.Scanning,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NasIndigo),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("snap_receipt_camera_button")
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Snap Receipt", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                cameraErrorHint = null
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            enabled = receiptScanState !is ReceiptScanUiState.Scanning,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("scan_receipt_gallery_button")
                        ) {
                            Icon(Icons.Default.DocumentScanner, contentDescription = null, tint = NasIndigo, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Scan Photo", color = NasIndigo, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (cameraErrorHint != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = cameraErrorHint ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = NasCrimson
                        )
                    }

                    AnimatedVisibility(visible = receiptScanState !is ReceiptScanUiState.Idle) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            when (receiptScanState) {
                                is ReceiptScanUiState.Scanning -> {
                                    Surface(
                                        color = NasWhite,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.5.dp,
                                                color = NasIndigo
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = "Scanning receipt for name, amount paid, date & invoice items...",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = NasSlate800
                                            )
                                        }
                                    }
                                }
                                is ReceiptScanUiState.Success -> {
                                    val d = receiptScanState.data
                                    Surface(
                                        color = NasEmeraldSoft,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Receipt Scanned",
                                                tint = NasEmerald,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Extracted from Receipt / Invoice:",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = NasEmerald,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "Name: ${d.clientName.ifBlank { "Not specified" }} • Paid: ${StudioViewModel.formatCurrency(d.amountPaid)} • Pics: ${d.pictureCount}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = NasSlate800
                                                )
                                                if (d.invoiceDetails.isNotBlank()) {
                                                    Text(
                                                        text = "Details: ${d.invoiceDetails}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = NasSlate600
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                is ReceiptScanUiState.Error -> {
                                    Surface(
                                        color = NasCrimsonSoft,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ErrorOutline,
                                                contentDescription = "Scan Error",
                                                tint = NasCrimson,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = receiptScanState.message,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = NasCrimson,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                                else -> Unit
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Live Total Paid & Price / Additional Pictures Adjustment Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isEditMode) NasEmeraldSoft else NasIndigoSoft
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isEditMode) "ADJUSTED AMOUNT PAID" else "TRANSPARENT SHOOT / INVOICE TOTAL",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isEditMode) NasEmerald else NasIndigo,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            if (isEditMode && existingTransaction != null) {
                                Text(
                                    text = "Previous Paid: ${StudioViewModel.formatCurrency(existingTransaction.totalAmount)} (${existingTransaction.pictureCount} pics)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NasSlate600
                                )
                            } else {
                                Text(
                                    text = "$pictureCount pictures × ${StudioViewModel.formatCurrency(pricePerPicture)} / pic",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NasSlate600
                                )
                            }
                        }
                        Text(
                            text = StudioViewModel.formatCurrency(liveTotal),
                            style = MaterialTheme.typography.headlineMedium,
                            color = if (isEditMode) NasEmerald else NasIndigo,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = NasBorder.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick Extra Pictures & Price Adder
                    Text(
                        text = "Quick Price & Extra Pictures Adjustment",
                        style = MaterialTheme.typography.labelMedium,
                        color = NasSlate800,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Client took additional pictures or paid extra? Tap to add pictures or add money directly:",
                        style = MaterialTheme.typography.labelSmall,
                        color = NasSlate600
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(1L, 5L, 10L).forEach { extraPics ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    val newCount = pictureCount + extraPics
                                    val unitRate = if (pricePerPicture > 0.0) {
                                        pricePerPicture
                                    } else {
                                        activeBusiness?.defaultPricePerPicture ?: 15.0
                                    }
                                    val addedMoney = extraPics * unitRate
                                    val newTotal = liveTotal + addedMoney
                                    pictureCountText = newCount.toString()
                                    totalAmountText = formatCleanNumber(newTotal)
                                    pricePerPictureText = formatCleanNumber(newTotal / newCount.toDouble())
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = NasIndigo
                                    )
                                },
                                label = {
                                    Text(
                                        text = "+$extraPics Pics (+${StudioViewModel.formatCurrency(extraPics * pricePerPicture)})",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = NasWhite,
                                    labelColor = NasIndigo
                                ),
                                modifier = Modifier.testTag("add_extra_pics_$extraPics")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = extraMoneyToAddText,
                            onValueChange = { extraMoneyToAddText = it },
                            label = { Text("Add Extra Money Paid (+$)") },
                            placeholder = { Text("e.g., 5000") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("adjust_add_amount_input")
                        )
                        Button(
                            onClick = {
                                val delta = extraMoneyToAddText.toDoubleOrNull() ?: 0.0
                                if (delta != 0.0) {
                                    val updatedTotal = (liveTotal + delta).coerceAtLeast(0.0)
                                    totalAmountText = formatCleanNumber(updatedTotal)
                                    if (pictureCount > 0L) {
                                        pricePerPictureText = formatCleanNumber(updatedTotal / pictureCount.toDouble())
                                    }
                                    extraMoneyToAddText = ""
                                }
                            },
                            enabled = (extraMoneyToAddText.toDoubleOrNull() ?: 0.0) != 0.0,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NasIndigo),
                            modifier = Modifier
                                .height(52.dp)
                                .testTag("apply_add_amount_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add to Paid", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Quick Select from CRM Clients if available
            if (clients.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Quick-Fill from CRM Clients",
                    style = MaterialTheme.typography.labelMedium,
                    color = NasSlate600
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    clients.forEach { client ->
                        val isSelected = selectedClientId == client.id
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedClientId = client.id
                                customerPhone = client.phoneNumber
                                clientName = client.name
                            },
                            label = { Text("${client.name} (${client.phoneNumber})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NasIndigoSoft,
                                selectedLabelColor = NasIndigo
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Date Picker Field
            OutlinedButton(
                onClick = {
                    val cal = Calendar.getInstance().apply { timeInMillis = transactionDateMillis }
                    DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            val pickedCal = Calendar.getInstance().apply {
                                set(Calendar.YEAR, year)
                                set(Calendar.MONTH, month)
                                set(Calendar.DAY_OF_MONTH, dayOfMonth)
                            }
                            transactionDateMillis = pickedCal.timeInMillis
                        },
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH),
                        cal.get(Calendar.DAY_OF_MONTH)
                    ).show()
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("transaction_date_picker_button")
            ) {
                Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Transaction Date: ${dateFormatter.format(Date(transactionDateMillis))}")
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = clientName,
                onValueChange = { clientName = it },
                label = { Text("Customer / Invoice Name *") },
                placeholder = { Text("e.g., Sarah Jenkins") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transaction_client_name_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = customerPhone,
                onValueChange = {
                    customerPhone = it
                    val matched = clients.find { c -> c.phoneNumber.equals(it.trim(), ignoreCase = true) }
                    if (matched != null) {
                        selectedClientId = matched.id
                        if (clientName.isBlank()) clientName = matched.name
                    }
                },
                label = { Text("Customer Phone Number") },
                placeholder = { Text("+1 555-234-5678") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transaction_phone_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = pictureCountText,
                    onValueChange = { newVal ->
                        val digits = newVal.filter { ch -> ch.isDigit() }
                        pictureCountText = digits
                        val count = digits.toLongOrNull() ?: 0L
                        val unit = pricePerPictureText.toDoubleOrNull() ?: 0.0
                        if (count > 0L) {
                            totalAmountText = formatCleanNumber(count * unit)
                        }
                    },
                    label = { Text("No. of Pictures *") },
                    leadingIcon = { Icon(Icons.Default.PhotoLibrary, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("transaction_picture_count_input")
                )

                OutlinedTextField(
                    value = pricePerPictureText,
                    onValueChange = { newVal ->
                        pricePerPictureText = newVal
                        val unit = newVal.toDoubleOrNull()
                        val count = pictureCountText.toLongOrNull() ?: 0L
                        if (unit != null && count > 0L) {
                            totalAmountText = formatCleanNumber(count * unit)
                        }
                    },
                    label = { Text("Price / Picture") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("transaction_price_per_pic_input")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Direct Editable Amount Paid / Total Price Field
            OutlinedTextField(
                value = totalAmountText,
                onValueChange = { newTotalStr ->
                    totalAmountText = newTotalStr
                    val parsedTotal = newTotalStr.toDoubleOrNull()
                    val count = pictureCountText.toLongOrNull()?.coerceAtLeast(1L) ?: 1L
                    if (parsedTotal != null && parsedTotal >= 0.0 && count > 0L) {
                        pricePerPictureText = formatCleanNumber(parsedTotal / count.toDouble())
                    }
                },
                label = { Text("Total Amount Paid ($) — Direct Price Adjustment *") },
                placeholder = { Text("e.g., 20000") },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = NasEmerald) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transaction_total_amount_input")
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Payment Method *",
                style = MaterialTheme.typography.labelLarge,
                color = NasSlate800
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PaymentMethodOptionCard(
                    title = "Cash",
                    subtitle = "In-person payment",
                    icon = Icons.Default.Payments,
                    selected = paymentMethod == "CASH",
                    accentColor = NasAmber,
                    softBg = NasAmberSoft,
                    onClick = { paymentMethod = "CASH" },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("payment_method_cash")
                )
                PaymentMethodOptionCard(
                    title = "Bank Transfer",
                    subtitle = "Wire / ACH / Direct",
                    icon = Icons.Default.AccountBalance,
                    selected = paymentMethod == "BANK_TRANSFER",
                    accentColor = NasEmerald,
                    softBg = NasEmeraldSoft,
                    onClick = { paymentMethod = "BANK_TRANSFER" },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("payment_method_bank")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Invoice Details / Shoot Notes") },
                placeholder = { Text("e.g., Portrait package + 5 additional pictures") },
                leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transaction_notes_input")
            )

            if (!isEditMode && selectedClientId.isBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { saveNewClientToCrm = !saveNewClientToCrm }
                ) {
                    Checkbox(
                        checked = saveNewClientToCrm,
                        onCheckedChange = { saveNewClientToCrm = it }
                    )
                    Text(
                        text = "Automatically save this customer to CRM Client Directory",
                        style = MaterialTheme.typography.bodySmall,
                        color = NasSlate600
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Primary Save / Update Action
            Button(
                onClick = {
                    val safeCount = pictureCount.coerceAtLeast(1L)
                    val safeTotal = liveTotal.coerceAtLeast(0.0)
                    val safeUnit = if (safeCount > 0L) safeTotal / safeCount.toDouble() else pricePerPicture
                    if (isEditMode && existingTransaction != null) {
                        onUpdateTransaction(
                            existingTransaction,
                            customerPhone,
                            clientName,
                            selectedClientId,
                            transactionDateMillis,
                            safeCount,
                            safeUnit,
                            safeTotal,
                            paymentMethod,
                            notes
                        )
                    } else {
                        onSaveTransaction(
                            customerPhone,
                            clientName,
                            selectedClientId,
                            transactionDateMillis,
                            safeCount,
                            safeUnit,
                            safeTotal,
                            paymentMethod,
                            notes,
                            saveNewClientToCrm
                        )
                    }
                    onResetReceiptScan()
                    onDismiss()
                },
                enabled = (customerPhone.isNotBlank() || clientName.isNotBlank()) && pictureCount >= 1L,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NasEmerald),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("submit_transaction_button")
            ) {
                Text(
                    text = if (isEditMode) {
                        "Save Adjusted Price (${StudioViewModel.formatCurrency(liveTotal)})"
                    } else {
                        "Save Transaction & Invoice (${StudioViewModel.formatCurrency(liveTotal)})"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Share / Export Formatted Invoice Button
            OutlinedButton(
                onClick = {
                    val studioName = activeBusiness?.name ?: "NAS Finance Studio"
                    val invoiceDate = dateFormatter.format(Date(transactionDateMillis))
                    val customerDisplay = clientName.ifBlank { customerPhone.ifBlank { "Valued Client" } }
                    val methodDisplay = if (paymentMethod == "BANK_TRANSFER") "Bank Transfer" else "Cash"
                    val invoiceText = buildString {
                        appendLine("========================================")
                        appendLine("INVOICE & PAYMENT RECEIPT — $studioName")
                        appendLine("========================================")
                        appendLine("Date           : $invoiceDate")
                        appendLine("Client Name    : $customerDisplay")
                        if (customerPhone.isNotBlank()) {
                            appendLine("Customer Phone : $customerPhone")
                        }
                        appendLine("----------------------------------------")
                        appendLine("Pictures / Qty : $pictureCount pictures")
                        appendLine("Rate / Picture : ${StudioViewModel.formatCurrency(if (pictureCount > 0) liveTotal / pictureCount else pricePerPicture)}")
                        appendLine("TOTAL PAID     : ${StudioViewModel.formatCurrency(liveTotal)}")
                        appendLine("Payment Method : $methodDisplay")
                        if (notes.isNotBlank()) {
                            appendLine("Invoice Details: $notes")
                        }
                        appendLine("========================================")
                    }
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Invoice Receipt — $studioName ($customerDisplay)")
                        putExtra(Intent.EXTRA_TEXT, invoiceText)
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share Invoice / Receipt"))
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("share_invoice_receipt_button")
            ) {
                Icon(Icons.Default.IosShare, contentDescription = null, tint = NasIndigo, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share / Send Invoice Receipt", color = NasIndigo, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun PaymentMethodOptionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    accentColor: Color,
    softBg: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) accentColor else NasBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) softBg else NasWhite
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accentColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = NasSlate800
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = NasSlate600
                )
            }
        }
    }
}
