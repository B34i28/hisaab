package com.example.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.local.entity.Shop
import com.example.ui.components.AddEditShopDialog
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.DeleteShopConfirmDialog
import com.example.ui.components.ShopTopAppBar
import com.example.ui.screens.customers.CustomerDetailScreen
import com.example.ui.screens.customers.CustomersScreen
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.expenses.ExpensesScreen
import com.example.ui.screens.lock.AppLockScreen
import com.example.ui.screens.onboarding.FirstShopScreen
import com.example.ui.screens.reports.ReportsScreen
import com.example.ui.screens.sales.SalesScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.suppliers.SupplierDetailScreen
import com.example.ui.screens.suppliers.SuppliersScreen
import com.example.ui.viewmodel.ShopViewModel

@Composable
fun HisaabApp(viewModel: ShopViewModel) {
    val navController = rememberNavController()
    val allShops by viewModel.allShops.collectAsStateWithLifecycle()
    val currentShop by viewModel.currentShop.collectAsStateWithLifecycle()
    val isInitialLoading by viewModel.isInitialLoading.collectAsStateWithLifecycle()
    val isAppLocked by viewModel.isAppLocked.collectAsStateWithLifecycle()

    if (isAppLocked) {
        AppLockScreen(viewModel = viewModel)
        return
    }

    if (isInitialLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    // FIRST RUN: If no shops exist, show "Add your first shop" screen
    if (allShops.isEmpty()) {
        FirstShopScreen(
            onCreateShop = { name ->
                viewModel.createShop(name)
            },
            onToggleLanguage = { viewModel.toggleLanguage() }
        )
        return
    }


    var showAddShopDialog by remember { mutableStateOf(false) }
    var shopToRename by remember { mutableStateOf<Shop?>(null) }
    var shopToDelete by remember { mutableStateOf<Shop?>(null) }

    var openSalesAddDialog by remember { mutableStateOf(false) }
    var openExpensesAddDialog by remember { mutableStateOf(false) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isTopLevelDestination = Screen.bottomNavItems.any { it.route == currentRoute }

    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val isDark = when (themeMode) {
        com.example.util.ThemeMode.LIGHT -> false
        com.example.util.ThemeMode.DARK -> true
        com.example.util.ThemeMode.SYSTEM -> isSystemDark
    }

    Scaffold(
        contentWindowInsets = WindowInsets.navigationBars,
        topBar = {
            if (isTopLevelDestination) {
                ShopTopAppBar(
                    currentShop = currentShop,
                    allShops = allShops,
                    themeMode = themeMode,
                    isDarkTheme = isDark,
                    onSelectShop = { shopId ->
                        viewModel.selectShop(shopId)
                    },
                    onAddNewShopClick = { showAddShopDialog = true },
                    onRenameShopClick = { shop -> shopToRename = shop },
                    onDeleteShopClick = { shop -> shopToDelete = shop },
                    onReportsClick = { navController.navigate(Screen.Reports.route) },
                    onToggleTheme = { viewModel.toggleTheme() },
                    onToggleLanguage = { viewModel.toggleLanguage() },
                    onSettingsClick = { navController.navigate("settings") }
                )
            }
        },

        bottomBar = {
            if (isTopLevelDestination) {
                val strings = com.example.util.LocalAppStrings.current
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    Screen.bottomNavItems.forEach { screen ->
                        val selected = currentRoute == screen.route
                        val title = when (screen) {
                            Screen.Dashboard -> strings.navDashboard
                            Screen.Customers -> strings.navCustomers
                            Screen.Sales -> strings.navSales
                            Screen.Expenses -> strings.navExpenses
                            Screen.Suppliers -> strings.navSuppliers
                            Screen.Reports -> strings.navReports
                        }
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = title
                                )
                            },
                            label = { Text(title) },
                            selected = selected,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            modifier = Modifier.testTag("nav_item_${screen.route}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToSales = {
                        navController.navigate(Screen.Sales.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToExpenses = {
                        navController.navigate(Screen.Expenses.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToSuppliers = {
                        navController.navigate(Screen.Suppliers.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToCustomers = {
                        navController.navigate(Screen.Customers.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onQuickAddSale = {
                        openSalesAddDialog = true
                        navController.navigate(Screen.Sales.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onQuickAddExpense = {
                        openExpensesAddDialog = true
                        navController.navigate(Screen.Expenses.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            composable(Screen.Customers.route) {
                CustomersScreen(
                    viewModel = viewModel,
                    onNavigateToCustomerDetail = { customerId ->
                        navController.navigate("customer_detail/$customerId")
                    }
                )
            }

            composable(Screen.Sales.route) {
                SalesScreen(
                    viewModel = viewModel,
                    onOpenAddDialogRequested = openSalesAddDialog,
                    onAddDialogOpened = { openSalesAddDialog = false }
                )
            }

            composable(Screen.Expenses.route) {
                ExpensesScreen(
                    viewModel = viewModel,
                    onOpenAddDialogRequested = openExpensesAddDialog,
                    onAddDialogOpened = { openExpensesAddDialog = false }
                )
            }

            composable(Screen.Suppliers.route) {
                SuppliersScreen(
                    viewModel = viewModel,
                    onNavigateToSupplierDetail = { supplierId ->
                        navController.navigate("supplier_detail/$supplierId")
                    }
                )
            }

            composable(Screen.Reports.route) {
                ReportsScreen(viewModel = viewModel)
            }

            composable(
                route = "supplier_detail/{supplierId}",
                arguments = listOf(navArgument("supplierId") { type = NavType.LongType })
            ) { backStackEntry ->
                val supplierId = backStackEntry.arguments?.getLong("supplierId") ?: 0L
                SupplierDetailScreen(
                    supplierId = supplierId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "customer_detail/{customerId}",
                arguments = listOf(navArgument("customerId") { type = NavType.LongType })
            ) { backStackEntry ->
                val customerId = backStackEntry.arguments?.getLong("customerId") ?: 0L
                CustomerDetailScreen(
                    customerId = customerId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("settings") {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }

    if (showAddShopDialog) {
        AddEditShopDialog(
            title = "Add Another Shop",
            confirmButtonText = "Create Shop",
            onDismiss = { showAddShopDialog = false },
            onConfirm = { name ->
                viewModel.createShop(name)
                showAddShopDialog = false
            }
        )
    }

    if (shopToRename != null) {
        AddEditShopDialog(
            initialName = shopToRename!!.name,
            title = "Rename Shop",
            confirmButtonText = "Save Name",
            onDismiss = { shopToRename = null },
            onConfirm = { newName ->
                shopToRename?.let {
                    viewModel.updateShop(it.copy(name = newName))
                }
                shopToRename = null
            }
        )
    }

    if (shopToDelete != null) {
        DeleteShopConfirmDialog(
            shopName = shopToDelete!!.name,
            onDismiss = { shopToDelete = null },
            onConfirm = {
                shopToDelete?.let { viewModel.deleteShop(it) }
                shopToDelete = null
            }
        )
    }
}

@Deprecated("Use HisaabApp instead", ReplaceWith("HisaabApp(viewModel)"))
@Composable
fun ShopLedgerApp(viewModel: ShopViewModel) {
    HisaabApp(viewModel)
}
