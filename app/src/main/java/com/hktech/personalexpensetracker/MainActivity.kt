package com.hktech.personalexpensetracker

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hktech.personalexpensetracker.data.TransactionEntity
import com.hktech.personalexpensetracker.ui.MainViewModel
import com.hktech.personalexpensetracker.ui.theme.PersonalexpensetrackerTheme
import com.hktech.personalexpensetracker.ui.charts.SummarySection
import com.hktech.personalexpensetracker.ui.navigation.AppNav


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

        AppNav(
            txns = txns,
            onChangeCategory = { id: Long, cat: String -> vm.updateCategory(id, cat) }
        )
    }
}


@Composable
fun PermissionsGate(content: @Composable () -> Unit) {
    var granted by remember { mutableStateOf(false) }

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

    if (granted) content() else Box(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Grant SMS permissions to start capturing transactions.")
    }
}

@Composable
fun Screen(
    txns: List<TransactionEntity>,
    onChangeCategory: (Long, String) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Transactions", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        LazyColumn(Modifier.weight(1f)) {
            items(txns) { t: TransactionEntity ->
                Card(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("${t.merchant ?: "Merchant"} • ${t.channel ?: "OTHER"} • ${t.direction}")
                        Text("₹${"%,.2f".format(t.amount)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(t.rawText, style = MaterialTheme.typography.bodySmall)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Category: ${t.category}")
                            CategoryDropdown(current = t.category) { picked ->
                                onChangeCategory(t.id, picked)
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        SummarySection(txns)
    }
}

@Composable
fun CategoryDropdown(current: String, onPick: (String) -> Unit) {
    val options = listOf("Food","Groceries","Transport","Shopping","Utilities","Fuel","Rent","Education","Transfers","Income","Wallet","UPI","Uncategorized")
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) { Text(current) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { o ->
                DropdownMenuItem(
                    text = { Text(o) },
                    onClick = { expanded = false; onPick(o) }
                )
            }
        }
    }
}
