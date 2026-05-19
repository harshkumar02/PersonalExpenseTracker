package com.hktech.personalexpensetracker.ui.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.hktech.personalexpensetracker.data.TransactionEntity
import com.hktech.personalexpensetracker.ui.theme.safeCategoryColor
import java.text.SimpleDateFormat
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.*

enum class TimePeriod(val label: String) {
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    THIS_YEAR("This Year"),
    CUSTOM("Custom")
}

@Composable
fun SummarySection(txns: List<TransactionEntity>) {
    var selectedPeriod by remember { mutableStateOf(TimePeriod.THIS_MONTH) }
    var showPeriodPicker by remember { mutableStateOf(false) }
    var customStartDate by remember { mutableStateOf<Long?>(null) }
    var customEndDate by remember { mutableStateOf<Long?>(null) }
    var showDateRangePicker by remember { mutableStateOf(false) }

    // Filter transactions based on selected period
    val filteredTxns = remember(txns, selectedPeriod, customStartDate, customEndDate) {
        filterTransactionsByPeriod(txns, selectedPeriod, customStartDate, customEndDate)
    }

    // Calculate period stats for header
    val periodLabel = remember(selectedPeriod, customStartDate, customEndDate) {
        getPeriodLabel(selectedPeriod, customStartDate, customEndDate)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Period Selector Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = periodLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "${filteredTxns.size} transactions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }

                ExposedDropdownMenuBox(
                    expanded = showPeriodPicker,
                    onExpandedChange = { showPeriodPicker = it }
                ) {
                    Surface(
                        modifier = Modifier
                            .menuAnchor()
                            .clickable { showPeriodPicker = true },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedPeriod.label,
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.labelLarge
                            )
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    ExposedDropdownMenu(
                        expanded = showPeriodPicker,
                        onDismissRequest = { showPeriodPicker = false }
                    ) {
                        TimePeriod.entries.filter { it != TimePeriod.CUSTOM }.forEach { period ->
                            DropdownMenuItem(
                                text = { Text(period.label) },
                                onClick = {
                                    selectedPeriod = period
                                    showPeriodPicker = false
                                }
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Custom Range") },
                            leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                            onClick = {
                                showPeriodPicker = false
                                showDateRangePicker = true
                            }
                        )
                    }
                }
            }
        }

        // Content
        if (filteredTxns.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No transactions",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "No spending data for the selected period",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            SummaryContent(txns = filteredTxns)
        }
    }

    // Custom Date Range Picker
    if (showDateRangePicker) {
        CustomDateRangeDialog(
            currentStart = customStartDate,
            currentEnd = customEndDate,
            onApply = { start, end ->
                customStartDate = start
                customEndDate = end
                selectedPeriod = TimePeriod.CUSTOM
                showDateRangePicker = false
            },
            onClear = {
                customStartDate = null
                customEndDate = null
                showDateRangePicker = false
            },
            onDismiss = { showDateRangePicker = false }
        )
    }
}

