package com.hktech.personalexpensetracker.ui.transactions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hktech.personalexpensetracker.data.AccountEntity
import com.hktech.personalexpensetracker.data.CategoryEntity
import com.hktech.personalexpensetracker.data.TransactionEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionScreen(
    txns: List<TransactionEntity>,
    allCategories: List<CategoryEntity>,
    accounts: List<AccountEntity>,
    onChangeCategory: (Long, String) -> Unit,
    onDeleteTransaction: (Long) -> Unit,
    onUpdateTransaction: (Long, Double, String, String?, String?) -> Unit
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

    val filteredTxns = remember(txns, searchQuery, selectedCategory, filterDirection, filterChannel,
        filterMinAmount, filterMaxAmount, filterStartDate, filterEndDate, selectedAccount) {
        val minAmt = filterMinAmount
        val maxAmt = filterMaxAmount
        val startDt = filterStartDate
        val endDt = filterEndDate
        txns.filter { txn ->
            val matchesSearch = searchQuery.isBlank() ||
                    txn.merchant?.contains(searchQuery, ignoreCase = true) == true ||
                    txn.rawText.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory == null || txn.category == selectedCategory
            val matchesDirection = filterDirection == null || txn.direction == filterDirection
            val matchesChannel = filterChannel == null || txn.channel == filterChannel
            val matchesMinAmount = minAmt == null || txn.amount >= minAmt
            val matchesMaxAmount = maxAmt == null || txn.amount <= maxAmt
            val matchesStartDate = startDt == null || txn.ts >= startDt
            val matchesEndDate = endDt == null || txn.ts <= endDt
            val matchesAccount = selectedAccount == null ||
                    txn.accountId == selectedAccount ||
                    (txn.accountHint != null && accounts.find { it.id == selectedAccount }?.cardSuffix == txn.accountHint)

            matchesSearch && matchesCategory && matchesDirection && matchesChannel &&
                    matchesMinAmount && matchesMaxAmount && matchesStartDate && matchesEndDate && matchesAccount
        }
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
            EmptyTransactionState(searchQuery.isNotEmpty() || selectedCategory != null || hasActiveFilters)
        } else {
            TransactionList(
                txns = filteredTxns,
                categories = allCategories,
                accounts = accounts,
                onChangeCategory = onChangeCategory,
                onDeleteTransaction = onDeleteTransaction,
                onUpdateTransaction = onUpdateTransaction,
                modifier = Modifier.weight(1f)
            )
        }
    }

    if (showFilterDialog) {
        FilterDialogContent(
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
private fun EmptyTransactionState(isFiltered: Boolean) {
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