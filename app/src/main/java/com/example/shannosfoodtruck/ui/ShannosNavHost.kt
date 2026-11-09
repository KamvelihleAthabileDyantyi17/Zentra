package com.example.shannosfoodtruck.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.shannosfoodtruck.AppContainer
import com.example.shannosfoodtruck.ui.customer.CartScreen
import com.example.shannosfoodtruck.ui.customer.CheckoutScreen
import com.example.shannosfoodtruck.ui.customer.CustomerViewModel
import com.example.shannosfoodtruck.ui.customer.FeedbackScreen
import com.example.shannosfoodtruck.ui.customer.HomeScreen
import com.example.shannosfoodtruck.ui.customer.MenuScreen
import com.example.shannosfoodtruck.ui.customer.ThanksScreen
import com.example.shannosfoodtruck.ui.customer.TrackScreen
import com.example.shannosfoodtruck.ui.customer.TruckPickerScreen
import com.example.shannosfoodtruck.ui.staff.StaffScreen
import com.example.shannosfoodtruck.ui.staff.StaffViewModel
import com.example.shannosfoodtruck.ui.theme.Kasi

object Routes {
    const val HOME = "home"
    const val TRUCKS = "trucks"
    const val MENU = "menu"
    const val CART = "cart"
    const val CHECKOUT = "checkout"
    const val TRACK = "track"
    const val FEEDBACK = "feedback"
    const val THANKS = "thanks"
    const val STAFF = "staff"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, "Home", Icons.Filled.Home),
    Tab(Routes.MENU, "Menu", Icons.Filled.Menu),
    Tab(Routes.CART, "Order", Icons.Filled.ShoppingCart),
    Tab(Routes.TRACK, "Track", Icons.Filled.LocationOn),
)

@Composable
fun ShannosNavHost(container: AppContainer) {
    val nav = rememberNavController()
    // One customer ViewModel shared by every customer screen (cart, checkout, tracking all need the same data)
    val customerVm: CustomerViewModel = viewModel(factory = viewModelFactory {
        initializer { CustomerViewModel(container.repository, container.auth, container.activeOrderStore) }
    })
    val state by customerVm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            customerVm.dismissMessage()
        }
    }

    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val showTabs = route != null && tabs.any { it.route == route }

    Scaffold(
        containerColor = Kasi.Paper,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (showTabs) {
                NavigationBar(containerColor = Color.White) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = { nav.goToTab(tab.route) },
                            label = { Text(tab.label) },
                            icon = {
                                if (tab.route == Routes.CART && state.cartCount > 0) {
                                    BadgedBox(badge = { Badge(containerColor = Kasi.Chilli) { Text("${state.cartCount}") } }) {
                                        Icon(tab.icon, contentDescription = null)
                                    }
                                } else {
                                    Icon(tab.icon, contentDescription = null)
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Kasi.Chilli,
                                selectedTextColor = Kasi.Chilli,
                                indicatorColor = Kasi.SignSoft,
                                unselectedIconColor = Kasi.Muted,
                                unselectedTextColor = Kasi.Muted,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = Routes.HOME, modifier = Modifier.padding(padding)) {
            composable(Routes.HOME) {
                HomeScreen(
                    state = state,
                    onOrderNow = { nav.goToTab(Routes.MENU) },
                    onPickTruck = { nav.navigate(Routes.TRUCKS) },
                    onTrack = { nav.goToTab(Routes.TRACK) },
                    onAdd = customerVm::add,
                    onStaff = { nav.navigate(Routes.STAFF) },
                )
            }
            composable(Routes.TRUCKS) {
                TruckPickerScreen(
                    state = state,
                    onPick = { customerVm.selectTruck(it); nav.popBackStack() },
                    onBack = { nav.popBackStack() },
                )
            }
            composable(Routes.MENU) {
                MenuScreen(state, onCategory = customerVm::selectCategory, onAdd = customerVm::add, onRemove = customerVm::removeOne)
            }
            composable(Routes.CART) {
                CartScreen(
                    state = state,
                    onAdd = customerVm::add,
                    onRemove = customerVm::removeOne,
                    onNoteChange = { note -> customerVm.updateForm { it.copy(note = note) } },
                    onBrowse = { nav.goToTab(Routes.MENU) },
                    onCheckout = { nav.navigate(Routes.CHECKOUT) },
                )
            }
            composable(Routes.CHECKOUT) {
                CheckoutScreen(
                    state = state,
                    onFormChange = customerVm::updateForm,
                    onBack = { nav.popBackStack() },
                    onPlace = {
                        customerVm.placeOrder {
                            nav.navigate(Routes.TRACK) { popUpTo(Routes.HOME) }
                        }
                    },
                )
            }
            composable(Routes.TRACK) {
                TrackScreen(
                    state = state,
                    onBrowse = { nav.goToTab(Routes.MENU) },
                    onRate = { nav.navigate(Routes.FEEDBACK) },
                )
            }
            composable(Routes.FEEDBACK) {
                FeedbackScreen(
                    onBack = { nav.popBackStack() },
                    onSend = { rating, comment ->
                        customerVm.submitFeedback(rating, comment) {
                            nav.navigate(Routes.THANKS) { popUpTo(Routes.HOME) }
                        }
                    },
                )
            }
            composable(Routes.THANKS) {
                ThanksScreen(onHome = { nav.goToTab(Routes.HOME) })
            }
            composable(Routes.STAFF) {
                val staffVm: StaffViewModel = viewModel(factory = viewModelFactory {
                    initializer { StaffViewModel(container.repository, container.auth) }
                })
                StaffScreen(staffVm, onExit = { nav.popBackStack() })
            }
        }
    }
}

/** Switch tabs without piling up screens on the back stack. */
private fun NavHostController.goToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
