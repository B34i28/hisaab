package com.example.ui.screens.suppliers

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.Purchase
import com.example.data.local.entity.Supplier
import com.example.data.local.entity.SupplierPayment
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.DatePickerModal
import com.example.ui.theme.LossRed
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.ShopViewModel
import com.example.util.CurrencyUtils
import com.example.util.DateUtils

sealed class LedgerEntry {
    abstract val date: Long
    abstract val id: Long

    data class PurchaseEntry(val purchase: Purchase) : LedgerEntry() {
        override val date: Long get() = purchase.date
        override val id: Long get() = purchase.id
    }

    data class PaymentEntry(val payment: SupplierPayment) : LedgerEntry() {
        override val date: Long get() = payment.date
        override val id: Long get() = payment.id
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierDetailScreen(
    supplierId: Long,
    viewModel: ShopViewModel,
    onNavigateBack: () -> Unit
) {
    BackHandler { onNavigateBack() }

    val context = LocalContext.current
    val supplier by viewModel.getSupplierById(supplierId).collectAsStateWithLifecycle(initialValue = null)
    val purchases by viewModel.getPurchasesForSupplier(supplierId).collectAsStateWithLifecycle(initialValue = emptyList())
    val payments by viewModel.getPaymentsForSupplier(supplierId).collectAsStateWithLifecycle(initialValue = emptyList())

    var showAddPurchaseDialog by remember { mutableStateOf(false) }
    var showAddPaymentDialog by remember { mutableStateOf(false) }
    var purchaseToEdit by remember { mutableStateOf<Purchase?>(null) }
    var paymentToEdit by remember { mutableStateOf<SupplierPayment?>(null) }
    var itemToDelete by remember { mutableStateOf<LedgerEntry?>(null) }

    val totalPurchases = remember(purchases) { purchases.sumOf { it.totalAmount } }
    val totalPaidOnPurchases = remember(purchases) { purchases.sumOf { it.paidAmount } }
    val totalSubsequentPayments = remember(payments) { payments.sumOf { it.amount } }
    val totalPaid = totalPaidOnPurchases + totalSubsequentPayments
    val balancePayable = totalPurchases - totalPaid

    // Combine and sort purchases and payments chronologically
    val combinedLedger: List<LedgerEntry> = remember(purchases, payments) {
        val list = mutableListOf<LedgerEntry>()
        purchases.forEach { list.add(LedgerEntry.PurchaseEntry(it)) }
        payments.forEach { list.add(LedgerEntry.PaymentEntry(it)) }
        list.sortedByDescending { it.date }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = supplier?.name ?: "Supplier Ledger",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        if (supplier?.phone?.isNotBlank() == true) {
                            Text(
                                text = supplier!!.phone,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("supplier_detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (supplier?.phone?.isNotBlank() == true) {
                        IconButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${supplier!!.phone}"))
                                context.startActivity(intent)
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Call, contentDescription = "Call")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Balance & Stats Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (balancePayable > 0) WarningAmber.copy(alpha = 0.12f) else ProfitGreen.copy(alpha = 0.12f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (balancePayable > 0) "BALANCE REMAINING" else "ACCOUNT STATUS",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (balancePayable > 0) WarningAmber else ProfitGreen
                        )
                        Text(
                            text = if (balancePayable > 0) "Due to Supplier" else "All Cleared",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = CurrencyUtils.formatPaisa(balancePayable.coerceAtLeast(0L)),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = if (balancePayable > 0) WarningAmber else ProfitGreen
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Total Purchased",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CurrencyUtils.formatPaisa(totalPurchases),
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Total Paid",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CurrencyUtils.formatPaisa(totalPaid),
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = ProfitGreen
                            )
                        }
                    }
                }
            }

