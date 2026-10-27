package com.example.ui.screens.suppliers

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.Supplier
import com.example.data.repository.SupplierSummary
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.ShopViewModel
import com.example.util.CurrencyUtils

@Composable
fun SuppliersScreen(
    viewModel: ShopViewModel,
    onNavigateToSupplierDetail: (Long) -> Unit
) {
    val suppliersSummary by viewModel.suppliersSummary.collectAsStateWithLifecycle()
    val currentShop by viewModel.currentShop.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var supplierToEdit by remember { mutableStateOf<Supplier?>(null) }
    var supplierToDelete by remember { mutableStateOf<Supplier?>(null) }

    val filteredSuppliers = remember(suppliersSummary, searchQuery) {
        val list = if (searchQuery.isBlank()) {
            suppliersSummary
        } else {
            suppliersSummary.filter {
                it.supplier.name.contains(searchQuery, ignoreCase = true) ||
                        it.supplier.phone.contains(searchQuery, ignoreCase = true) ||
                        it.supplier.note.contains(searchQuery, ignoreCase = true)
            }
        }
        list.sortedByDescending { it.balancePayable }
    }

    val totalPayableAcrossAll = remember(suppliersSummary) {
        suppliersSummary.sumOf { it.balancePayable.coerceAtLeast(0L) }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_supplier_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Supplier")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Supplier", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Summary Banner
            Surface(
                color = WarningAmber.copy(alpha = 0.09f),
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
                            text = "TOTAL SUPPLIER PAYABLE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${suppliersSummary.size} registered suppliers",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = CurrencyUtils.formatPaisa(totalPayableAcrossAll),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = WarningAmber
                    )
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search supplier by name or phone...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .testTag("search_supplier_input")
            )

            if (filteredSuppliers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.People,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No suppliers match '$searchQuery'" else "No suppliers added yet",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Add wholesale suppliers and vendors to track stock purchases, cash payments, and credit balances for ${currentShop?.name ?: "your shop"}.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredSuppliers, key = { it.supplier.id }) { item ->
                        SupplierCard(
                            summary = item,
                            onClick = { onNavigateToSupplierDetail(item.supplier.id) },
                            onCall = { phone ->
                                if (phone.isNotBlank()) {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                    context.startActivity(intent)
                                }
                            },
                            onEdit = { supplierToEdit = item.supplier },
                            onDelete = { supplierToDelete = item.supplier }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(88.dp))
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddEditSupplierDialog(
            supplier = null,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, phone, note ->
                viewModel.addSupplier(name = name, phone = phone, note = note)
                showAddDialog = false
            }
        )
    }

    if (supplierToEdit != null) {
        AddEditSupplierDialog(
            supplier = supplierToEdit,
            onDismiss = { supplierToEdit = null },
            onConfirm = { name, phone, note ->
                supplierToEdit?.let {
                    viewModel.updateSupplier(it.copy(name = name, phone = phone, note = note))
                }
                supplierToEdit = null
            }
        )
    }

    if (supplierToDelete != null) {
        ConfirmDeleteDialog(
            title = "Delete Supplier?",
            message = "Are you sure you want to delete '${supplierToDelete!!.name}'? All related purchases and payments will also be permanently deleted.",
            onDismiss = { supplierToDelete = null },
            onConfirm = {
                supplierToDelete?.let { viewModel.deleteSupplier(it) }
                supplierToDelete = null
            }
        )
    }
}

@Composable
private fun SupplierCard(
    summary: SupplierSummary,
    onClick: () -> Unit,
    onCall: (String) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val supplier = summary.supplier
    val hasBalance = summary.balancePayable > 0

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("supplier_card_${supplier.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = supplier.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (supplier.phone.isNotBlank()) {
                            Text(
                                text = supplier.phone,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (supplier.note.isNotBlank()) {
                            Text(
                                text = supplier.note,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (supplier.phone.isNotBlank()) {
                        IconButton(
                            onClick = { onCall(supplier.phone) },
                            modifier = Modifier.testTag("call_supplier_${supplier.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Call Supplier",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = null)
                                },
                                text = { Text("Edit Supplier") },
                                onClick = {
                                    menuExpanded = false
                                    onEdit()
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                text = {
                                    Text("Delete Supplier", color = MaterialTheme.colorScheme.error)
                                },
                                onClick = {
                                    menuExpanded = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Financial Summary row inside supplier card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Purchases: ${CurrencyUtils.formatPaisa(summary.totalPurchases)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Paid: ${CurrencyUtils.formatPaisa(summary.totalPaid)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (hasBalance) "PAYABLE" else "CLEARED",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (hasBalance) WarningAmber else ProfitGreen
                        )
                        Text(
                            text = CurrencyUtils.formatPaisa(summary.balancePayable.coerceAtLeast(0L)),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (hasBalance) WarningAmber else ProfitGreen
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddEditSupplierDialog(
    supplier: Supplier?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String, note: String) -> Unit
) {
    var name by remember { mutableStateOf(supplier?.name ?: "") }
    var phone by remember { mutableStateOf(supplier?.phone ?: "") }
    var note by remember { mutableStateOf(supplier?.note ?: "") }
    var isNameError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (supplier == null) "Add Supplier / Vendor" else "Edit Supplier",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (it.isNotBlank()) isNameError = false
                    },
                    label = { Text("Supplier Name *") },
                    placeholder = { Text("e.g. Tariq Traders, Lahore Mills") },
                    isError = isNameError,
                    supportingText = {
                        if (isNameError) {
                            Text("Supplier name is required", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("supplier_name_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    placeholder = { Text("e.g. 0300 1234567") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("supplier_phone_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note / Items Supplied") },
                    placeholder = { Text("e.g. Beverages, flour, rice distributor") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("supplier_note_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.trim().isEmpty()) {
                        isNameError = true
                    } else {
                        onConfirm(name.trim(), phone.trim(), note.trim())
                    }
                },
                modifier = Modifier.testTag("save_supplier_button")
            ) {
                Text(if (supplier == null) "Add Supplier" else "Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