@Composable
private fun SummaryContent(txns: List<TransactionEntity>) {
    val totalExpenses = txns.filter { it.direction == "DEBIT" }.sumOf { it.amount }
    val totalIncome = txns.filter { it.direction == "CREDIT" }.sumOf { it.amount }
    val netSavings = totalIncome - totalExpenses
    val txnCount = txns.size
    val avgTransaction = if (txnCount > 0) totalExpenses / txnCount else 0.0

    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        // Summary cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryCard(
                title = "Expenses",
                amount = totalExpenses,
                color = Color(0xFFE53935),
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                title = "Income",
                amount = totalIncome,
                color = Color(0xFF43A047),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryCard(
                title = "Net Savings",
                amount = netSavings,
                color = if (netSavings >= 0) Color(0xFF1E88E5) else Color(0xFFE53935),
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                title = "Avg. Txn",
                amount = avgTransaction,
                color = Color(0xFF7B1FA2),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(24.dp))

        // Category breakdown
        val byCategory = txns.filter { it.direction == "DEBIT" }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        if (byCategory.isNotEmpty()) {
            Text(
                text = "Spending by Category",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))

            // Category list with percentages
            byCategory.entries.sortedByDescending { it.value }.forEach { (category, amount) ->
                val percentage = if (totalExpenses > 0) (amount / totalExpenses * 100) else 0.0
                CategoryRow(
                    category = category,
                    amount = amount,
                    percentage = percentage.toFloat(),
                    totalAmount = totalExpenses.toFloat()
                )
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(12.dp))
            ExpensePieChart(byCategory.mapValues { it.value.toFloat() })
        }

        Spacer(Modifier.height(24.dp))

        // Trend chart
        Text(
            text = "Daily Trend",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(12.dp))

        val byDay = txns.filter { it.direction == "DEBIT" }
            .groupBy { txn ->
                val date = Instant.ofEpochMilli(txn.ts).atZone(ZoneId.systemDefault()).toLocalDate()
                date
            }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        if (byDay.isNotEmpty()) {
            DailyBarChart(byDay.mapValues { it.value.toFloat() })
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun CategoryRow(
    category: String,
    amount: Double,
    percentage: Float,
    totalAmount: Float
) {
    val categoryColor = safeCategoryColor(category)

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(categoryColor, RoundedCornerShape(4.dp))
                )
                Spacer(Modifier.width(8.dp))
                Text(text = category, style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                text = "₹${"%,.0f".format(amount)} (${"%.0f".format(percentage)}%)",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { percentage / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = categoryColor,
            trackColor = categoryColor.copy(alpha = 0.2f)
        )
    }
}

