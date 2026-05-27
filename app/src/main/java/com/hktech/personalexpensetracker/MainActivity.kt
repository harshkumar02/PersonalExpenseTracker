package com.hktech.personalexpensetracker

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hktech.personalexpensetracker.data.CategoryEntity
import com.hktech.personalexpensetracker.data.MerchantEntity
import com.hktech.personalexpensetracker.data.TransactionEntity
import com.hktech.personalexpensetracker.ui.MainViewModel
import com.hktech.personalexpensetracker.ui.theme.PersonalexpensetrackerTheme
import com.hktech.personalexpensetracker.ui.navigation.AppNav

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Defer expensive composable initialization
        setContent {
            PersonalexpensetrackerTheme {
                AppContent()
            }
        }
    }
}

@Composable
private fun AppContent() {
    var hasPermissions by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Check permissions synchronously on first composition
    LaunchedEffect(Unit) {
        hasPermissions = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECEIVE_SMS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    if (!hasPermissions) {
        // Request permissions
        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            hasPermissions = permissions[Manifest.permission.RECEIVE_SMS] == true &&
                    permissions[Manifest.permission.READ_SMS] == true
        }

        LaunchedEffect(Unit) {
            launcher.launch(arrayOf(
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS
            ))
        }

        PermissionDeniedScreen(
            onOpenSettings = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
            }
        )
    } else {
        // Deferred content - loads after permissions confirmed
        DeferredMainContent()
    }
}

@Composable
private fun DeferredMainContent() {
    // ViewModel created here - only when actually needed
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
        onUpdateTransaction = { id, amount, direction, merchant, channel ->
            vm.updateTransaction(id, amount, direction, merchant, channel)
        },
        onAddTransaction = { txn -> vm.addTransaction(txn) },
        onAddCategory = { cat -> vm.addCategory(cat) },
        onUpdateCategory = { cat -> vm.updateCategory(cat) },
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

@Composable
private fun App() {
    AppContent()
}

@Composable
fun PermissionsGate(content: @Composable () -> Unit) {
    content()
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