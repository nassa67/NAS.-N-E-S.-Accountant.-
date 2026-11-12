package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ClientInteraction
import com.example.data.model.ClientProfile
import com.example.ui.theme.NasAmber
import com.example.ui.theme.NasAmberSoft
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
import com.example.ui.viewmodel.StudioUiState
import com.example.ui.viewmodel.StudioViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CrmClientsScreen(
    uiState: StudioUiState,
    onCreateClient: (name: String, phone: String, email: String, notes: String, firstLog: String) -> Unit,
    onUpdateClient: (ClientProfile, String, String, String, String) -> Unit,
    onDeleteClient: (String) -> Unit,
    onLogInteraction: (ClientProfile, String, String) -> Unit,
    onDeleteInteraction: (String) -> Unit,
    onRecordTransactionForClient: (ClientProfile) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showAddClientSheet by remember { mutableStateOf(false) }
    var editingClient by remember { mutableStateOf<ClientProfile?>(null) }
    var selectedClientForDetails by remember { mutableStateOf<ClientProfile?>(null) }

    val filteredClients = remember(uiState.clientsForBusiness, searchQuery) {
        if (searchQuery.isBlank()) {
            uiState.clientsForBusiness
        } else {
            val q = searchQuery.trim().lowercase(Locale.US)
            uiState.clientsForBusiness.filter {
                it.name.lowercase(Locale.US).contains(q) ||
                    it.phoneNumber.lowercase(Locale.US).contains(q) ||
                    it.notes.lowercase(Locale.US).contains(q)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("crm_clients_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header & Add Client CTA
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Client CRM & Interaction Logs",
                                style = MaterialTheme.typography.titleLarge,
                                color = NasSlate800
                            )
                            Text(
                                text = "${uiState.clientsForBusiness.size} clients • ${uiState.interactionsForBusiness.size} interaction logs in ${uiState.activeBusiness?.name ?: "Studio"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = NasSlate600
                            )
                        }
                        Button(
                            onClick = {
                                editingClient = null
                                showAddClientSheet = true
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NasIndigo),
                            modifier = Modifier.testTag("add_crm_client_button")
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Client")
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by client name, phone number, or notes...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Clients") },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear Search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("crm_search_input")
                    )
                }
            }
        }

        if (filteredClients.isEmpty()) {
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
                            imageVector = Icons.Default.Person,
                            contentDescription = "No CRM Clients",
                            tint = NasIndigo,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (searchQuery.isBlank()) "No CRM clients added yet" else "No matching clients found",
                            style = MaterialTheme.typography.titleMedium,
                            color = NasSlate800
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Keep client details, phone numbers, and chronological interaction logs organized for each photography business.",
                            style = MaterialTheme.typography.bodySmall,
                            color = NasSlate600,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(
                items = filteredClients,
                key = { it.id }
            ) { client ->
                val clientInteractions = uiState.interactionsForBusiness.filter { it.clientId == client.id }
                val clientTransactions = uiState.transactionsForBusiness.filter {
                    it.clientId == client.id || it.customerPhone.equals(client.phoneNumber, ignoreCase = true)
                }
                val lifetimeSpend = clientTransactions.sumOf { it.totalAmount }
                val lifetimePictures = clientTransactions.sumOf { it.pictureCount }

                CrmClientCard(
                    client = client,
                    interactionCount = clientInteractions.size,
                    latestInteraction = clientInteractions.firstOrNull(),
                    lifetimeSpend = lifetimeSpend,
                    lifetimePictures = lifetimePictures,
                    onOpenDetailsAndLogs = { selectedClientForDetails = client },
                    onEditClient = {
                        editingClient = client
                        showAddClientSheet = true
                    },
                    onRecordShoot = { onRecordTransactionForClient(client) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (showAddClientSheet) {
        AddOrEditClientBottomSheet(
            existingClient = editingClient,
            onSave = { name, phone, email, notes, firstInteraction ->
                val target = editingClient
                if (target != null) {
                    onUpdateClient(target, name, phone, email, notes)
                } else {
                    onCreateClient(name, phone, email, notes, firstInteraction)
                }
                showAddClientSheet = false
            },
            onDelete = { clientId ->
                onDeleteClient(clientId)
                showAddClientSheet = false
            },
            onDismiss = { showAddClientSheet = false }
        )
    }

    val detailClient = selectedClientForDetails
    if (detailClient != null) {
        val currentClient = uiState.clientsForBusiness.find { it.id == detailClient.id } ?: detailClient
        val interactions = uiState.interactionsForBusiness.filter { it.clientId == currentClient.id }
        val transactions = uiState.transactionsForBusiness.filter {
            it.clientId == currentClient.id || it.customerPhone.equals(currentClient.phoneNumber, ignoreCase = true)
        }
        ClientDetailAndLogsBottomSheet(
            client = currentClient,
            interactions = interactions,
            totalSpend = transactions.sumOf { it.totalAmount },
            totalPictures = transactions.sumOf { it.pictureCount },
            onAddInteraction = { type, summary ->
                onLogInteraction(currentClient, type, summary)
            },
            onDeleteInteraction = onDeleteInteraction,
            onRecordShoot = {
                selectedClientForDetails = null
                onRecordTransactionForClient(currentClient)
            },
            onDismiss = { selectedClientForDetails = null }
        )
    }
}

@Composable
private fun CrmClientCard(
    client: ClientProfile,
    interactionCount: Int,
    latestInteraction: ClientInteraction?,
    lifetimeSpend: Double,
    lifetimePictures: Long,
    onOpenDetailsAndLogs: () -> Unit,
    onEditClient: () -> Unit,
    onRecordShoot: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenDetailsAndLogs() }
            .testTag("crm_client_card_${client.id}"),
        shape = RoundedCornerShape(22.dp),
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(NasIndigoSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = client.name.take(1).uppercase(Locale.US),
                        style = MaterialTheme.typography.titleMedium,
                        color = NasIndigo,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = client.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = NasSlate800
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Phone",
                            tint = NasIndigo,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = client.phoneNumber,
                            style = MaterialTheme.typography.bodyMedium,
                            color = NasSlate600
                        )
                    }
                }

                IconButton(onClick = onEditClient) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Client",
                        tint = NasSlate600,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (client.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = client.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = NasSlate600,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Metrics Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = NasEmeraldSoft,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${StudioViewModel.formatCurrency(lifetimeSpend)} ($lifetimePictures pics)",
                        style = MaterialTheme.typography.labelSmall,
                        color = NasEmerald,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Surface(
                    color = NasIndigoSoft,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "$interactionCount Interaction Logs",
                        style = MaterialTheme.typography.labelSmall,
                        color = NasIndigo,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (latestInteraction != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = NasSurfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Latest Log",
                            tint = NasAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "[${latestInteraction.type}] ${latestInteraction.summary}",
                            style = MaterialTheme.typography.bodySmall,
                            color = NasSlate800,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenDetailsAndLogs,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("open_interaction_logs_${client.id}")
                ) {
                    Icon(Icons.Default.AddComment, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Interaction Logs")
                }

                Button(
                    onClick = onRecordShoot,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NasEmerald),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Record Shoot")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddOrEditClientBottomSheet(
    existingClient: ClientProfile?,
    onSave: (name: String, phone: String, email: String, notes: String, firstInteraction: String) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf(existingClient?.name ?: "") }
    var phone by remember { mutableStateOf(existingClient?.phoneNumber ?: "") }
    var email by remember { mutableStateOf(existingClient?.email ?: "") }
    var notes by remember { mutableStateOf(existingClient?.notes ?: "") }
    var initialInteraction by remember { mutableStateOf("") }

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
                    text = if (existingClient != null) "Edit Client Details" else "New CRM Client",
                    style = MaterialTheme.typography.titleLarge,
                    color = NasSlate800
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Client Full Name *") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("client_name_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number *") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("client_phone_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address (Optional)") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Client Details & Shoot Preferences") },
                leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )

            if (existingClient == null) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = initialInteraction,
                    onValueChange = { initialInteraction = it },
                    label = { Text("Initial Interaction Log (Optional)") },
                    placeholder = { Text("e.g., Called regarding wedding package pricing") },
                    leadingIcon = { Icon(Icons.Default.AddComment, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("client_initial_log_input")
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { onSave(name, phone, email, notes, initialInteraction) },
                enabled = name.isNotBlank() && phone.isNotBlank(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NasIndigo),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_client_button")
            ) {
                Text(if (existingClient != null) "Update Client" else "Save Client to CRM")
            }

            if (existingClient != null) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = { onDelete(existingClient.id) },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Delete Client Record", color = NasCrimson)
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClientDetailAndLogsBottomSheet(
    client: ClientProfile,
    interactions: List<ClientInteraction>,
    totalSpend: Double,
    totalPictures: Long,
    onAddInteraction: (type: String, summary: String) -> Unit,
    onDeleteInteraction: (String) -> Unit,
    onRecordShoot: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val interactionTypes = listOf("CALL", "SHOOT", "DELIVERY", "FOLLOW_UP", "INQUIRY", "NOTE")
    var selectedType by remember { mutableStateOf("CALL") }
    var summaryInput by remember { mutableStateOf("") }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy • h:mm a", Locale.US) }

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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = client.name,
                        style = MaterialTheme.typography.headlineMedium,
                        color = NasSlate800
                    )
                    Text(
                        text = client.phoneNumber,
                        style = MaterialTheme.typography.bodyMedium,
                        color = NasIndigo
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Actions: Call, SMS, Record Shoot
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${client.phoneNumber}"))
                        context.startActivity(intent)
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Call, contentDescription = "Call", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Call")
                }
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${client.phoneNumber}"))
                        context.startActivity(intent)
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Sms, contentDescription = "SMS", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Message")
                }
                Button(
                    onClick = onRecordShoot,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NasEmerald),
                    modifier = Modifier.weight(1.3f)
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Log Shoot")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Lifetime Summary Strip
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NasSurfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Lifetime Revenue", style = MaterialTheme.typography.labelSmall, color = NasSlate600)
                        Text(
                            StudioViewModel.formatCurrency(totalSpend),
                            style = MaterialTheme.typography.titleMedium,
                            color = NasSlate800,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Pictures Ordered", style = MaterialTheme.typography.labelSmall, color = NasSlate600)
                        Text(
                            "$totalPictures pictures",
                            style = MaterialTheme.typography.titleMedium,
                            color = NasIndigo,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Log New Client Interaction",
                style = MaterialTheme.typography.titleMedium,
                color = NasSlate800
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                interactionTypes.forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        label = { Text(type.replace("_", " ")) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NasIndigoSoft,
                            selectedLabelColor = NasIndigo
                        )
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
                    value = summaryInput,
                    onValueChange = { summaryInput = it },
                    placeholder = { Text("Add interaction note (e.g., Sent proof gallery)...") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("interaction_summary_input")
                )
                Button(
                    onClick = {
                        if (summaryInput.isNotBlank()) {
                            onAddInteraction(selectedType, summaryInput)
                            summaryInput = ""
                        }
                    },
                    enabled = summaryInput.isNotBlank(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NasIndigo),
                    modifier = Modifier
                        .height(54.dp)
                        .testTag("submit_interaction_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Log")
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Interaction Log History (${interactions.size})",
                style = MaterialTheme.typography.titleMedium,
                color = NasSlate800
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (interactions.isEmpty()) {
                Text(
                    text = "No interactions logged for this client yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = NasSlate600,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                interactions.forEach { log ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = NasSurfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Surface(
                                color = NasAmberSoft,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = log.type,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NasAmber,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = log.summary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = NasSlate800
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = dateFormat.format(Date(log.interactionDateMillis)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NasSlate600
                                )
                            }
                            IconButton(
                                onClick = { onDeleteInteraction(log.id) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete Log",
                                    tint = NasSlate400,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}