@Composable
private fun DailyBarChart(data: Map<LocalDate, Float>) {
    if (data.isEmpty()) {
        EmptyChartPlaceholder("No daily data")
        return
    }

    val sortedData = data.entries.sortedBy { it.key }.takeLast(14)
    val dateFormatter = DateTimeFormatter.ofPattern("dd MMM")

    AndroidView(
        factory = { context ->
            BarChart(context).apply {
                description.isEnabled = false
                axisRight.isEnabled = false
                axisLeft.textSize = 10f
                setFitBars(true)
                animateY(500)
                legend.isEnabled = false
            }
        },
        update = { chart ->
            val entries = sortedData.mapIndexed { i, (_, amt) ->
                BarEntry(i.toFloat(), amt)
            }
            val dataSet = BarDataSet(entries, "Daily Spending").apply {
                colors = listOf(0xFFFF7043.toInt())
                valueTextSize = 9f
            }
            chart.data = BarData(dataSet)

            val xAxis = chart.xAxis
            xAxis.valueFormatter = IndexAxisValueFormatter(sortedData.map { it.key.format(dateFormatter) })
            xAxis.granularity = 1f
            xAxis.setDrawGridLines(false)
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.textSize = 8f
            xAxis.labelRotationAngle = -45f

            chart.invalidate()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomDateRangeDialog(
    currentStart: Long?,
    currentEnd: Long?,
    onApply: (Long, Long) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    var startDate by remember { mutableStateOf(currentStart) }
    var endDate by remember { mutableStateOf(currentEnd) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Date Range", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = startDate?.let { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(it)) } ?: "Select start date",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Start Date") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showStartPicker = true },
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = endDate?.let { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(it)) } ?: "Select end date",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("End Date") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showEndPicker = true },
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (startDate != null && endDate != null) {
                        onApply(startDate!!, endDate!!)
                    }
                },
                enabled = startDate != null && endDate != null
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onClear) {
                    Text("Clear")
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )

    if (showStartPicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = startDate ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { startDate = it }
                    showStartPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartPicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showEndPicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = endDate ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { endDate = it }
                    showEndPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showEndPicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun EmptyChartPlaceholder(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ExpensePieChart(data: Map<String, Float>) {
    if (data.isEmpty()) {
        EmptyChartPlaceholder("No expense data")
        return
    }

    AndroidView(
        factory = { context ->
            PieChart(context).apply {
                description.isEnabled = false
                setUsePercentValues(true)
                setDrawEntryLabels(true)
                isDrawHoleEnabled = true
                setHoleColor(android.graphics.Color.TRANSPARENT)
                holeRadius = 45f
                transparentCircleRadius = 50f
                setEntryLabelTextSize(10f)
                setEntryLabelColor(android.graphics.Color.DKGRAY)
                legend.isEnabled = true
                legend.textSize = 12f
            }
        },
        update = { chart ->
            val sortedData = data.entries.sortedByDescending { it.value }.take(6)
            val entries = sortedData.map { (cat, amt) -> PieEntry(amt, cat) }
            val dataSet = PieDataSet(entries, "").apply {
                colors = listOf(
                    0xFFFFB74D.toInt(), 0xFF81C784.toInt(), 0xFF64B5F6.toInt(),
                    0xFFBA68C8.toInt(), 0xFF4DD0E1.toInt(), 0xFFFF8A65.toInt()
                )
                valueTextSize = 11f
                valueTextColor = android.graphics.Color.WHITE
            }
            chart.data = PieData(dataSet)
            chart.invalidate()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
    )
}

@Composable
private fun SummaryCard(
    title: String,
    amount: Double,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = color
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "₹${"%,.0f".format(amount)}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

// Helper functions
private fun filterTransactionsByPeriod(
    txns: List<TransactionEntity>,
    period: TimePeriod,
    customStart: Long?,
    customEnd: Long?
): List<TransactionEntity> {
    val now = LocalDate.now()
    val zoneId = ZoneId.systemDefault()

    val (startDate, endDate) = when (period) {
        TimePeriod.THIS_WEEK -> {
            val startOfWeek = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val endOfWeek = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
            startOfWeek to endOfWeek
        }
        TimePeriod.THIS_MONTH -> {
            val startOfMonth = now.withDayOfMonth(1)
            val endOfMonth = now.with(TemporalAdjusters.lastDayOfMonth())
            startOfMonth to endOfMonth
        }
        TimePeriod.LAST_MONTH -> {
            val lastMonth = now.minusMonths(1)
            val startOfLastMonth = lastMonth.withDayOfMonth(1)
            val endOfLastMonth = lastMonth.with(TemporalAdjusters.lastDayOfMonth())
            startOfLastMonth to endOfLastMonth
        }
        TimePeriod.THIS_YEAR -> {
            val startOfYear = now.withDayOfYear(1)
            val endOfYear = now.with(TemporalAdjusters.lastDayOfYear())
            startOfYear to endOfYear
        }
        TimePeriod.CUSTOM -> {
            if (customStart != null && customEnd != null) {
                Instant.ofEpochMilli(customStart).atZone(zoneId).toLocalDate() to
                    Instant.ofEpochMilli(customEnd).atZone(zoneId).toLocalDate()
            } else {
                return txns
            }
        }
    }

    val startMillis = startDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
    val endMillis = endDate.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

    return txns.filter { it.ts in startMillis until endMillis }
}

private fun getPeriodLabel(period: TimePeriod, customStart: Long?, customEnd: Long?): String {
    return when (period) {
        TimePeriod.THIS_WEEK -> "This Week"
        TimePeriod.THIS_MONTH -> {
            val monthFormat = DateTimeFormatter.ofPattern("MMMM yyyy")
            LocalDate.now().format(monthFormat)
        }
        TimePeriod.LAST_MONTH -> {
            val monthFormat = DateTimeFormatter.ofPattern("MMMM yyyy")
            LocalDate.now().minusMonths(1).format(monthFormat)
        }
        TimePeriod.THIS_YEAR -> {
            val yearFormat = DateTimeFormatter.ofPattern("yyyy")
            LocalDate.now().format(yearFormat)
        }
        TimePeriod.CUSTOM -> {
            if (customStart != null && customEnd != null) {
                val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
                "${dateFormat.format(Date(customStart))} - ${dateFormat.format(Date(customEnd))}"
            } else {
                "Custom Range"
            }
        }
    }
}