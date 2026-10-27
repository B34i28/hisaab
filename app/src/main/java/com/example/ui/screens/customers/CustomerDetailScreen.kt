package com.example.ui.screens.customers

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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.Customer
import com.example.data.local.entity.CustomerPayment
import com.example.data.local.entity.CustomerWriteOff
import com.example.data.local.entity.Sale
import com.example.data.repository.CustomerLedgerItem
import com.example.data.repository.CustomerLedgerType
import com.example.data.repository.CustomerSummary
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.DatePickerModal
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.ProfitGreen
import com.example.ui.viewmodel.ShopViewModel
import com.example.util.CurrencyUtils
import com.example.util.DateUtils
import com.example.util.LocalAppStrings
import com.example.util.NumberFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    customerId: Long,
    viewModel: ShopViewModel,
    onNavigateBack: () -> Unit
) {
    BackHandler { onNavigateBack() }
    val strings = LocalAppStrings.current
    val context = LocalContext.current

    val customer by viewModel.getCustomerById(customerId).collectAsStateWithLifecycle()
    val ledger by viewModel.getCustomerLedger(customerId).collectAsStateWithLifecycle()
    val customersSummary by viewModel.customersSummary.collectAsStateWithLifecycle()

    val summary = remember(customersSummary, customerId) {
        customersSummary.find { it.customer.id == customerId } ?: CustomerSummary(
            customer = customer ?: Customer(id = customerId, shopId = 0L, name = ""),
            totalCreditGiven = 0L,
            totalReceivedBack = 0L,
            totalWriteOffs = 0L,
            balance = 0L
        )
    }

    var showEditCustomerDialog by remember { mutableStateOf(false) }
    var showDeleteCustomerDialog by remember { mutableStateOf(false) }

    var showPaymentDialog by remember { mutableStateOf(false) }
    var paymentToEdit by remember { mutableStateOf<CustomerPayment?>(null) }
    var paymentToDelete by remember { mutableStateOf<CustomerPayment?>(null) }

    var showWriteOffDialog by remember { mutableStateOf(false) }
    var writeOffToEdit by remember { mutableStateOf<CustomerWriteOff?>(null) }
    var writeOffToDelete by remember { mutableStateOf<CustomerWriteOff?>(null) }

    var saleToDelete by remember { mutableStateOf<Sale?>(null) }

    if (customer == null) {
        Scaffold { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Customer not found", style = MaterialTheme.typography.bodyLarge)
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = customer!!.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        if (customer!!.phone.isNotBlank()) {
                            Text(
                                text = customer!!.phone,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                    }
                },
                actions = {
                    // Share Statement
                    IconButton(
                        onClick = {
                            val text = viewModel.generateCustomerStatementText(customer!!, ledger, summary)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            context.startActivity(Intent.createChooser(intent, strings.shareStatement))
                        },
                        modifier = Modifier.testTag("share_statement_button")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = strings.shareStatement)
                    }

                    // Edit Customer
                    IconButton(
                        onClick = { showEditCustomerDialog = true },
                        modifier = Modifier.testTag("edit_customer_button")
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = strings.editCustomer)
                    }

                    // Delete Customer
                    IconButton(
                        onClick = { showDeleteCustomerDialog = true },
                        modifier = Modifier.testTag("delete_customer_button")
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Customer", tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                // Summary Card
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.currentBalance.uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                            val limit = customer?.creditLimit
                            if (limit != null) {
                                Text(
                                    text = "Limit: ${NumberFormatter.formatPaisa(limit)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = NumberFormatter.formatPaisa(summary.balance),
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (summary.balance > 0) ExpenseRed else ProfitGreen
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = strings.totalCreditGiven,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = NumberFormatter.formatPaisa(summary.totalCreditGiven),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }

                            Column {
                                Text(
                                    text = strings.totalReceivedBack,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = NumberFormatter.formatPaisa(summary.totalReceivedBack),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = ProfitGreen
                                )
                            }

                            if (summary.totalWriteOffs > 0) {
                                Column {
                                    Text(
                                        text = strings.totalWriteOffs,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = NumberFormatter.formatPaisa(summary.totalWriteOffs),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = ExpenseRed
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons: Receive Payment, Write Off, Send Reminder
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Receive Payment
                    Button(
                        onClick = { showPaymentDialog = true },
                        enabled = summary.balance > 0L,
                        colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("receive_payment_button")
                    ) {
                        Icon(imageVector = Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.receivePayment, style = MaterialTheme.typography.labelMedium)
                    }

                    // Write Off
                    OutlinedButton(
                        onClick = { showWriteOffDialog = true },
                        enabled = summary.balance > 0L,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseRed),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("write_off_button")
                    ) {
                        Icon(imageVector = Icons.Default.RemoveCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.writeOff, style = MaterialTheme.typography.labelMedium)
                    }

                    // Send Reminder
                    Button(
                        onClick = {
                            val reminderText = viewModel.generateCustomerReminderText(customer!!.name, summary.balance)
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, reminderText)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, strings.sendReminder))
                        },
                        enabled = summary.balance > 0L,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("send_reminder_button")
                    ) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.sendReminder, style = MaterialTheme.typography.labelMedium)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = strings.customerLedgerTitle,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            if (ledger.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = strings.noTransactionsYet,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(ledger, key = { "${it.type}_${it.id}" }) { item ->
                    CustomerLedgerRow(
                        item = item,
                        onEdit = {
                            when (item.type) {
                                CustomerLedgerType.PAYMENT -> paymentToEdit = item.sourcePayment
                                CustomerLedgerType.WRITE_OFF -> writeOffToEdit = item.sourceWriteOff
                                CustomerLedgerType.SALE -> {
                                    // Sale editing can be handled or alerted
                                }
                            }
                        },
                        onDelete = {
                            when (item.type) {
                                CustomerLedgerType.PAYMENT -> paymentToDelete = item.sourcePayment
                                CustomerLedgerType.WRITE_OFF -> writeOffToDelete = item.sourceWriteOff
                                CustomerLedgerType.SALE -> saleToDelete = item.sourceSale
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // Dialogs:
    // Edit Customer
    if (showEditCustomerDialog) {
        AddEditCustomerDialog(
            customer = customer,
            onDismiss = { showEditCustomerDialog = false },
            onConfirm = { name, phone, note, limit ->
                viewModel.updateCustomer(
                    customer!!.copy(
                        name = name,
                        phone = phone,
                        note = note,
                        creditLimit = limit
                    )
                )
                showEditCustomerDialog = false
            }
        )
    }

    // Delete Customer
    if (showDeleteCustomerDialog) {
        ConfirmDeleteDialog(
            title = "Delete ${customer!!.name}?",
            message = "This will remove the customer and their transactions. This action cannot be undone.",
            onDismiss = { showDeleteCustomerDialog = false },
            onConfirm = {
                viewModel.deleteCustomer(customer!!)
                showDeleteCustomerDialog = false
                onNavigateBack()
            }
        )
    }

    // Add Payment Dialog
    if (showPaymentDialog) {
        RecordCustomerPaymentDialog(
            currentBalance = summary.balance,
            onDismiss = { showPaymentDialog = false },
            onConfirm = { amount, date, note ->
                viewModel.addCustomerPayment(customerId, amount, date, note)
                showPaymentDialog = false
            }
        )
    }

    // Edit Payment Dialog
    if (paymentToEdit != null) {
        RecordCustomerPaymentDialog(
            initialPayment = paymentToEdit,
            currentBalance = summary.balance + paymentToEdit!!.amount,
            onDismiss = { paymentToEdit = null },
            onConfirm = { amount, date, note ->
                viewModel.updateCustomerPayment(
                    paymentToEdit!!.copy(
                        amount = amount,
                        date = date,
                        note = note
                    )
                )
                paymentToEdit = null
            }
        )
    }

    // Delete Payment Confirm
    if (paymentToDelete != null) {
        ConfirmDeleteDialog(
            title = "Delete Payment?",
            message = "Are you sure you want to delete this payment of ${NumberFormatter.formatPaisa(paymentToDelete!!.amount)}? The customer's balance will increase accordingly.",
            onDismiss = { paymentToDelete = null },
            onConfirm = {
                viewModel.deleteCustomerPayment(paymentToDelete!!)
                paymentToDelete = null
            }
        )
    }

    // Add Write-Off Dialog
    if (showWriteOffDialog) {
        RecordWriteOffDialog(
            currentBalance = summary.balance,
            onDismiss = { showWriteOffDialog = false },
            onConfirm = { amount, date, note ->
                viewModel.addCustomerWriteOff(customerId, amount, date, note)
                showWriteOffDialog = false
            }
        )
    }

    // Edit Write-Off Dialog
    if (writeOffToEdit != null) {
        RecordWriteOffDialog(
            initialWriteOff = writeOffToEdit,
            currentBalance = summary.balance + writeOffToEdit!!.amount,
            onDismiss = { writeOffToEdit = null },
            onConfirm = { amount, date, note ->
                viewModel.updateCustomerWriteOff(
                    writeOffToEdit!!.copy(
                        amount = amount,
                        date = date,
                        note = note
                    )
                )
                writeOffToEdit = null
            }
        )
    }

    // Delete Write-Off Confirm
    if (writeOffToDelete != null) {
        ConfirmDeleteDialog(
            title = "Delete Write-Off?",
            message = "Are you sure you want to delete this write-off of ${NumberFormatter.formatPaisa(writeOffToDelete!!.amount)}? The balance will be restored and profit adjusted.",
            onDismiss = { writeOffToDelete = null },
            onConfirm = {
                viewModel.deleteCustomerWriteOff(writeOffToDelete!!)
                writeOffToDelete = null
            }
        )
    }

    // Delete Sale Confirm
    if (saleToDelete != null) {
        ConfirmDeleteDialog(
            title = "Delete Sale?",
            message = "Are you sure you want to delete this sale of ${NumberFormatter.formatPaisa(saleToDelete!!.amount)}? The customer's balance and sales will be updated.",
            onDismiss = { saleToDelete = null },
            onConfirm = {
                viewModel.deleteSale(saleToDelete!!)
                saleToDelete = null
            }
        )
    }
}

@Composable
fun CustomerLedgerRow(
    item: CustomerLedgerItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val strings = LocalAppStrings.current
    var menuExpanded by remember { mutableStateOf(false) }

    val icon = when (item.type) {
        CustomerLedgerType.SALE -> Icons.Default.Receipt
        CustomerLedgerType.PAYMENT -> Icons.Default.Payments
        CustomerLedgerType.WRITE_OFF -> Icons.Default.RemoveCircleOutline
    }

    val iconColor = when (item.type) {
        CustomerLedgerType.SALE -> ExpenseRed
        CustomerLedgerType.PAYMENT -> ProfitGreen
        CustomerLedgerType.WRITE_OFF -> MaterialTheme.colorScheme.outline
    }

    val typeLabel = when (item.type) {
        CustomerLedgerType.SALE -> strings.creditSaleItem
        CustomerLedgerType.PAYMENT -> strings.customerPaymentItem
        CustomerLedgerType.WRITE_OFF -> strings.writeOffItem
    }

    val amountPrefix = when (item.type) {
        CustomerLedgerType.SALE -> "+ "
        CustomerLedgerType.PAYMENT -> "- "
        CustomerLedgerType.WRITE_OFF -> "- "
    }

    val amountColor = when (item.type) {
        CustomerLedgerType.SALE -> ExpenseRed
        CustomerLedgerType.PAYMENT -> ProfitGreen
        CustomerLedgerType.WRITE_OFF -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val displayedAmount = when (item.type) {
        CustomerLedgerType.SALE -> item.creditAmount
        CustomerLedgerType.PAYMENT -> item.amount
        CustomerLedgerType.WRITE_OFF -> item.amount
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = iconColor.copy(alpha = 0.12f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = typeLabel,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = DateUtils.formatDateTime(item.date),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (item.note.isNotBlank()) {
                    Text(
                        text = item.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$amountPrefix${NumberFormatter.formatPaisa(displayedAmount)}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = amountColor
                )
                Text(
                    text = "Bal: ${NumberFormatter.formatPaisa(item.runningBalance)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Options")
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    if (item.type != CustomerLedgerType.SALE) {
                        DropdownMenuItem(
                            text = { Text(strings.edit) },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(strings.delete, color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
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

@Composable
fun RecordCustomerPaymentDialog(
    initialPayment: CustomerPayment? = null,
    currentBalance: Long,
    onDismiss: () -> Unit,
    onConfirm: (amount: Long, date: Long, note: String) -> Unit
) {
    val strings = LocalAppStrings.current
    var amountText by remember {
        mutableStateOf(if (initialPayment != null) CurrencyUtils.paisaToEditableString(initialPayment.amount) else "")
    }
    var note by remember { mutableStateOf(initialPayment?.note ?: "") }
    var selectedDate by remember { mutableStateOf(initialPayment?.date ?: System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialPayment == null) strings.receivePayment else strings.edit,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "${strings.currentBalance}: ${NumberFormatter.formatPaisa(currentBalance)}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = ExpenseRed
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        errorMessage = null
                    },
                    label = { Text(strings.paymentAmount) },
                    placeholder = { Text("e.g. 500") },
                    prefix = { Text("PKR ") },
                    isError = errorMessage != null,
                    supportingText = {
                        if (errorMessage != null) {
                            Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_payment_amount_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { showDatePicker = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(DateUtils.formatDateShort(selectedDate))
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(strings.paymentNote) },
                    placeholder = { Text("Cash, EasyPaisa, etc.") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = NumberFormatter.parseInputToPaisa(amountText)
                    if (parsed == null || parsed <= 0L) {
                        errorMessage = strings.enterValidAmount
                    } else if (parsed > currentBalance) {
                        errorMessage = strings.amountExceedsBalance
                    } else {
                        onConfirm(parsed, selectedDate, note.trim())
                    }
                },
                modifier = Modifier.testTag("confirm_receive_payment_button")
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

    if (showDatePicker) {
        DatePickerModal(
            initialDateMillis = selectedDate,
            onDateSelected = { selectedDate = it },
            onDismiss = { showDatePicker = false }
        )
    }
}

@Composable
fun RecordWriteOffDialog(
    initialWriteOff: CustomerWriteOff? = null,
    currentBalance: Long,
    onDismiss: () -> Unit,
    onConfirm: (amount: Long, date: Long, note: String) -> Unit
) {
    val strings = LocalAppStrings.current
    var amountText by remember {
        mutableStateOf(if (initialWriteOff != null) CurrencyUtils.paisaToEditableString(initialWriteOff.amount) else "")
    }
    var note by remember { mutableStateOf(initialWriteOff?.note ?: "") }
    var selectedDate by remember { mutableStateOf(initialWriteOff?.date ?: System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showConfirmWarning by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var validatedAmount by remember { mutableStateOf(0L) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialWriteOff == null) strings.writeOff else strings.edit,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = ExpenseRed
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "${strings.currentBalance}: ${NumberFormatter.formatPaisa(currentBalance)}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = ExpenseRed
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Writing off balance as bad debt will reduce your shop's net profit.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        errorMessage = null
                    },
                    label = { Text(strings.paymentAmount) },
                    placeholder = { Text("e.g. 500") },
                    prefix = { Text("PKR ") },
                    isError = errorMessage != null,
                    supportingText = {
                        if (errorMessage != null) {
                            Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("write_off_amount_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { showDatePicker = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(DateUtils.formatDateShort(selectedDate))
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = {
                        note = it
                        errorMessage = null
                    },
                    label = { Text("Reason / Note *") },
                    placeholder = { Text("e.g. Moved away / uncollectible") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("write_off_note_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = NumberFormatter.parseInputToPaisa(amountText)
                    if (parsed == null || parsed <= 0L) {
                        errorMessage = strings.enterValidAmount
                    } else if (parsed > currentBalance) {
                        errorMessage = strings.amountExceedsBalance
                    } else if (note.trim().isBlank()) {
                        errorMessage = strings.writeOffNoteRequired
                    } else {
                        validatedAmount = parsed
                        showConfirmWarning = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                modifier = Modifier.testTag("confirm_write_off_button")
            ) {
                Text(strings.writeOff)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
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

    if (showConfirmWarning) {
        AlertDialog(
            onDismissRequest = { showConfirmWarning = false },
            title = { Text(strings.writeOffConfirmTitle, fontWeight = FontWeight.Bold, color = ExpenseRed) },
            text = { Text(strings.writeOffConfirmMessage) },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmWarning = false
                        onConfirm(validatedAmount, selectedDate, note.trim())
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) {
                    Text(strings.confirm)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmWarning = false }) {
                    Text(strings.cancel)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}
