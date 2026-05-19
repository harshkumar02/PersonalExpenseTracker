package com.hktech.personalexpensetracker.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hktech.personalexpensetracker.data.AccountEntity
import com.hktech.personalexpensetracker.data.CategoryEntity
import com.hktech.personalexpensetracker.data.MerchantEntity
import com.hktech.personalexpensetracker.data.PaymentChannelEntity
import com.hktech.personalexpensetracker.data.TransactionEntity
import com.hktech.personalexpensetracker.ingest.TransactionParser
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionFab(
    onAddTransaction: (TransactionEntity) -> Unit,
    categories: List<CategoryEntity>,
    onAddCategory: (CategoryEntity) -> Unit,
    onDeleteCategory: (String) -> Unit,
    merchants: List<MerchantEntity>,
    onAddMerchant: (MerchantEntity) -> Unit,
    onDeleteMerchant: (String) -> Unit,
    accounts: List<AccountEntity>,
    onAddAccount: (AccountEntity) -> Unit,
    onUpdateAccount: (AccountEntity) -> Unit,
    onDeleteAccount: (Long) -> Unit,
    paymentChannels: List<PaymentChannelEntity> = emptyList(),
    onAddPaymentChannel: (PaymentChannelEntity) -> Unit = {},
    onDeletePaymentChannel: (String) -> Unit = {}
) {
    var showDialog by remember { mutableStateOf(false) }
    var showCategoryManagement by remember { mutableStateOf(false) }
    var showMerchantManagement by remember { mutableStateOf(false) }
    var showTestParser by remember { mutableStateOf(false) }
    var showChannelManagement by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomStart
    ) {
        Column(
            horizontalAlignment = Alignment.Start
        ) {
            SmallFloatingActionButton(
                onClick = { showTestParser = true },
                modifier = Modifier.padding(start = 24.dp, bottom = 8.dp),
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.BugReport,
                    contentDescription = "Test Parser"
                )
            }
            FloatingActionButton(
                onClick = { showDialog = true },
                modifier = Modifier.padding(start = 24.dp, bottom = 16.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Transaction"
                )
            }
        }
    }

    if (showDialog) {
        AddTransactionDialog(
            categories = categories,
            accounts = accounts,
            paymentChannels = paymentChannels,
            onDismiss = { showDialog = false },
            onManageCategories = { showCategoryManagement = true },
            onManageMerchants = { showMerchantManagement = true },
            onManageChannels = { showChannelManagement = true },
            onSave = { txn ->
                onAddTransaction(txn)
                showDialog = false
            }
        )
    }

    if (showCategoryManagement) {
        CategoryManagementDialog(
            categories = categories,
            onAddCategory = onAddCategory,
            onDeleteCategory = onDeleteCategory,
            onDismiss = { showCategoryManagement = false }
        )
    }

    if (showMerchantManagement) {
        MerchantManagementDialog(
            merchants = merchants,
            categories = categories,
            onAddMerchant = onAddMerchant,
            onDeleteMerchant = onDeleteMerchant,
            onDismiss = { showMerchantManagement = false }
        )
    }

    if (showTestParser) {
        TestParserDialog(
            merchants = merchants,
            onDismiss = { showTestParser = false }
        )
    }

    if (showChannelManagement) {
        PaymentChannelManagementDialog(
            channels = paymentChannels,
            onAddChannel = onAddPaymentChannel,
            onDeleteChannel = onDeletePaymentChannel,
            onDismiss = { showChannelManagement = false }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TestParserDialog(
    merchants: List<MerchantEntity>,
    onDismiss: () -> Unit
) {
    var testSms by remember { mutableStateOf("") }
    var parseResult by remember { mutableStateOf<TransactionParser.ParseResult?>(null) }
    val clipboardManager = LocalClipboardManager.current
    var copiedField by remember { mutableStateOf<String?>(null) }

    // Show toast-like feedback
    var showCopied by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Test SMS Parser", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = testSms,
                    onValueChange = {
                        testSms = it
                        parseResult = null
                    },
                    label = { Text("Paste SMS text here") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                Button(
                    onClick = {
                        if (testSms.isNotBlank()) {
                            TransactionParser.updateMerchants(merchants)
                            parseResult = TransactionParser.parse(testSms, System.currentTimeMillis())
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Parse SMS")
                }

                parseResult?.let { result ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Check if parsing actually failed (no amount found)
                        if (result.transaction.amount <= 0) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.errorContainer
                            ) {
                                Text(
                                    text = "No parse result (amount not found or OTP/skipped)",
                                    modifier = Modifier.padding(12.dp),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        } else {
                            Text(
                                "Results (tap to copy):",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )

                            result.transaction.merchant?.let { merchant ->
                                CopyableField(
                                    label = "Merchant",
                                    value = merchant,
                                    onCopy = {
                                        clipboardManager.setText(AnnotatedString(merchant))
                                        copiedField = "Merchant"
                                    }
                                )
                            }

                            CopyableField(
                                label = "Category",
                                value = result.transaction.category,
                                onCopy = {
                                    clipboardManager.setText(AnnotatedString(result.transaction.category))
                                    copiedField = "Category"
                                }
                            )

                            CopyableField(
                                label = "Amount",
                                value = "₹${"%,.2f".format(result.transaction.amount)}",
                                onCopy = {
                                    clipboardManager.setText(AnnotatedString(result.transaction.amount.toString()))
                                    copiedField = "Amount"
                                }
                            )

                            CopyableField(
                                label = "Direction",
                                value = result.transaction.direction,
                                onCopy = {
                                    clipboardManager.setText(AnnotatedString(result.transaction.direction))
                                    copiedField = "Direction"
                                }
                            )

                            result.transaction.channel?.let { channel ->
                                CopyableField(
                                    label = "Channel",
                                    value = channel,
                                    onCopy = {
                                        clipboardManager.setText(AnnotatedString(channel))
                                        copiedField = "Channel"
                                    }
                                )
                            }

                            result.balance?.let { balance ->
                                CopyableField(
                                    label = "Balance",
                                    value = "₹${"%,.2f".format(balance)}",
                                    onCopy = {
                                        clipboardManager.setText(AnnotatedString(balance.toString()))
                                        copiedField = "Balance"
                                    }
                                )
                            }

                            result.transaction.accountHint?.let { hint ->
                                CopyableField(
                                    label = "Account Hint",
                                    value = hint,
                                    onCopy = {
                                        clipboardManager.setText(AnnotatedString(hint))
                                        copiedField = "Account"
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun CopyableField(
    label: String,
    value: String,
    onCopy: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCopy),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentChannelManagementDialog(
    channels: List<PaymentChannelEntity>,
    onAddChannel: (PaymentChannelEntity) -> Unit,
    onDeleteChannel: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var newCode by remember { mutableStateOf("") }
    var newDisplayName by remember { mutableStateOf("") }
    var newKeywords by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Payment Channels", fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Settings, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                // Add new channel
                if (!showAddDialog) {
                    OutlinedButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add Channel")
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newCode,
                            onValueChange = { newCode = it.uppercase().filter { c -> c.isLetterOrDigit() || c == '_' }.take(20) },
                            label = { Text("Code (e.g., UPI)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = newDisplayName,
                            onValueChange = { newDisplayName = it },
                            label = { Text("Display Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = newKeywords,
                            onValueChange = { newKeywords = it },
                            label = { Text("Keywords (comma-separated)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            placeholder = { Text("e.g., gpay,google pay") }
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (newCode.isNotBlank() && newDisplayName.isNotBlank()) {
                                        onAddChannel(PaymentChannelEntity(
                                            code = newCode.trim(),
                                            displayName = newDisplayName.trim(),
                                            keywords = newKeywords.trim()
                                        ))
                                        newCode = ""
                                        newDisplayName = ""
                                        newKeywords = ""
                                        showAddDialog = false
                                    }
                                },
                                enabled = newCode.isNotBlank() && newDisplayName.isNotBlank()
                            ) {
                                Text("Add")
                            }
                            TextButton(onClick = { showAddDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))

                Text(
                    "Channels (${channels.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(channels.sortedBy { it.displayName }) { channel ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = channel.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Code: ${channel.code}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (channel.keywords.isNotBlank()) {
                                    Text(
                                        text = "Keywords: ${channel.keywords}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            if (channel.code != "OTHER") {
                                IconButton(
                                    onClick = { onDeleteChannel(channel.code) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    categories: List<CategoryEntity>,
    accounts: List<AccountEntity>,
    paymentChannels: List<PaymentChannelEntity>,
    onDismiss: () -> Unit,
    onManageCategories: () -> Unit,
    onManageMerchants: () -> Unit,
    onManageChannels: () -> Unit,
    onSave: (TransactionEntity) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var amountError by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedTime by remember { mutableStateOf(Calendar.getInstance()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var direction by remember { mutableStateOf("DEBIT") }
    var selectedCategory by remember { mutableStateOf("Uncategorized") }
    var selectedPaymentMethod by remember { mutableStateOf("UPI") }
    var merchant by remember { mutableStateOf("") }
    var expandedCategory by remember { mutableStateOf(false) }
    var expandedPaymentMethod by remember { mutableStateOf(false) }

    val categoryNames = remember(categories) {
        categories.map { it.name }.ifEmpty {
            listOf("Food", "Groceries", "Transport", "Shopping", "Utilities", "Uncategorized")
        }
    }

    val paymentMethods = remember(paymentChannels) {
        paymentChannels.map { it.code }.ifEmpty { listOf("UPI", "CARD", "NETBANKING", "CASH", "WALLET", "OTHER") }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Add Transaction", fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TextButton(onClick = onManageCategories, contentPadding = PaddingValues(horizontal = 8.dp)) {
                        Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Categories", style = MaterialTheme.typography.labelSmall)
                    }
                    TextButton(onClick = onManageMerchants, contentPadding = PaddingValues(horizontal = 8.dp)) {
                        Icon(Icons.Default.Store, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Merchants", style = MaterialTheme.typography.labelSmall)
                    }
                    TextButton(onClick = onManageChannels, contentPadding = PaddingValues(horizontal = 8.dp)) {
                        Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Channels", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = direction == "DEBIT",
                        onClick = { direction = "DEBIT" },
                        label = { Text("Expense") },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    )
                    FilterChip(
                        selected = direction == "CREDIT",
                        onClick = { direction = "CREDIT" },
                        label = { Text("Income") },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFE8F5E9),
                            selectedLabelColor = Color(0xFF2E7D32)
                        )
                    )
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount = it.filter { c -> c.isDigit() || c == '.' }
                        amountError = false
                    },
                    label = { Text("Amount *") },
                    prefix = { Text("₹ ") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = amountError,
                    supportingText = if (amountError) {
                        { Text("Amount is required", color = MaterialTheme.colorScheme.error) }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("Description / Merchant") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                ExposedDropdownMenuBox(
                    expanded = expandedCategory,
                    onExpandedChange = { expandedCategory = it }
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedCategory,
                        onDismissRequest = { expandedCategory = false }
                    ) {
                        categoryNames.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    selectedCategory = cat
                                    expandedCategory = false
                                }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = expandedPaymentMethod,
                    onExpandedChange = { expandedPaymentMethod = it }
                ) {
                    OutlinedTextField(
                        value = selectedPaymentMethod,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Payment Method") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPaymentMethod) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedPaymentMethod,
                        onDismissRequest = { expandedPaymentMethod = false }
                    ) {
                        paymentMethods.forEach { method ->
                            DropdownMenuItem(
                                text = { Text(method) },
                                onClick = {
                                    selectedPaymentMethod = method
                                    expandedPaymentMethod = false
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(selectedDate.time),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Date") },
                        trailingIcon = {
                            Icon(Icons.Default.CalendarMonth, contentDescription = "Select Date")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showDatePicker = true },
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = SimpleDateFormat("h:mm a", Locale.getDefault()).format(selectedTime.time),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Time") },
                        trailingIcon = {
                            Icon(Icons.Default.Schedule, contentDescription = "Select Time")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showTimePicker = true },
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (amount.isBlank()) {
                        amountError = true
                        return@Button
                    }
                    val amountValue = amount.toDoubleOrNull()
                    if (amountValue == null || amountValue <= 0) {
                        amountError = true
                        return@Button
                    }
                    val combinedTs = Calendar.getInstance().apply {
                        set(Calendar.YEAR, selectedDate.get(Calendar.YEAR))
                        set(Calendar.MONTH, selectedDate.get(Calendar.MONTH))
                        set(Calendar.DAY_OF_MONTH, selectedDate.get(Calendar.DAY_OF_MONTH))
                        set(Calendar.HOUR_OF_DAY, selectedTime.get(Calendar.HOUR_OF_DAY))
                        set(Calendar.MINUTE, selectedTime.get(Calendar.MINUTE))
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis

                    onSave(
                        TransactionEntity(
                            ts = combinedTs,
                            source = "MANUAL",
                            channel = selectedPaymentMethod,
                            direction = direction,
                            merchant = merchant.takeIf { it.isNotBlank() },
                            amount = amountValue,
                            currency = "INR",
                            accountHint = null,
                            rawText = "Manual entry: ${if (direction == "DEBIT") "Spent" else "Received"} ₹${amountValue} via $selectedPaymentMethod",
                            category = selectedCategory,
                            confidence = 100
                        )
                    )
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.timeInMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        selectedDate.timeInMillis = it
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedTime.get(Calendar.HOUR_OF_DAY),
            initialMinute = selectedTime.get(Calendar.MINUTE)
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select Time") },
            text = {
                TimePicker(state = timePickerState)
            },
            confirmButton = {
                TextButton(onClick = {
                    selectedTime.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                    selectedTime.set(Calendar.MINUTE, timePickerState.minute)
                    showTimePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}