package com.example.ui.screens.customers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.Customer
import com.example.data.repository.CustomerSummary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.ProfitGreen
import com.example.ui.viewmodel.ShopViewModel
import com.example.util.CurrencyUtils
import com.example.util.LocalAppStrings
import com.example.util.NumberFormatter

@Composable
fun CustomersScreen(
    viewModel: ShopViewModel,
    onNavigateToCustomerDetail: (Long) -> Unit
) {
    val strings = LocalAppStrings.current
    val customersSummary by viewModel.customersSummary.collectAsStateWithLifecycle()
    val totalReceivable by viewModel.totalReceivable.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var showOnlyOwing by remember { mutableStateOf(false) }
    var showAddCustomerDialog by remember { mutableStateOf(false) }

    // Filter customers
    val filteredCustomers = remember(customersSummary, searchQuery, showOnlyOwing) {
        customersSummary.filter { summary ->
            val matchesSearch = searchQuery.isBlank() ||
                    summary.customer.name.contains(searchQuery.trim(), ignoreCase = true) ||
                    summary.customer.phone.contains(searchQuery.trim(), ignoreCase = true)

            val matchesFilter = if (showOnlyOwing) summary.balance > 0L else true
            matchesSearch && matchesFilter
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddCustomerDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_customer_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = strings.addCustomer)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.addCustomer, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Top Banner: Total Receivable
            Surface(
                color = ExpenseRed.copy(alpha = 0.08f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = strings.totalReceivable.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = strings.customersOweYou,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = NumberFormatter.formatPaisa(totalReceivable),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (totalReceivable > 0) ExpenseRed else ProfitGreen
                    )
                }
            }

            // Search bar & Filter Chips
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(strings.search) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_search_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = !showOnlyOwing,
                        onClick = { showOnlyOwing = false },
                        label = { Text(strings.allCustomers) },
                        modifier = Modifier.testTag("filter_all_customers")
                    )
                    FilterChip(
                        selected = showOnlyOwing,
                        onClick = { showOnlyOwing = true },
                        label = { Text(strings.owingOnly) },
                        leadingIcon = if (showOnlyOwing) {
                            { Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        modifier = Modifier.testTag("filter_owing_only")
                    )
                }
            }

            // List of Customers
            if (filteredCustomers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = strings.noCustomersYet,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    items(filteredCustomers, key = { it.customer.id }) { summary ->
                        CustomerCard(
                            summary = summary,
                            onClick = { onNavigateToCustomerDetail(summary.customer.id) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    if (showAddCustomerDialog) {
        AddEditCustomerDialog(
            customer = null,
            onDismiss = { showAddCustomerDialog = false },
            onConfirm = { name, phone, note, limit ->
                viewModel.addCustomer(name, phone, note, limit)
                showAddCustomerDialog = false
            }
        )
    }
}

@Composable
fun CustomerCard(
    summary: CustomerSummary,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("customer_card_${summary.customer.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (summary.balance > 0) ExpenseRed.copy(alpha = 0.12f) else ProfitGreen.copy(alpha = 0.12f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = if (summary.balance > 0) ExpenseRed else ProfitGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = summary.customer.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (summary.customer.phone.isNotBlank()) {
                    Text(
                        text = summary.customer.phone,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (summary.customer.creditLimit != null) {
                    Text(
                        text = "Limit: ${NumberFormatter.formatPaisa(summary.customer.creditLimit)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = NumberFormatter.formatPaisa(summary.balance),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (summary.balance > 0) ExpenseRed else ProfitGreen
                )
                Text(
                    text = if (summary.balance > 0) "Owed" else "Settled",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (summary.balance > 0) ExpenseRed else ProfitGreen
                )
            }

            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun AddEditCustomerDialog(
    customer: Customer?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String, note: String, creditLimit: Long?) -> Unit
) {
    val strings = LocalAppStrings.current
    var name by remember { mutableStateOf(customer?.name ?: "") }
    var phone by remember { mutableStateOf(customer?.phone ?: "") }
    var note by remember { mutableStateOf(customer?.note ?: "") }
    var limitText by remember {
        mutableStateOf(if (customer?.creditLimit != null) CurrencyUtils.paisaToEditableString(customer.creditLimit) else "")
    }

    var isNameError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (customer == null) strings.addCustomer else strings.editCustomer,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        isNameError = false
                    },
                    label = { Text(strings.customerName) },
                    isError = isNameError,
                    supportingText = {
                        if (isNameError) {
                            Text("Name cannot be empty", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_name_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(strings.customerPhone) },
                    placeholder = { Text("0300-1234567") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_phone_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = limitText,
                    onValueChange = { limitText = it },
                    label = { Text(strings.creditLimit) },
                    placeholder = { Text("Optional limit e.g. 10000") },
                    prefix = { Text("PKR ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_limit_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(strings.customerNote) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_note_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        isNameError = true
                    } else {
                        val limitPaisa = if (limitText.isNotBlank()) NumberFormatter.parseInputToPaisa(limitText) else null
                        onConfirm(name.trim(), phone.trim(), note.trim(), limitPaisa)
                    }
                },
                modifier = Modifier.testTag("save_customer_button")
            ) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
