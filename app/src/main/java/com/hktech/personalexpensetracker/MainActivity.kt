package com.hktech.personalexpensetracker

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hktech.personalexpensetracker.data.CategoryEntity
import com.hktech.personalexpensetracker.data.MerchantEntity
import com.hktech.personalexpensetracker.data.TransactionEntity
import com.hktech.personalexpensetracker.ui.MainViewModel
import com.hktech.personalexpensetracker.ui.theme.*
import com.hktech.personalexpensetracker.ui.navigation.AppNav
import com.hktech.personalexpensetracker.ui.theme.safeCategoryColor
import java.text.SimpleDateFormat
import java.util.*
import java.text.NumberFormat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PersonalexpensetrackerTheme {
                App()
            }
        }
    }
}

@Composable
private fun App() {
    PermissionsGate {
        val vm: MainViewModel = viewModel()
        val txns by vm.txns.collectAsState(initial = emptyList<TransactionEntity>())
        val categories by vm.categories.collectAsState(initial = emptyList<CategoryEntity>())
        val merchants by vm.merchants.collectAsState(initial = emptyList<MerchantEntity>())
        val accounts by vm.accounts.collectAsState(initial = emptyList())
        val paymentChannels by vm.paymentChannels.collectAsState(initial = emptyList())

        AppNav(
            txns = txns,
            categories = categories,
            merchants = merchants,
            accounts = accounts,
            paymentChannels = paymentChannels,
            onChangeCategory = { id: Long, cat: String -> vm.updateCategory(id, cat) },
            onDeleteTransaction = { id: Long -> vm.deleteTransaction(id) },
            onAddTransaction = { txn -> vm.addTransaction(txn) },
            onAddCategory = { cat -> vm.addCategory(cat) },
            onDeleteCategory = { name -> vm.deleteCategory(name) },
            onAddMerchant = { m -> vm.addMerchant(m) },
            onDeleteMerchant = { name -> vm.deleteMerchant(name) },
            onAddAccount = { a -> vm.addAccount(a) },
            onUpdateAccount = { a -> vm.updateAccount(a) },
            onDeleteAccount = { id -> vm.deleteAccount(id) },
            onAddPaymentChannel = { c -> vm.addPaymentChannel(c) },
            onDeletePaymentChannel = { code -> vm.deletePaymentChannel(code) }
        )
    }
}

@Composable
fun PermissionsGate(content: @Composable () -> Unit) {
    var granted by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions: Map<String, Boolean> ->
        granted = listOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS
        ).all { permissions[it] == true }
    }

    LaunchedEffect(Unit) {
        launcher.launch(
            arrayOf(
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS
            )
        )
    }

    if (granted) {
        content()
    } else {
        PermissionDeniedScreen(
            onOpenSettings = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
            }
        )
    }
}

@Composable
private fun PermissionDeniedScreen(onOpenSettings: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = "SMS Permission Required",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "To automatically capture your bank transactions, please grant SMS permission.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onOpenSettings,
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Text("Open Settings")
        }
    }
}

