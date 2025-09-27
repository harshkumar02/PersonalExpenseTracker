package com.hktech.personalexpensetracker.ui.charts

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import androidx.compose.ui.viewinterop.AndroidView
import com.hktech.personalexpensetracker.data.TransactionEntity
import java.text.SimpleDateFormat
import java.util.*

/** Pie chart by category */
@Composable
fun ExpensePieChart(data: Map<String, Float>) {
    AndroidView(
        factory = { context ->
            PieChart(context).apply {
                description.isEnabled = false
                setUsePercentValues(true)
                setDrawEntryLabels(true)
            }
        },
        update = { chart ->
            val entries = data.map { (cat, amt) -> PieEntry(amt, cat) }
            val dataSet = PieDataSet(entries, "Expenses").apply {
                colors = ColorTemplate.MATERIAL_COLORS.toList()
                valueTextSize = 12f
            }
            chart.data = PieData(dataSet)
            chart.invalidate()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    )
}

/** Bar chart by month */
@Composable
fun ExpenseBarChart(data: Map<String, Float>) {
    AndroidView(
        factory = { context ->
            BarChart(context).apply {
                description.isEnabled = false
                axisRight.isEnabled = false
            }
        },
        update = { chart ->
            val entries = data.entries.mapIndexed { i, (month, amt) ->
                BarEntry(i.toFloat(), amt)
            }
            val dataSet = BarDataSet(entries, "Monthly Totals").apply {
                colors = ColorTemplate.COLORFUL_COLORS.toList()
                valueTextSize = 12f
            }
            chart.data = BarData(dataSet)

            val xAxis = chart.xAxis
            xAxis.valueFormatter = IndexAxisValueFormatter(data.keys.toList())
            xAxis.granularity = 1f
            xAxis.setDrawGridLines(false)
            xAxis.position = XAxis.XAxisPosition.BOTTOM

            chart.invalidate()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    )
}

/** Summary section combining both charts */
@Composable
fun SummarySection(txns: List<TransactionEntity>) {
    // group by category
    val byCategory = txns.groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amount }.toFloat() }

    // group by month (format: "MM/yyyy")
    val byMonth = txns.groupBy {
        val d = Date(it.ts)
        SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(d)
    }.mapValues { entry -> entry.value.sumOf { it.amount }.toFloat() }

    Column {
        Text("Summary", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))

        ExpensePieChart(byCategory)
        Spacer(Modifier.height(16.dp))
        ExpenseBarChart(byMonth)
    }
}
