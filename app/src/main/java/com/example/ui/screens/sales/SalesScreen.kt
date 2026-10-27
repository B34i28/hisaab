package com.example.ui.screens.sales

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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.data.local.entity.Sale
import com.example.data.repository.CustomerSummary
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.DatePickerModal
import com.example.ui.screens.customers.AddEditCustomerDialog
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.ProfitGreen
import com.example.ui.viewmodel.ShopViewModel
import com.example.util.CurrencyUtils
import com.example.util.DateUtils
import com.example.util.LocalAppStrings
import com.example.util.NumberFormatter

@Composable
fun SalesScreen(
    viewModel: ShopViewModel,
    onOpenAddDialogRequested: Boolean = false,
    onAddDialogOpened: () -> Unit = {}
) {
    val sales by viewModel.salesList.collectAsStateWithLifecycle()
    val currentShop by viewModel.currentShop.collectAsStateWithLifecycle()
    val rawCustomers by viewModel.rawCustomersList.collectAsStateWithLifecycle()
    val customersSummary by viewModel.customersSummary.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var saleToEdit by remember { mutableStateOf<Sale?>(null) }
    var saleToDelete by remember { mutableStateOf<Sale?>(null) }

    if (onOpenAddDialogRequested) {
        showAddDialog = true
        onAddDialogOpened()
    }

    // Group sales by date header
    val groupedSales = remember(sales) {
        sales.groupBy { DateUtils.formatDateHeader(it.date) }
    }

    val totalSalesSum = remember(sales) {
        sales.sumOf { it.amount }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_sale_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Sale")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Sale", fontWeight = FontWeight.Bold)
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
                color = ProfitGreen.copy(alpha = 0.08f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TOTAL SALES RECORDED",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${sales.size} transactions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = CurrencyUtils.formatPaisa(totalSalesSum),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = ProfitGreen
                    )
                }
            }

            if (sales.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = ProfitGreen.copy(alpha = 0.12f),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Receipt,
                                    contentDescription = null,
                                    tint = ProfitGreen,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No sales recorded yet",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap the 'Add Sale' button below to record daily sales for ${currentShop?.name ?: "your shop"}.",
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
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    groupedSales.forEach { (dateHeader, salesInGroup) ->
                        val groupTotal = salesInGroup.sumOf { it.amount }
                        item(key = "header_$dateHeader") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp, bottom = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = dateHeader,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = CurrencyUtils.formatPaisa(groupTotal),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        items(salesInGroup, key = { it.id }) { sale ->
                            val customerName = remember(sale.customerId, rawCustomers) {
                                rawCustomers.find { it.id == sale.customerId }?.name
                            }
                            SaleItemCard(
                                sale = sale,
                                customerName = customerName,
                                onEdit = { saleToEdit = sale },
                                onDelete = { saleToDelete = sale }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(88.dp))
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddEditSaleDialog(
            sale = null,
            customers = rawCustomers,
            customerSummaries = customersSummary,
            onDismiss = { showAddDialog = false },
            onQuickAddCustomer = { name, phone, limit, onCreated ->
                viewModel.addCustomer(name, phone, "", limit, onCreated)
            },
            onConfirm = { amount, received, customerId, date, note ->
                viewModel.addSale(
                    amount = amount,
                    receivedAmount = received,
                    customerId = customerId,
                    date = date,
                    note = note
                )
                showAddDialog = false
            }
        )
    }

    if (saleToEdit != null) {
        AddEditSaleDialog(
            sale = saleToEdit,
            customers = rawCustomers,
            customerSummaries = customersSummary,
            onDismiss = { saleToEdit = null },
            onQuickAddCustomer = { name, phone, limit, onCreated ->
                viewModel.addCustomer(name, phone, "", limit, onCreated)
            },
            onConfirm = { amount, received, customerId, date, note ->
                saleToEdit?.let {
                    viewModel.updateSale(
                        it.copy(
                            amount = amount,
                            receivedAmount = received,
                            customerId = customerId,
                            date = date,
                            note = note
                        )
                    )
                }
                saleToEdit = null
            }
        )
    }

    if (saleToDelete != null) {
        ConfirmDeleteDialog(
            title = "Delete Sale?",
            message = "Are you sure you want to delete this sale of ${CurrencyUtils.formatPaisa(saleToDelete!!.amount)}? This action cannot be undone.",
            onDismiss = { saleToDelete = null },
            onConfirm = {
                saleToDelete?.let { viewModel.deleteSale(it) }
                saleToDelete = null
            }
        )
    }
}

@Composable
private fun SaleItemCard(
    sale: Sale,
    customerName: String?,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val isCreditSale = sale.amount > sale.receivedAmount
    val udhaarAmount = sale.amount - sale.receivedAmount

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
            .testTag("sale_card_${sale.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isCreditSale) ExpenseRed.copy(alpha = 0.12f) else ProfitGreen.copy(alpha = 0.12f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isCreditSale) Icons.Default.Receipt else Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = if (isCreditSale) ExpenseRed else ProfitGreen,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (sale.note.isNotBlank()) sale.note else "Sale",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (customerName != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = customerName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = DateUtils.formatTimeOnly(sale.date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (isCreditSale) {
                        Text(
                            text = "Received: ${NumberFormatter.formatPaisa(sale.receivedAmount)} | Udhaar: ${NumberFormatter.formatPaisa(udhaarAmount)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ExpenseRed,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    text = "+${CurrencyUtils.formatPaisa(sale.amount)}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = ProfitGreen,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.testTag("sale_menu_${sale.id}")
                    ) {
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
                            text = { Text("Edit") },
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
                                Text("Delete", color = MaterialTheme.colorScheme.error)
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
    }
}

@Composable
fun AddEditSaleDialog(
    sale: Sale?,
    customers: List<Customer>,
    customerSummaries: List<CustomerSummary>,
    onDismiss: () -> Unit,
    onQuickAddCustomer: (name: String, phone: String, limit: Long?, onCreated: (Long) -> Unit) -> Unit,
    onConfirm: (amount: Long, receivedAmount: Long, customerId: Long?, date: Long, note: String) -> Unit
) {
    val strings = LocalAppStrings.current
    var selectedCustomerId by remember { mutableStateOf(sale?.customerId) }

    var totalAmountText by remember {
        mutableStateOf(if (sale != null) CurrencyUtils.paisaToEditableString(sale.amount) else "")
    }
    var receivedAmountText by remember {
        mutableStateOf(
            if (sale != null) CurrencyUtils.paisaToEditableString(sale.receivedAmount) else ""
        )
    }
    var note by remember { mutableStateOf(sale?.note ?: "") }
    var selectedDate by remember { mutableStateOf(sale?.date ?: System.currentTimeMillis()) }

    var customerDropdownExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showQuickAddCustomerDialog by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var pendingConfirmedAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showCreditLimitWarning by remember { mutableStateOf(false) }
    var creditWarningDetails by remember { mutableStateOf("") }

    // Selected customer entity
    val selectedCustomer = remember(selectedCustomerId, customers) {
        customers.find { it.id == selectedCustomerId }
    }

    // Calculations
    val totalPaisa = NumberFormatter.parseInputToPaisa(totalAmountText) ?: 0L
    val receivedPaisa = if (selectedCustomerId == null) {
        totalPaisa
    } else {
        NumberFormatter.parseInputToPaisa(receivedAmountText) ?: 0L
    }
    val remainingUdhaar = (totalPaisa - receivedPaisa).coerceAtLeast(0L)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (sale == null) strings.addSale else strings.edit,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Customer selector
                Text(
                    text = strings.selectCustomer,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { customerDropdownExpanded = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sale_customer_selector")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = selectedCustomer?.name ?: strings.noCustomerCashOnly,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }

                    DropdownMenu(
                        expanded = customerDropdownExpanded,
                        onDismissRequest = { customerDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(strings.noCustomerCashOnly) },
                            onClick = {
                                selectedCustomerId = null
                                receivedAmountText = totalAmountText
                                customerDropdownExpanded = false
                            }
                        )

                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                            text = { Text(strings.newCustomerQuickAdd, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                            onClick = {
                                customerDropdownExpanded = false
                                showQuickAddCustomerDialog = true
                            }
                        )

                        customers.forEach { cust ->
                            val custBalance = customerSummaries.find { it.customer.id == cust.id }?.balance ?: 0L
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(cust.name, fontWeight = FontWeight.SemiBold)
                                        if (custBalance > 0L) {
                                            Text(
                                                "Owes: ${NumberFormatter.formatPaisa(custBalance)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ExpenseRed
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    selectedCustomerId = cust.id
                                    customerDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Total Sale Amount Field
                OutlinedTextField(
                    value = totalAmountText,
                    onValueChange = {
                        totalAmountText = it
                        if (selectedCustomerId == null) {
                            receivedAmountText = it
                        }
                        errorMessage = null
                    },
                    label = { Text(strings.saleAmountPrompt) },
                    placeholder = { Text("e.g. 1500") },
                    prefix = { Text("PKR ") },
                    isError = errorMessage != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sale_amount_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Amount Received Now Field
                if (selectedCustomerId != null) {
                    OutlinedTextField(
                        value = receivedAmountText,
                        onValueChange = {
                            receivedAmountText = it
                            errorMessage = null
                        },
                        label = { Text(strings.amountReceivedNow) },
                        placeholder = { Text("e.g. 500") },
                        prefix = { Text("PKR ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sale_received_amount_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Live Udhaar Banner
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (remainingUdhaar > 0) ExpenseRed.copy(alpha = 0.1f) else ProfitGreen.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.remainingUdhaar,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = NumberFormatter.formatPaisa(remainingUdhaar),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (remainingUdhaar > 0) ExpenseRed else ProfitGreen
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Date Selector
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sale_date_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Date",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = DateUtils.formatDateShort(selectedDate))
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(strings.noteOptionalPrompt) },
                    placeholder = { Text("e.g. Items purchased") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sale_note_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedTotal = NumberFormatter.parseInputToPaisa(totalAmountText)
                    if (parsedTotal == null || parsedTotal <= 0L) {
                        errorMessage = strings.enterValidAmount
                        return@Button
                    }

                    val parsedReceived = if (selectedCustomerId == null) {
                        parsedTotal
                    } else {
                        NumberFormatter.parseInputToPaisa(receivedAmountText) ?: 0L
                    }

                    if (parsedReceived < 0L) {
                        errorMessage = "Received amount cannot be negative."
                        return@Button
                    }

                    if (parsedReceived > parsedTotal) {
                        errorMessage = strings.receivedCannotExceedTotal
                        return@Button
                    }

                    val udhaar = parsedTotal - parsedReceived
                    if (udhaar > 0L && selectedCustomerId == null) {
                        errorMessage = strings.noCreditWithoutCustomer
                        return@Button
                    }

                    // Check credit limit if customer has one
                    if (selectedCustomer?.creditLimit != null && udhaar > 0L) {
                        val currentCustBal = customerSummaries.find { it.customer.id == selectedCustomerId }?.balance ?: 0L
                        val baselineBal = if (sale != null && sale.customerId == selectedCustomerId) {
                            currentCustBal - (sale.amount - sale.receivedAmount)
                        } else currentCustBal

                        val projectedBal = baselineBal + udhaar
                        if (projectedBal > selectedCustomer.creditLimit!!) {
                            creditWarningDetails = "Customer limit: ${NumberFormatter.formatPaisa(selectedCustomer.creditLimit!!)}\nNew projected balance: ${NumberFormatter.formatPaisa(projectedBal)}"
                            pendingConfirmedAction = {
                                onConfirm(parsedTotal, parsedReceived, selectedCustomerId, selectedDate, note.trim())
                            }
                            showCreditLimitWarning = true
                            return@Button
                        }
                    }

                    onConfirm(parsedTotal, parsedReceived, selectedCustomerId, selectedDate, note.trim())
                },
                modifier = Modifier.testTag("save_sale_button")
            ) {
                Text(if (sale == null) strings.recordSale else strings.saveChanges)
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

    if (showQuickAddCustomerDialog) {
        AddEditCustomerDialog(
            customer = null,
            onDismiss = { showQuickAddCustomerDialog = false },
            onConfirm = { name, phone, custNote, limit ->
                onQuickAddCustomer(name, phone, limit) { newId ->
                    selectedCustomerId = newId
                }
                showQuickAddCustomerDialog = false
            }
        )
    }

    if (showCreditLimitWarning) {
        AlertDialog(
            onDismissRequest = { showCreditLimitWarning = false },
            title = {
                Text(
                    text = strings.creditLimitWarningTitle,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = ExpenseRed
                )
            },
            text = {
                Column {
                    Text(strings.creditLimitWarningMessage)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = creditWarningDetails,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCreditLimitWarning = false
                        pendingConfirmedAction?.invoke()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                    modifier = Modifier.testTag("override_credit_limit_button")
                ) {
                    Text("Proceed Anyway")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreditLimitWarning = false }) {
                    Text(strings.cancel)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}