@Composable
fun Screen(
    txns: List<TransactionEntity>,
    allCategories: List<CategoryEntity>,
    accounts: List<com.hktech.personalexpensetracker.data.AccountEntity>,
    onChangeCategory: (Long, String) -> Unit,
    onDeleteTransaction: (Long) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var selectedAccount by remember { mutableStateOf<Long?>(null) }

    // Advanced filter states
    var filterDirection by remember { mutableStateOf<String?>(null) }
    var filterChannel by remember { mutableStateOf<String?>(null) }
    var filterMinAmount by remember { mutableStateOf<Double?>(null) }
    var filterMaxAmount by remember { mutableStateOf<Double?>(null) }
    var filterStartDate by remember { mutableStateOf<Long?>(null) }
    var filterEndDate by remember { mutableStateOf<Long?>(null) }

    val hasActiveFilters = filterDirection != null || filterChannel != null ||
            filterMinAmount != null || filterMaxAmount != null ||
            filterStartDate != null || filterEndDate != null || selectedAccount != null

    val filteredTxns = txns.filter { txn ->
        val search = searchQuery
        val cat = selectedCategory
        val dir = filterDirection
        val chan = filterChannel
        val minAmt = filterMinAmount
        val maxAmt = filterMaxAmount
        val start = filterStartDate
        val end = filterEndDate
        val acct = selectedAccount

        val matchesSearch = search.isBlank() ||
                txn.merchant?.contains(search, ignoreCase = true) == true ||
                txn.rawText.contains(search, ignoreCase = true)
        val matchesCategory = cat == null || txn.category == cat
        val matchesDirection = dir == null || txn.direction == dir
        val matchesChannel = chan == null || txn.channel == chan
        val matchesMinAmount = minAmt == null || txn.amount >= minAmt
        val matchesMaxAmount = maxAmt == null || txn.amount <= maxAmt
        val matchesStartDate = start == null || txn.ts >= start
        val matchesEndDate = end == null || txn.ts <= end
        val matchesAccount = acct == null ||
            txn.accountId == acct ||
            (txn.accountHint != null && accounts.find { it.id == acct }?.cardSuffix == txn.accountHint)

        matchesSearch && matchesCategory && matchesDirection && matchesChannel &&
                matchesMinAmount && matchesMaxAmount && matchesStartDate && matchesEndDate && matchesAccount
    }

    val filterCategories = remember(txns) {
        listOf("All") + txns.map { it.category }.distinct().sorted()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar with filter button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search transactions...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.width(8.dp))
            Badge(
                containerColor = if (hasActiveFilters) MaterialTheme.colorScheme.error else Color.Transparent
            ) {
                IconButton(onClick = { showFilterDialog = true }) {
                    Icon(
                        Icons.Default.FilterList,
                        contentDescription = "Filters",
                        tint = if (hasActiveFilters) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Account filter
        if (accounts.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedAccount == null,
                        onClick = { selectedAccount = null },
                        label = { Text("All Accounts") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                }
                items(accounts.filter { it.bankName != "Unknown" }) { account ->
                    FilterChip(
                        selected = selectedAccount == account.id,
                        onClick = { selectedAccount = account.id },
                        label = { Text(account.nickname.ifBlank { "${account.bankName} ****${account.cardSuffix}" }) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                }
            }
        }

        // Category filter chips
        LazyRow(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filterCategories) { category ->
                val isSelected = (category == "All" && selectedCategory == null) ||
                        category == selectedCategory
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        selectedCategory = if (category == "All") null else category
                    },
                    label = { Text(category) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        // Transaction list
        if (filteredTxns.isEmpty()) {
            EmptyState(searchQuery.isNotEmpty() || selectedCategory != null || hasActiveFilters)
        } else {
            TransactionList(
                txns = filteredTxns,
                categories = allCategories,
                accounts = accounts,
                onChangeCategory = onChangeCategory,
                onDeleteTransaction = onDeleteTransaction,
                modifier = Modifier.weight(1f)
            )
        }
    }

    if (showFilterDialog) {
        FilterDialog(
            currentDirection = filterDirection,
            currentChannel = filterChannel,
            currentMinAmount = filterMinAmount,
            currentMaxAmount = filterMaxAmount,
            currentStartDate = filterStartDate,
            currentEndDate = filterEndDate,
            onApply = { dir, chan, minAmt, maxAmt, start, end ->
                filterDirection = dir
                filterChannel = chan
                filterMinAmount = minAmt
                filterMaxAmount = maxAmt
                filterStartDate = start
                filterEndDate = end
                showFilterDialog = false
            },
            onClear = {
                filterDirection = null
                filterChannel = null
                filterMinAmount = null
                filterMaxAmount = null
                filterStartDate = null
                filterEndDate = null
                showFilterDialog = false
            },
            onDismiss = { showFilterDialog = false }
        )
    }
}

@Composable
private fun EmptyState(isFiltered: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isFiltered) "No matching transactions" else "No transactions yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (isFiltered) "Try adjusting your search or filters"
                   else "Your bank SMS will appear here automatically",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TransactionList(
    txns: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    accounts: List<com.hktech.personalexpensetracker.data.AccountEntity>,
    onChangeCategory: (Long, String) -> Unit,
    onDeleteTransaction: (Long) -> Unit,
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
                    onDeleteTransaction = onDeleteTransaction
                )
            }
        }
    }
}

@Composable
private fun TransactionCard(
    transaction: TransactionEntity,
    categories: List<CategoryEntity>,
    accounts: List<com.hktech.personalexpensetracker.data.AccountEntity>,
    onChangeCategory: (Long, String) -> Unit,
    onDeleteTransaction: (Long) -> Unit
) {
    val isCredit = transaction.direction == "CREDIT"
    val backgroundColor = if (isCredit) CreditGreen else DebitRed
    val textColor = if (isCredit) CreditGreenText else DebitRedText
    val categoryColor = safeCategoryColor(transaction.category)

    // Get account name for display
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
            onDismiss = { showDetails = false }
        )
    }
}

@Composable
private fun CategorySelectionDialog(
    categories: List<CategoryEntity>,
    currentCategory: String,
    onCategorySelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val categoryNames = remember(categories) { categories.map { it.name }.ifEmpty { listOf("Food", "Groceries", "Transport", "Shopping", "Utilities", "Uncategorized") } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Category") },
        text = {
            Column {
                categoryNames.forEach { category ->
                    val isSelected = category == currentCategory
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCategorySelected(category) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(
                                    safeCategoryColor(category),
                                    RoundedCornerShape(6.dp)
                                )
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = category,
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

// Filter Dialog
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterDialog(
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

// Transaction Details Bottom Sheet
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionDetailsSheet(
    transaction: TransactionEntity,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val isCredit = transaction.direction == "CREDIT"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transaction.merchant ?: "Unknown Merchant",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isCredit) "Credit" else "Debit",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isCredit) CreditGreenText else DebitRedText
                    )
                }
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = if (isCredit) CreditGreenText else DebitRedText)) {
                            append(if (isCredit) "+₹" else "-₹")
                        }
                        append(formatAmount(transaction.amount))
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            // Details grid
            DetailRow("Category", transaction.category)
            DetailRow("Payment Method", transaction.channel ?: "OTHER")
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
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
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

private fun formatAmount(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale("en", "IN"))
    formatter.minimumFractionDigits = 2
    formatter.maximumFractionDigits = 2
    return formatter.format(amount)
}