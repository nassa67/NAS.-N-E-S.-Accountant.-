package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.PhotoTransaction
import com.example.ui.components.TransactionItemRow
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
import java.util.Locale

@Composable
fun TransactionsScreen(
    uiState: StudioUiState,
    onOpenRecordTransaction: () -> Unit,
    onEditTransaction: (PhotoTransaction) -> Unit = {},
    onDeleteTransaction: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var methodFilter by remember { mutableStateOf("ALL") } // ALL, CASH, BANK_TRANSFER

    val filteredTransactions = remember(uiState.transactionsForBusiness, searchQuery, methodFilter) {
        uiState.transactionsForBusiness.filter { tx ->
            val matchesMethod = when (methodFilter) {
                "CASH" -> tx.paymentMethod == "CASH"
                "BANK_TRANSFER" -> tx.paymentMethod == "BANK_TRANSFER"
                else -> true
            }
            val matchesQuery = if (searchQuery.isBlank()) {
                true
            } else {
                val q = searchQuery.trim().lowercase(Locale.US)
                tx.customerPhone.lowercase(Locale.US).contains(q) ||
                    tx.clientName.lowercase(Locale.US).contains(q) ||
                    tx.notes.lowercase(Locale.US).contains(q)
            }
            matchesMethod && matchesQuery
        }
    }

    val filteredRevenue = remember(filteredTransactions) {
        filteredTransactions.sumOf { it.totalAmount }
    }
    val filteredPictures = remember(filteredTransactions) {
        filteredTransactions.sumOf { it.pictureCount }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("transactions_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
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
                    Text(
                        text = "Accounting & Invoice Ledger",
                        style = MaterialTheme.typography.titleLarge,
                        color = NasSlate800
                    )
                    Text(
                        text = "Tap 'Edit' on any transaction to adjust price or add extra pictures",
                        style = MaterialTheme.typography.bodySmall,
                        color = NasSlate600
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onOpenRecordTransaction,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NasEmerald),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("ledger_add_transaction_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("New Entry", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onOpenRecordTransaction,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("ledger_ai_scan_receipt_button")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = NasIndigo, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("AI Scan Receipt", color = NasIndigo, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by customer phone, name, or invoice note...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ledger_search_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = methodFilter == "ALL",
                            onClick = { methodFilter = "ALL" },
                            label = { Text("All Methods (${uiState.transactionsForBusiness.size})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NasIndigoSoft,
                                selectedLabelColor = NasIndigo
                            )
                        )
                        FilterChip(
                            selected = methodFilter == "CASH",
                            onClick = { methodFilter = "CASH" },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Payments,
                                    contentDescription = null,
                                    tint = NasAmber,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text("Cash") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NasAmberSoft,
                                selectedLabelColor = NasAmber
                            )
                        )
                        FilterChip(
                            selected = methodFilter == "BANK_TRANSFER",
                            onClick = { methodFilter = "BANK_TRANSFER" },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = NasEmerald,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text("Bank Transfer") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NasEmeraldSoft,
                                selectedLabelColor = NasEmerald
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Filtered Totals Banner
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = NasSurfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${filteredTransactions.size} records • $filteredPictures pictures",
                                style = MaterialTheme.typography.labelMedium,
                                color = NasSlate600
                            )
                            Text(
                                text = StudioViewModel.formatCurrency(filteredRevenue),
                                style = MaterialTheme.typography.titleMedium,
                                color = NasSlate800,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        if (filteredTransactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = NasWhite)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "No transactions",
                            tint = NasIndigo,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No matching transactions",
                            style = MaterialTheme.typography.titleMedium,
                            color = NasSlate800
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Every shoot transaction records the date, customer name & phone, pictures, amount paid, and Cash or Bank Transfer categorization.",
                            style = MaterialTheme.typography.bodySmall,
                            color = NasSlate600,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(
                items = filteredTransactions,
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
}
