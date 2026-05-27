package com.hktech.personalexpensetracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hktech.personalexpensetracker.data.AccountEntity
import java.text.NumberFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountManagementContent(
    accounts: List<AccountEntity>,
    onAddAccount: (AccountEntity) -> Unit,
    onUpdateAccount: (AccountEntity) -> Unit,
    onDeleteAccount: (Long) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<AccountEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header with Add button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Manage Accounts",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            FilledTonalButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Add")
            }
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        Text(
            "Your Accounts (${accounts.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )

        Spacer(Modifier.height(12.dp))

        if (accounts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No accounts added yet.\nTap 'Add' to add your first account.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(accounts.sortedBy { it.bankName }) { account ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = account.nickname.ifBlank { account.bankName },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "****${account.cardSuffix} • ${account.accountType}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                account.currentBalance?.let { balance ->
                                    Text(
                                        text = "Balance: ₹${"%,.2f".format(balance)}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Row {
                                IconButton(onClick = { editingAccount = account }) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (account.bankName != "Unknown") {
                                    IconButton(onClick = { onDeleteAccount(account.id) }) {
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
            }
        }
    }

    if (showAddDialog) {
        AddEditAccountDialog(
            account = null,
            onSave = { account ->
                onAddAccount(account)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    editingAccount?.let { account ->
        AddEditAccountDialog(
            account = account,
            onSave = { updated ->
                onUpdateAccount(updated)
                editingAccount = null
            },
            onDismiss = { editingAccount = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountManagementDialog(
    accounts: List<AccountEntity>,
    onAddAccount: (AccountEntity) -> Unit,
    onUpdateAccount: (AccountEntity) -> Unit,
    onDeleteAccount: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<AccountEntity?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Manage Accounts", fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                // Add new account button
                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add New Account")
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))

                Text(
                    "Your Accounts (${accounts.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (accounts.isEmpty()) {
                    Text(
                        "No accounts added yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(accounts.sortedBy { it.bankName }) { account ->
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
                                        text = account.nickname.ifBlank { account.bankName },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "****${account.cardSuffix} • ${account.accountType}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    account.currentBalance?.let { balance ->
                                        Text(
                                            text = "Balance: ₹${"%,.2f".format(balance)}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Row {
                                    IconButton(
                                        onClick = { editingAccount = account },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Edit",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    if (account.bankName != "Unknown") {
                                        IconButton(
                                            onClick = { onDeleteAccount(account.id) },
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
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )

    if (showAddDialog) {
        AddEditAccountDialog(
            account = null,
            onSave = { account ->
                onAddAccount(account)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    editingAccount?.let { account ->
        AddEditAccountDialog(
            account = account,
            onSave = { updated ->
                onUpdateAccount(updated)
                editingAccount = null
            },
            onDismiss = { editingAccount = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditAccountDialog(
    account: AccountEntity?,
    onSave: (AccountEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var bankName by remember { mutableStateOf(account?.bankName ?: "") }
    var cardSuffix by remember { mutableStateOf(account?.cardSuffix ?: "") }
    var nickname by remember { mutableStateOf(account?.nickname ?: "") }
    var selectedType by remember { mutableStateOf(account?.accountType ?: "CARD") }
    var expandedType by remember { mutableStateOf(false) }

    val accountTypes = listOf("CARD", "ACCOUNT", "UPI", "WALLET")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (account == null) "Add Account" else "Edit Account", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = bankName,
                    onValueChange = { bankName = it },
                    label = { Text("Bank Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    placeholder = { Text("e.g., ICICI Bank") }
                )

                OutlinedTextField(
                    value = cardSuffix,
                    onValueChange = { cardSuffix = it.filter { c -> c.isDigit() }.take(4) },
                    label = { Text("Last 4 Digits *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    placeholder = { Text("e.g., 2007") },
                    supportingText = { Text("Last 4 digits of card/account") }
                )

                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text("Nickname (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    placeholder = { Text("e.g., ICICI Credit") }
                )

                ExposedDropdownMenuBox(
                    expanded = expandedType,
                    onExpandedChange = { expandedType = it }
                ) {
                    OutlinedTextField(
                        value = selectedType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Account Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedType) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedType,
                        onDismissRequest = { expandedType = false }
                    ) {
                        accountTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    selectedType = type
                                    expandedType = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (bankName.isNotBlank() && cardSuffix.length == 4) {
                        onSave(
                            AccountEntity(
                                id = account?.id ?: 0L,
                                bankName = bankName.trim(),
                                cardSuffix = cardSuffix,
                                nickname = nickname.trim(),
                                accountType = selectedType,
                                isActive = true
                            )
                        )
                    }
                },
                enabled = bankName.isNotBlank() && cardSuffix.length == 4
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
}
