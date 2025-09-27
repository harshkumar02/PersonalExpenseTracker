package com.hktech.personalexpensetracker.ui.navigation

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.hktech.personalexpensetracker.Screen
import com.hktech.personalexpensetracker.ui.charts.SummarySection
import com.hktech.personalexpensetracker.data.TransactionEntity
import androidx.compose.foundation.layout.padding




sealed class ScreenNav(val route: String, val label: String) {
    object Transactions : ScreenNav("transactions", "Transactions")
    object Summary : ScreenNav("summary", "Summary")
}

@Composable
fun AppNav(txns: List<TransactionEntity>, onChangeCategory: (Long, String) -> Unit) {
    val navController = rememberNavController()
    val items = listOf(ScreenNav.Transactions, ScreenNav.Summary)

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                items.forEach { screen ->
                    NavigationBarItem(
                        selected = currentRoute == screen.route,
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
                        icon = {}
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController,
            startDestination = ScreenNav.Transactions.route,
            Modifier.padding(innerPadding)
        ) {
            composable(ScreenNav.Transactions.route) {
                Screen(txns, onChangeCategory)
            }
            composable(ScreenNav.Summary.route) {
                SummarySection(txns)
            }
        }
    }
}