            // Action Buttons: Add Purchase | Pay Now
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { showAddPurchaseDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("record_purchase_button")
                ) {
                    Icon(imageVector = Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Purchase", fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = { showAddPaymentDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E824C)),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("record_payment_button")
                ) {
                    Icon(imageVector = Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pay Now", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Ledger History (${combinedLedger.size})",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (combinedLedger.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No transactions with this supplier",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Add purchases of stock or record cash payments above.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(combinedLedger, key = {
                        when (it) {
                            is LedgerEntry.PurchaseEntry -> "purchase_${it.purchase.id}"
                            is LedgerEntry.PaymentEntry -> "payment_${it.payment.id}"
                        }
                    }) { entry ->
                        when (entry) {
                            is LedgerEntry.PurchaseEntry -> {
                                PurchaseLedgerItem(
                                    purchase = entry.purchase,
                                    onEdit = { purchaseToEdit = entry.purchase },
                                    onDelete = { itemToDelete = entry }
                                )
                            }
                            is LedgerEntry.PaymentEntry -> {
                                PaymentLedgerItem(
                                    payment = entry.payment,
                                    onEdit = { paymentToEdit = entry.payment },
                                    onDelete = { itemToDelete = entry }
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }

    val supplierDisplayName = supplier?.name ?: "Supplier"

    if (showAddPurchaseDialog) {
        AddEditPurchaseDialog(
            supplierId = supplierId,
            supplierName = supplierDisplayName,
            purchase = null,
            onDismiss = { showAddPurchaseDialog = false },
            onConfirm = { totalAmount, paidAmount, date, desc ->
                viewModel.addPurchase(
                    supplierId = supplierId,
                    totalAmount = totalAmount,
                    paidAmount = paidAmount,
                    date = date,
                    description = desc
                )
                showAddPurchaseDialog = false
            }
        )
    }

    if (purchaseToEdit != null) {
        AddEditPurchaseDialog(
            supplierId = supplierId,
            supplierName = supplierDisplayName,
            purchase = purchaseToEdit,
            onDismiss = { purchaseToEdit = null },
            onConfirm = { totalAmount, paidAmount, date, desc ->
                purchaseToEdit?.let {
                    viewModel.updatePurchase(
                        it.copy(
                            totalAmount = totalAmount,
                            paidAmount = paidAmount,
                            date = date,
                            description = desc
                        )
                    )
                }
                purchaseToEdit = null
            }
        )
    }

    if (showAddPaymentDialog) {
        AddEditSupplierPaymentDialog(
            supplierId = supplierId,
            supplierName = supplierDisplayName,
            currentBalance = balancePayable,
            payment = null,
            onDismiss = { showAddPaymentDialog = false },
            onConfirm = { amount, date, note ->
                viewModel.addSupplierPayment(
                    supplierId = supplierId,
                    purchaseId = null,
                    amount = amount,
                    date = date,
                    note = note
                )
                showAddPaymentDialog = false
            }
        )
    }

    if (paymentToEdit != null) {
        AddEditSupplierPaymentDialog(
            supplierId = supplierId,
            supplierName = supplierDisplayName,
            currentBalance = balancePayable,
            payment = paymentToEdit,
            onDismiss = { paymentToEdit = null },
            onConfirm = { amount, date, note ->
                paymentToEdit?.let {
                    viewModel.updateSupplierPayment(
                        it.copy(
                            amount = amount,
                            date = date,
                            note = note
                        )
                    )
                }
                paymentToEdit = null
            }
        )
    }

    if (itemToDelete != null) {
        val isPurchase = itemToDelete is LedgerEntry.PurchaseEntry
        ConfirmDeleteDialog(
            title = if (isPurchase) "Delete Purchase?" else "Delete Payment?",
            message = "Are you sure you want to delete this record? The supplier balance will be adjusted accordingly.",
            onDismiss = { itemToDelete = null },
            onConfirm = {
                when (val item = itemToDelete) {
                    is LedgerEntry.PurchaseEntry -> viewModel.deletePurchase(item.purchase)
                    is LedgerEntry.PaymentEntry -> viewModel.deleteSupplierPayment(item.payment)
                    null -> {}
                }
                itemToDelete = null
            }
        )
    }
}

@Composable
private fun PurchaseLedgerItem(
    purchase: Purchase,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val unpaid = purchase.totalAmount - purchase.paidAmount

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF3B82F6).copy(alpha = 0.12f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "Purchase",
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = if (purchase.description.isNotBlank()) purchase.description else "Stock Purchase",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "${DateUtils.formatDateTime(purchase.date)} • Paid: ${CurrencyUtils.formatPaisa(purchase.paidAmount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = CurrencyUtils.formatPaisa(purchase.totalAmount),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (unpaid > 0) {
                        Text(
                            text = "Due: ${CurrencyUtils.formatPaisa(unpaid)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = WarningAmber
                        )
                    }
                }

                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            text = { Text("Edit") },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentLedgerItem(
    payment: SupplierPayment,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = ProfitGreen.copy(alpha = 0.12f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = "Payment Made",
                            tint = ProfitGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = if (payment.note.isNotBlank()) payment.note else "Payment Made",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = DateUtils.formatDateTime(payment.date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "-${CurrencyUtils.formatPaisa(payment.amount)}",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = ProfitGreen
                )

                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            text = { Text("Edit") },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddEditPurchaseDialog(
    supplierId: Long,
    supplierName: String,
    purchase: Purchase?,
    onDismiss: () -> Unit,
    onConfirm: (totalAmount: Long, paidAmount: Long, date: Long, description: String) -> Unit
) {
    var totalAmountText by remember {
        mutableStateOf(if (purchase != null) CurrencyUtils.paisaToEditableString(purchase.totalAmount) else "")
    }
    var paidAmountText by remember {
        mutableStateOf(if (purchase != null) CurrencyUtils.paisaToEditableString(purchase.paidAmount) else "")
    }
    var description by remember { mutableStateOf(purchase?.description ?: "") }
    var selectedDate by remember { mutableStateOf(purchase?.date ?: System.currentTimeMillis()) }
    var isTotalAmountError by remember { mutableStateOf(false) }
    var isPaidAmountError by remember { mutableStateOf(false) }
    var paidErrorMessage by remember { mutableStateOf("Paid amount cannot exceed total bill") }
    var showDatePicker by remember { mutableStateOf(false) }

    val totalPaisa = CurrencyUtils.parseToPaisa(totalAmountText) ?: 0L
    val paidPaisa = CurrencyUtils.parseToPaisaAllowZero(paidAmountText) ?: 0L
    val liveRemaining = totalPaisa - paidPaisa
    val isPaidExceedsTotal = totalPaisa > 0L && paidPaisa > totalPaisa

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (purchase == null) "Add Purchase" else "Edit Purchase",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Supplier: $supplierName",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedTextField(
                    value = totalAmountText,
                    onValueChange = {
                        totalAmountText = it
                        isTotalAmountError = false
                        isPaidAmountError = false
                    },
                    label = { Text("Total Bill Amount (PKR) *") },
                    placeholder = { Text("e.g. 50000") },
                    prefix = { Text("PKR ") },
                    isError = isTotalAmountError,
                    supportingText = {
                        if (isTotalAmountError) {
                            Text("Enter a valid positive amount", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("purchase_total_amount_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = paidAmountText,
                    onValueChange = {
                        paidAmountText = it
                        isPaidAmountError = false
                    },
                    label = { Text("Amount Paid Now (PKR)") },
                    placeholder = { Text("0 if fully on credit") },
                    prefix = { Text("PKR ") },
                    isError = isPaidAmountError || isPaidExceedsTotal,
                    supportingText = {
                        if (isPaidAmountError || isPaidExceedsTotal) {
                            Text(paidErrorMessage, color = MaterialTheme.colorScheme.error)
                        } else {
                            Text("Enter 0 or leave blank if buying on credit")
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("purchase_paid_amount_input")
                )

                // Show remaining = total - paid live as the user types
                if (totalPaisa > 0L) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isPaidExceedsTotal) LossRed.copy(alpha = 0.12f)
                        else if (liveRemaining > 0L) WarningAmber.copy(alpha = 0.12f)
                        else ProfitGreen.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isPaidExceedsTotal) "Error: Paid exceeds total" else "Remaining Due:",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = if (isPaidExceedsTotal) LossRed else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isPaidExceedsTotal) "Invalid"
                                else if (liveRemaining == 0L) "PKR 0 (Fully Paid)"
                                else CurrencyUtils.formatPaisa(liveRemaining),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isPaidExceedsTotal) LossRed
                                else if (liveRemaining > 0L) WarningAmber
                                else ProfitGreen
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Date Selector
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(DateUtils.formatDateShort(selectedDate))
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Invoice / Items Description") },
                    placeholder = { Text("e.g. 10 cartons oil, Invoice #884") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedTotal = CurrencyUtils.parseToPaisa(totalAmountText)
                    val parsedPaid = CurrencyUtils.parseToPaisaAllowZero(paidAmountText)

                    if (parsedTotal == null || parsedTotal <= 0L) {
                        isTotalAmountError = true
                        return@Button
                    }
                    if (parsedPaid == null || parsedPaid < 0L) {
                        isPaidAmountError = true
                        paidErrorMessage = "Enter a valid paid amount (0 or positive)"
                        return@Button
                    }
                    if (parsedPaid > parsedTotal) {
                        isPaidAmountError = true
                        paidErrorMessage = "Paid amount cannot exceed total bill"
                        return@Button
                    }

                    onConfirm(parsedTotal, parsedPaid, selectedDate, description)
                },
                modifier = Modifier.testTag("save_purchase_button")
            ) {
                Text(if (purchase == null) "Add Purchase" else "Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        shape = RoundedCornerShape(20.dp)
    )

    if (showDatePicker) {
        DatePickerModal(
            initialDateMillis = selectedDate,
            onDateSelected = { selectedDate = it },
            onDismiss = { showDatePicker = false }
        )
    }
}

@Composable
fun AddEditSupplierPaymentDialog(
    supplierId: Long,
    supplierName: String,
    currentBalance: Long,
    payment: SupplierPayment?,
    onDismiss: () -> Unit,
    onConfirm: (amount: Long, date: Long, note: String) -> Unit
) {
    var amountText by remember {
        mutableStateOf(if (payment != null) CurrencyUtils.paisaToEditableString(payment.amount) else "")
    }
    var note by remember { mutableStateOf(payment?.note ?: "") }
    var selectedDate by remember { mutableStateOf(payment?.date ?: System.currentTimeMillis()) }
    var isAmountError by remember { mutableStateOf(false) }
    var amountErrorMessage by remember { mutableStateOf("Enter a valid positive amount") }
    var showDatePicker by remember { mutableStateOf(false) }

    // When editing, the max balance payable includes what was already paid in this specific payment
    val maxPayable = (currentBalance + (payment?.amount ?: 0L)).coerceAtLeast(0L)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (payment == null) "Pay Now" else "Edit Payment",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Balance Due Banner
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = WarningAmber.copy(alpha = 0.12f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Supplier: $supplierName",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "Current Balance Due",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = CurrencyUtils.formatPaisa(maxPayable),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = WarningAmber
                        )
                    }
                }

                // Quick autofill chip
                if (maxPayable > 0L) {
                    Surface(
                        onClick = {
                            amountText = CurrencyUtils.paisaToEditableString(maxPayable)
                            isAmountError = false
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Pay Full Balance (${CurrencyUtils.formatPaisa(maxPayable)})",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        isAmountError = false
                    },
                    label = { Text("Payment Amount (PKR) *") },
                    placeholder = { Text("e.g. 10000") },
                    prefix = { Text("PKR ") },
                    isError = isAmountError,
                    supportingText = {
                        if (isAmountError) {
                            Text(amountErrorMessage, color = MaterialTheme.colorScheme.error)
                        } else if (maxPayable > 0L) {
                            Text("Maximum allowed: ${CurrencyUtils.formatPaisa(maxPayable)}")
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payment_amount_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = { showDatePicker = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(DateUtils.formatDateShort(selectedDate))
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Payment Note (Optional)") },
                    placeholder = { Text("e.g. Cash handed over, Bank transfer, Cheque #123") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = CurrencyUtils.parseToPaisa(amountText)
                    if (parsed == null || parsed <= 0L) {
                        isAmountError = true
                        amountErrorMessage = "Enter a valid positive amount"
                        return@Button
                    }
                    if (maxPayable > 0L && parsed > maxPayable) {
                        isAmountError = true
                        amountErrorMessage = "Amount cannot exceed current balance (${CurrencyUtils.formatPaisa(maxPayable)})"
                        return@Button
                    }
                    if (maxPayable <= 0L && parsed > 0L) {
                        isAmountError = true
                        amountErrorMessage = "Account has no outstanding balance due"
                        return@Button
                    }
                    onConfirm(parsed, selectedDate, note)
                },
                modifier = Modifier.testTag("save_payment_button")
            ) {
                Text(if (payment == null) "Pay Now" else "Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        shape = RoundedCornerShape(20.dp)
    )

    if (showDatePicker) {
        DatePickerModal(
            initialDateMillis = selectedDate,
            onDateSelected = { selectedDate = it },
            onDismiss = { showDatePicker = false }
        )
    }
}
