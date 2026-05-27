package com.hktech.personalexpensetracker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hktech.personalexpensetracker.data.TransactionEntity
import java.text.SimpleDateFormat
import java.util.*

data class QuickAddTemplate(
    val id: String,
    val name: String,
    val icon: String,
    val merchant: String,
    val channel: String,
    val category: String,
    val color: Long
)

@Composable
fun QuickAddDialog(
    onDismiss: () -> Unit,
    onSave: (TransactionEntity) -> Unit
) {
    val templates = remember {
        listOf(
            QuickAddTemplate("rapido", "Rapido", "🚗", "Rapido", "WALLET", "Transport", 0xFF4285F4),
            QuickAddTemplate("uber", "Uber", "🚕", "Uber", "WALLET", "Transport", 0xFF000000),
            QuickAddTemplate("amazon", "Amazon Pay", "📦", "Amazon Pay", "WALLET", "Transfers", 0xFFFF9000),
            QuickAddTemplate("flipkart", "Flipkart", "🛒", "Flipkart", "WALLET", "Shopping", 0xFF2874F0),
            QuickAddTemplate("swiggy", "Swiggy", "🍔", "Swiggy", "WALLET", "Food", 0xFFFF5722),
            QuickAddTemplate("zomato", "Zomato", "🍕", "Zomato", "WALLET", "Food", 0xFFE23765),
            QuickAddTemplate("googlepay", "Google Pay", "💳", "Google Pay", "UPI", "Transfers", 0xFF4285F4),
            QuickAddTemplate("phonepe", "PhonePe", "💳", "PhonePe", "UPI", "Transfers", 0xFF5D4E91),
            QuickAddTemplate("paytm", "Paytm", "💳", "Paytm", "WALLET", "Transfers", 0xFF002E6E),
            QuickAddTemplate("custom", "Custom", "➕", "", "UPI", "Uncategorized", 0xFF6200EE)
        )
    }

    var selectedTemplate by remember { mutableStateOf<QuickAddTemplate?>(null) }
    var showAmountDialog by remember { mutableStateOf(false) }
    var amount by remember { mutableStateOf("") }
    var amountError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Quick Add", fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column {
                Text(
                    text = "Select a service to quickly add a transaction:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(templates) { template ->
                        QuickAddTemplateItem(
                            template = template,
                            onClick = {
                                if (template.id == "custom") {
                                    // For custom, show amount dialog first with empty merchant
                                    selectedTemplate = template
                                    showAmountDialog = true
                                } else {
                                    selectedTemplate = template
                                    showAmountDialog = true
                                }
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )

    // Amount input dialog
    if (showAmountDialog && selectedTemplate != null) {
        QuickAddAmountDialog(
            template = selectedTemplate!!,
            onDismiss = {
                showAmountDialog = false
                selectedTemplate = null
                amount = ""
                amountError = false
            },
            onSave = { amountValue, merchantOverride, channelOverride ->
                val template = selectedTemplate!!
                val txn = TransactionEntity(
                    ts = System.currentTimeMillis(),
                    source = "QUICK_ADD",
                    channel = channelOverride,
                    direction = "DEBIT",
                    merchant = merchantOverride,
                    amount = amountValue,
                    currency = "INR",
                    accountHint = null,
                    rawText = "Quick add: ${template.name} - ₹$amountValue",
                    category = template.category,
                    confidence = 100
                )
                onSave(txn)
                onDismiss()
            }
        )
    }
}

@Composable
private fun QuickAddTemplateItem(
    template: QuickAddTemplate,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = template.icon,
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = template.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${template.channel} • ${template.category}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                Icons.Default.Add,
                contentDescription = "Add",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickAddAmountDialog(
    template: QuickAddTemplate,
    onDismiss: () -> Unit,
    onSave: (Double, String, String) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var merchant by remember { mutableStateOf(template.merchant) }
    var channel by remember { mutableStateOf(template.channel) }
    var expandedChannel by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }

    val channels = listOf("UPI", "WALLET", "CARD", "NETBANKING", "CASH", "OTHER")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Add ${template.name} Transaction", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
                        { Text("Valid amount required") }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(8.dp)
                )

                if (template.id == "custom") {
                    OutlinedTextField(
                        value = merchant,
                        onValueChange = { merchant = it },
                        label = { Text("Merchant/Description") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    ExposedDropdownMenuBox(
                        expanded = expandedChannel,
                        onExpandedChange = { expandedChannel = it }
                    ) {
                        OutlinedTextField(
                            value = channel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Payment Method") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedChannel) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedChannel,
                            onDismissRequest = { expandedChannel = false }
                        ) {
                            channels.forEach { ch ->
                                DropdownMenuItem(
                                    text = { Text(ch) },
                                    onClick = {
                                        channel = ch
                                        expandedChannel = false
                                    }
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Date: ${SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.getDefault()).format(Date())}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountValue = amount.toDoubleOrNull()
                    if (amountValue == null || amountValue <= 0) {
                        amountError = true
                    } else {
                        onSave(amountValue, merchant, channel)
                    }
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
}