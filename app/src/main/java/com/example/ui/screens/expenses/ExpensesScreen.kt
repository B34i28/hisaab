package com.example.ui.screens.expenses

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
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
import com.example.data.local.entity.Expense
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.DatePickerModal
import com.example.ui.theme.LossRed
import com.example.ui.viewmodel.ShopViewModel
import com.example.util.CurrencyUtils
import com.example.util.DateUtils

val DEFAULT_EXPENSE_CATEGORIES = listOf(
    "Rent",
    "Electricity",
    "Salary",
    "Transport",
    "Other"
)

@Composable
fun ExpensesScreen(
    viewModel: ShopViewModel,
    onOpenAddDialogRequested: Boolean = false,
    onAddDialogOpened: () -> Unit = {}
) {
    val expenses by viewModel.expensesList.collectAsStateWithLifecycle()
    val distinctCategories by viewModel.distinctCategories.collectAsStateWithLifecycle()
    val currentShop by viewModel.currentShop.collectAsStateWithLifecycle()

    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var expenseToEdit by remember { mutableStateOf<Expense?>(null) }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }

    if (onOpenAddDialogRequested) {
        showAddDialog = true
        onAddDialogOpened()
    }

    // Merge default categories with distinct existing ones
    val allCategoriesList = remember(distinctCategories) {
        (DEFAULT_EXPENSE_CATEGORIES + distinctCategories).distinct()
    }

    val filteredExpenses = remember(expenses, selectedCategoryFilter) {
        if (selectedCategoryFilter == null) expenses
        else expenses.filter { it.category.equals(selectedCategoryFilter, ignoreCase = true) }
    }

    val groupedExpenses = remember(filteredExpenses) {
        filteredExpenses.groupBy { DateUtils.formatDateHeader(it.date) }
    }

    val totalExpensesSum = remember(filteredExpenses) {
        filteredExpenses.sumOf { it.amount }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_expense_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Expense")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Expense", fontWeight = FontWeight.Bold)
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
                color = LossRed.copy(alpha = 0.08f),
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
                            text = if (selectedCategoryFilter != null) "$selectedCategoryFilter Expenses".uppercase() else "TOTAL EXPENSES",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${filteredExpenses.size} records",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = CurrencyUtils.formatPaisa(totalExpensesSum),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = LossRed
                    )
                }
            }

            // Category Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategoryFilter == null,
                        onClick = { selectedCategoryFilter = null },
                        label = { Text("All") },
                        modifier = Modifier.testTag("filter_all")
                    )
                }
                items(allCategoriesList) { cat ->
                    FilterChip(
                        selected = selectedCategoryFilter == cat,
                        onClick = {
                            selectedCategoryFilter = if (selectedCategoryFilter == cat) null else cat
                        },
                        label = { Text(cat) },
                        modifier = Modifier.testTag("filter_$cat")
                    )
                }
            }

            if (filteredExpenses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = LossRed.copy(alpha = 0.12f),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = LossRed,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (selectedCategoryFilter != null) "No $selectedCategoryFilter expenses" else "No expenses recorded yet",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap 'Add Expense' below to log utility bills, rent, wages, or supplies for ${currentShop?.name ?: "your shop"}.",
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
                    groupedExpenses.forEach { (dateHeader, expensesInGroup) ->
                        val groupTotal = expensesInGroup.sumOf { it.amount }
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

                        items(expensesInGroup, key = { it.id }) { expense ->
                            ExpenseItemCard(
                                expense = expense,
                                onEdit = { expenseToEdit = expense },
                                onDelete = { expenseToDelete = expense }
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
        AddEditExpenseDialog(
            expense = null,
            availableCategories = allCategoriesList,
            onDismiss = { showAddDialog = false },
            onConfirm = { amount, category, date, note ->
                viewModel.addExpense(amount = amount, category = category, date = date, note = note)
                showAddDialog = false
            }
        )
    }

    if (expenseToEdit != null) {
        AddEditExpenseDialog(
            expense = expenseToEdit,
            availableCategories = allCategoriesList,
            onDismiss = { expenseToEdit = null },
            onConfirm = { amount, category, date, note ->
                expenseToEdit?.let {
                    viewModel.updateExpense(
                        it.copy(
                            amount = amount,
                            category = category,
                            date = date,
                            note = note
                        )
                    )
                }
                expenseToEdit = null
            }
        )
    }

    if (expenseToDelete != null) {
        ConfirmDeleteDialog(
            title = "Delete Expense?",
            message = "Are you sure you want to delete this ${expenseToDelete!!.category} expense of ${CurrencyUtils.formatPaisa(expenseToDelete!!.amount)}? This action cannot be undone.",
            onDismiss = { expenseToDelete = null },
            onConfirm = {
                expenseToDelete?.let { viewModel.deleteExpense(it) }
                expenseToDelete = null
            }
        )
    }
}

@Composable
private fun ExpenseItemCard(
    expense: Expense,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
            .testTag("expense_card_${expense.id}")
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
                    color = LossRed.copy(alpha = 0.12f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = LossRed,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Text(
                                text = expense.category,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        if (expense.note.isNotBlank()) {
                            Text(
                                text = expense.note,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                    Text(
                        text = DateUtils.formatTimeOnly(expense.date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    text = "-${CurrencyUtils.formatPaisa(expense.amount)}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = LossRed,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )


                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.testTag("expense_menu_${expense.id}")
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
fun AddEditExpenseDialog(
    expense: Expense?,
    availableCategories: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (amount: Long, category: String, date: Long, note: String) -> Unit
) {
    var amountText by remember {
        mutableStateOf(if (expense != null) CurrencyUtils.paisaToEditableString(expense.amount) else "")
    }
    var selectedCategory by remember {
        mutableStateOf(expense?.category ?: DEFAULT_EXPENSE_CATEGORIES.first())
    }
    var customCategoryText by remember { mutableStateOf("") }
    var isCustomCategoryMode by remember {
        mutableStateOf(expense != null && !DEFAULT_EXPENSE_CATEGORIES.contains(expense.category))
    }
    var note by remember { mutableStateOf(expense?.note ?: "") }
    var selectedDate by remember { mutableStateOf(expense?.date ?: System.currentTimeMillis()) }
    var isAmountError by remember { mutableStateOf(false) }
    var isCategoryError by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    val dropdownOptions = remember(availableCategories) {
        val base = DEFAULT_EXPENSE_CATEGORIES.toMutableList()
        availableCategories.forEach { if (!base.contains(it)) base.add(it) }
        base.add("Custom...")
        base
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (expense == null) "Add Expense" else "Edit Expense",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        isAmountError = false
                    },
                    label = { Text("Expense Amount (PKR) *") },
                    placeholder = { Text("e.g. 2000") },
                    prefix = { Text("PKR ") },
                    isError = isAmountError,
                    supportingText = {
                        if (isAmountError) {
                            Text(
                                "Enter a valid positive amount",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_amount_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Category Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = if (isCustomCategoryMode) "Custom: $customCategoryText" else selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category *") },
                        trailingIcon = {
                            IconButton(onClick = { categoryDropdownExpanded = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Category")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { categoryDropdownExpanded = true }
                            .testTag("expense_category_selector")
                    )

                    DropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        dropdownOptions.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt) },
                                onClick = {
                                    categoryDropdownExpanded = false
                                    if (opt == "Custom...") {
                                        isCustomCategoryMode = true
                                    } else {
                                        isCustomCategoryMode = false
                                        selectedCategory = opt
                                    }
                                }
                            )
                        }
                    }
                }

                if (isCustomCategoryMode) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customCategoryText,
                        onValueChange = {
                            customCategoryText = it
                            isCategoryError = false
                        },
                        label = { Text("Custom Category Name *") },
                        placeholder = { Text("e.g. Packaging, Repairs, Cleaning") },
                        isError = isCategoryError,
                        supportingText = {
                            if (isCategoryError) {
                                Text("Please enter a category name", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_category_input")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Date Selector
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_date_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Date",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = DateUtils.formatDateShort(selectedDate))
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note / Remarks (Optional)") },
                    placeholder = { Text("e.g. Paid to electricity bill, shop rent") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_note_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = CurrencyUtils.parseToPaisa(amountText)
                    val effectiveCategory = if (isCustomCategoryMode) customCategoryText.trim() else selectedCategory.trim()

                    if (parsed == null || parsed <= 0L) {
                        isAmountError = true
                        return@Button
                    }
                    if (effectiveCategory.isBlank()) {
                        isCategoryError = true
                        return@Button
                    }
                    onConfirm(parsed, effectiveCategory, selectedDate, note)
                },
                modifier = Modifier.testTag("save_expense_button")
            ) {
                Text(if (expense == null) "Record Expense" else "Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
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
