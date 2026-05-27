package com.hktech.personalexpensetracker.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.hktech.personalexpensetracker.data.AccountEntity
import com.hktech.personalexpensetracker.data.CategoryEntity
import com.hktech.personalexpensetracker.data.TransactionEntity
import com.hktech.personalexpensetracker.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TransactionList(
    txns: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    accounts: List<AccountEntity>,
    onChangeCategory: (Long, String) -> Unit,
    onDeleteTransaction: (Long) -> Unit,
    onUpdateTransaction: (Long, Double, String, String?, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val grouped = remember(txns) { groupTransactionsByDate(txns) }

    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        grouped.forEach { (dateHeader, transactions) ->
            item(key = "header_$dateHeader") {
                Text(
                    text = dateHeader,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            items(
                items = transactions,
                key = { it.id }
            ) { txn ->
                TransactionCard(
                    transaction = txn,
                    categories = categories,
                    accounts = accounts,
                    onChangeCategory = onChangeCategory,
                    onDeleteTransaction = onDeleteTransaction,
                    onUpdateTransaction = onUpdateTransaction
                )
            }
        }
    }
}

@Composable
fun TransactionCard(
    transaction: TransactionEntity,
    categories: List<CategoryEntity>,
    accounts: List<AccountEntity>,
    onChangeCategory: (Long, String) -> Unit,
    onDeleteTransaction: (Long) -> Unit,
    onUpdateTransaction: (Long, Double, String, String?, String?) -> Unit
) {
    val isCredit = transaction.direction == "CREDIT"
    val backgroundColor = if (isCredit) CreditGreen else DebitRed
    val textColor = if (isCredit) CreditGreenText else DebitRedText
    // Use custom color for custom categories, default color for built-in ones
    val categoryColor = categories.find { it.name == transaction.category }?.let { cat ->
        if (cat.name in CategoryColors) safeCategoryColor(cat.name) else safeParseColor(cat.color)
    } ?: safeCategoryColor(transaction.category)

    val accountName = transaction.accountId?.let { accId ->
        accounts.find { it.id == accId }?.let {
            it.nickname.ifBlank { "${it.bankName} ****${it.cardSuffix}" }
        }
    }

    var showCategoryDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDetails by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = textColor.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            ),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            onClick = { showDetails = true }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transaction.merchant ?: "Unknown Merchant",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AssistChip(
                            onClick = { },
                            label = {
                                Text(
                                    text = transaction.channel ?: "OTHER",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.height(24.dp),
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                        if (accountName != null) {
                            AssistChip(
                                onClick = { },
                                label = {
                                    Text(
                                        text = accountName,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                modifier = Modifier.height(24.dp),
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            )
                        }
                        if (transaction.isTransfer) {
                            AssistChip(
                                onClick = { },
                                label = {
                                    Text(
                                        text = "Transfer",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                modifier = Modifier.height(24.dp),
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = Color(0xFFE3F2FD)
                                )
                            )
                        }
                        Text(
                            text = formatTime(transaction.ts),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = textColor)) {
                                append(if (isCredit) "+₹" else "-₹")
                            }
                            append(formatAmount(transaction.amount))
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Category chip - clickable
            Surface(
                onClick = { showCategoryDialog = true },
                shape = RoundedCornerShape(16.dp),
                color = categoryColor.copy(alpha = 0.2f),
                modifier = Modifier.clip(RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(categoryColor, RoundedCornerShape(4.dp))
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = transaction.category,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "▼",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Raw SMS preview
            Text(
                text = transaction.rawText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp)
            )

            // Delete button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    if (showCategoryDialog) {
        CategorySelectionDialog(
            categories = categories,
            currentCategory = transaction.category,
            onCategorySelected = { category ->
                onChangeCategory(transaction.id, category)
                showCategoryDialog = false
            },
            onDismiss = { showCategoryDialog = false }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Transaction?") },
            text = {
                Text("Are you sure you want to delete this transaction? This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteTransaction(transaction.id)
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDetails) {
        TransactionDetailsSheet(
            transaction = transaction,
            onDismiss = { showDetails = false },
            onUpdateTransaction = onUpdateTransaction
        )
    }
}

@Composable
fun CategorySelectionDialog(
    categories: List<CategoryEntity>,
    currentCategory: String,
    onCategorySelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Category") },
        text = {
            Column {
                categories.forEach { category ->
                    val isSelected = category.name == currentCategory
                    val displayColor = if (category.name in CategoryColors) safeCategoryColor(category.name) else safeParseColor(category.color)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCategorySelected(category.name) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(displayColor, RoundedCornerShape(6.dp))
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = category.name,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Text("✓", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailsSheet(
    transaction: TransactionEntity,
    onDismiss: () -> Unit,
    onUpdateTransaction: (Long, Double, String, String?, String?) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editAmount by remember { mutableStateOf(transaction.amount.toString()) }
    var editDirection by remember { mutableStateOf(transaction.direction) }
    var editMerchant by remember { mutableStateOf(transaction.merchant ?: "") }
    var editChannel by remember { mutableStateOf(transaction.channel ?: "OTHER") }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val channels = listOf("UPI", "CARD", "NETBANKING", "CASH", "WALLET", "ATM", "OTHER")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header with Edit button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (isEditing) {
                        OutlinedTextField(
                            value = editMerchant,
                            onValueChange = { editMerchant = it },
                            label = { Text("Merchant") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                    } else {
                        Text(
                            text = transaction.merchant ?: "Unknown Merchant",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                IconButton(
                    onClick = {
                        if (isEditing) {
                            // Save changes
                            val newAmount = editAmount.toDoubleOrNull() ?: transaction.amount
                            onUpdateTransaction(
                                transaction.id,
                                newAmount,
                                editDirection,
                                editMerchant.ifBlank { null },
                                editChannel
                            )
                        }
                        isEditing = !isEditing
                    }
                ) {
                    Icon(
                        imageVector = if (isEditing) Icons.Default.Check else Icons.Default.Clear,
                        contentDescription = if (isEditing) "Save" else "Edit",
                        tint = if (isEditing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Direction selector
            if (isEditing) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Type:", style = MaterialTheme.typography.bodyMedium)
                    listOf("DEBIT", "CREDIT").forEach { dir ->
                        FilterChip(
                            selected = editDirection == dir,
                            onClick = { editDirection = dir },
                            label = { Text(if (dir == "DEBIT") "Expense" else "Income") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (dir == "CREDIT") CreditGreen.copy(alpha = 0.2f) else DebitRed.copy(alpha = 0.2f)
                            )
                        )
                    }
                }
            } else {
                Text(
                    text = if (transaction.direction == "CREDIT") "Credit" else "Debit",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (transaction.direction == "CREDIT") CreditGreenText else DebitRedText
                )
            }

            Spacer(Modifier.height(12.dp))

            // Amount
            if (isEditing) {
                OutlinedTextField(
                    value = editAmount,
                    onValueChange = { editAmount = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Amount") },
                    prefix = { Text("₹") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(8.dp)
                )
            } else {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = if (transaction.direction == "CREDIT") CreditGreenText else DebitRedText)) {
                            append(if (transaction.direction == "CREDIT") "+₹" else "-₹")
                        }
                        append(formatAmount(transaction.amount))
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            // Details grid
            if (isEditing) {
                // Channel selector
                Text("Payment Method", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(channels) { ch ->
                        FilterChip(
                            selected = editChannel == ch,
                            onClick = { editChannel = ch },
                            label = { Text(ch) }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            DetailRow("Category", transaction.category)
            DetailRow("Payment Method", if (isEditing) editChannel else (transaction.channel ?: "OTHER"))
            DetailRow("Date", SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault()).format(Date(transaction.ts)))
            DetailRow("Time", SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(transaction.ts)))
            DetailRow("Source", transaction.source)
            DetailRow("Confidence", "${transaction.confidence}%")
            transaction.accountHint?.let { DetailRow("Account", "****$it") }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            // Full SMS
            Text("Full Message", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = transaction.rawText,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDialogContent(
    currentDirection: String?,
    currentChannel: String?,
    currentMinAmount: Double?,
    currentMaxAmount: Double?,
    currentStartDate: Long?,
    currentEndDate: Long?,
    onApply: (String?, String?, Double?, Double?, Long?, Long?) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    var direction by remember { mutableStateOf(currentDirection) }
    var channel by remember { mutableStateOf(currentChannel) }
    var minAmount by remember { mutableStateOf(currentMinAmount?.toString() ?: "") }
    var maxAmount by remember { mutableStateOf(currentMaxAmount?.toString() ?: "") }
    var startDate by remember { mutableStateOf(currentStartDate) }
    var endDate by remember { mutableStateOf(currentEndDate) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    val directions = listOf("DEBIT", "CREDIT")
    val channels = listOf("UPI", "CARD", "NETBANKING", "CASH", "WALLET", "ATM", "OTHER")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter Transactions", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Direction
                Text("Transaction Type", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    directions.forEach { dir ->
                        FilterChip(
                            selected = direction == dir,
                            onClick = { direction = if (direction == dir) null else dir },
                            label = { Text(if (dir == "DEBIT") "Expense" else "Income") }
                        )
                    }
                }

                // Channel
                Text("Payment Method", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(channels) { ch ->
                        FilterChip(
                            selected = channel == ch,
                            onClick = { channel = if (channel == ch) null else ch },
                            label = { Text(ch) }
                        )
                    }
                }

                // Amount range
                Text("Amount Range", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = minAmount,
                        onValueChange = { minAmount = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Min") },
                        prefix = { Text("₹") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = maxAmount,
                        onValueChange = { maxAmount = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Max") },
                        prefix = { Text("₹") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                // Date range
                Text("Date Range", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startDate?.let { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(it)) } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("From") },
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showStartDatePicker = true },
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = endDate?.let { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(it)) } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("To") },
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showEndDatePicker = true },
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onApply(
                    direction,
                    channel,
                    minAmount.toDoubleOrNull(),
                    maxAmount.toDoubleOrNull(),
                    startDate,
                    endDate
                )
            }) {
                Text("Apply")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onClear) {
                    Text("Clear All")
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )

    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = startDate ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { startDate = it }
                    showStartDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = endDate ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { endDate = it }
                    showEndDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

// Helper functions
private fun groupTransactionsByDate(txns: List<TransactionEntity>): Map<String, List<TransactionEntity>> {
    val now = Calendar.getInstance()
    val today = getStartOfDay(now)
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }.let { getStartOfDay(it) }
    val lastWeek = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }.let { getStartOfDay(it) }

    return txns.groupBy { txn ->
        val txnDay = getStartOfDay(Calendar.getInstance().apply { timeInMillis = txn.ts })
        when {
            txnDay >= today -> "Today"
            txnDay >= yesterday -> "Yesterday"
            txnDay >= lastWeek -> {
                SimpleDateFormat("EEEE", Locale.getDefault()).format(Date(txn.ts))
            }
            else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(txn.ts))
        }
    }.toSortedMap { k1, k2 ->
        val order = listOf("Today", "Yesterday")
        val idx1 = order.indexOf(k1).let { if (it >= 0) it else Int.MAX_VALUE }
        val idx2 = order.indexOf(k2).let { if (it >= 0) it else Int.MAX_VALUE }
        if (idx1 != idx2) idx1.compareTo(idx2) else k2.compareTo(k1)
    }
}

private fun getStartOfDay(cal: Calendar): Long {
    return cal.apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun formatTime(ts: Long): String {
    return SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(ts))
}

internal fun formatAmount(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale("en", "IN"))
    formatter.minimumFractionDigits = 2
    formatter.maximumFractionDigits = 2
    return formatter.format(amount)
}