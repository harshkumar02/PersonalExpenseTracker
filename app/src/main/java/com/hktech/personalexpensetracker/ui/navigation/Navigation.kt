package com.hktech.personalexpensetracker.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.hktech.personalexpensetracker.Screen
import com.hktech.personalexpensetracker.data.AccountEntity
import com.hktech.personalexpensetracker.data.CategoryEntity
import com.hktech.personalexpensetracker.data.MerchantEntity
import com.hktech.personalexpensetracker.data.PaymentChannelEntity
import com.hktech.personalexpensetracker.data.TransactionEntity
import com.hktech.personalexpensetracker.ui.AccountManagementContent
import com.hktech.personalexpensetracker.ui.AddTransactionFab
import com.hktech.personalexpensetracker.ui.charts.SummarySection

sealed class ScreenNav(val route: String, val label: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    object Transactions : ScreenNav("transactions", "Transactions", Icons.Filled.Home, Icons.Outlined.Home)
    object Summary : ScreenNav("summary", "Summary", Icons.Filled.Assessment, Icons.Outlined.Assessment)
    object Accounts : ScreenNav("accounts", "Accounts", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet)
}

@Composable
fun AppNav(
    txns: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    merchants: List<MerchantEntity>,
    accounts: List<AccountEntity>,
    paymentChannels: List<PaymentChannelEntity>,
    onChangeCategory: (Long, String) -> Unit,
    onDeleteTransaction: (Long) -> Unit,
    onAddTransaction: (TransactionEntity) -> Unit,
    onAddCategory: (CategoryEntity) -> Unit,
    onDeleteCategory: (String) -> Unit,
    onAddMerchant: (MerchantEntity) -> Unit,
    onDeleteMerchant: (String) -> Unit,
    onAddAccount: (AccountEntity) -> Unit,
    onUpdateAccount: (AccountEntity) -> Unit,
    onDeleteAccount: (Long) -> Unit,
    onAddPaymentChannel: (PaymentChannelEntity) -> Unit,
    onDeletePaymentChannel: (String) -> Unit
) {
    val navController = rememberNavController()
    val items = listOf(ScreenNav.Transactions, ScreenNav.Summary, ScreenNav.Accounts)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    Text(
                        text = when (navBackStackEntry?.destination?.route) {
                            ScreenNav.Transactions.route -> "My Transactions"
                            ScreenNav.Summary.route -> "Summary"
                            ScreenNav.Accounts.route -> "Accounts"
                            else -> "Expense Tracker"
                        }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                items.forEach { screen ->
                    val selected = currentRoute == screen.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        label = { Text(screen.label) },
                        icon = {
                            Icon(
                                imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                contentDescription = screen.label
                            )
                        }
                    )
                }
            }
        },
        floatingActionButton = {
            if (navController.currentBackStackEntryAsState().value?.destination?.route == ScreenNav.Transactions.route) {
                AddTransactionFab(
                    onAddTransaction = onAddTransaction,
                    categories = categories,
                    onAddCategory = onAddCategory,
                    onDeleteCategory = onDeleteCategory,
                    merchants = merchants,
                    onAddMerchant = onAddMerchant,
                    onDeleteMerchant = onDeleteMerchant,
                    accounts = accounts,
                    onAddAccount = onAddAccount,
                    onUpdateAccount = onUpdateAccount,
                    onDeleteAccount = onDeleteAccount,
                    paymentChannels = paymentChannels,
                    onAddPaymentChannel = onAddPaymentChannel,
                    onDeletePaymentChannel = onDeletePaymentChannel
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController,
            startDestination = ScreenNav.Transactions.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(ScreenNav.Transactions.route) {
                Screen(
                    txns = txns,
                    allCategories = categories,
                    accounts = accounts,
                    onChangeCategory = onChangeCategory,
                    onDeleteTransaction = onDeleteTransaction
                )
            }
            composable(ScreenNav.Summary.route) {
                SummarySection(txns)
            }
            composable(ScreenNav.Accounts.route) {
                AccountManagementContent(
                    accounts = accounts,
                    onAddAccount = onAddAccount,
                    onUpdateAccount = onUpdateAccount,
                    onDeleteAccount = onDeleteAccount
                )
            }
        }
    }
}