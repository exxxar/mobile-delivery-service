package ru.fastoran.mini.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.*
import ru.fastoran.mini.ui.screens.*

@Composable
fun FastoranApp() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route ?: "partners"
    Scaffold(
        bottomBar = {
            NavigationBar {
                val items = listOf(
                    Triple("partners", "Партнёры", Icons.Default.Storefront),
                    Triple("products", "Товары", Icons.Default.Home),
                    Triple("cart", "Корзина", Icons.Default.ShoppingBag),
                    Triple("orders", "Заказы", Icons.Default.ReceiptLong),
                    Triple("profile", "Профиль", Icons.Default.Person)
                )
                items.forEach { (route, label, icon) ->
                    NavigationBarItem(
                        selected = current == route,
                        onClick = { nav.navigate(route) { launchSingleTop = true; popUpTo("partners") } },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(navController = nav, startDestination = "partners", modifier = Modifier.padding(padding)) {
            composable("partners") { PartnerSelectionScreen(onPartnerSelected = { nav.navigate("products") }) }
            composable("products") { ProductsScreen(onAddToCart = { nav.navigate("cart") }) }
            composable("cart") { CartScreen(onCheckout = { nav.navigate("checkout") }) }
            composable("checkout") { CheckoutScreen(onBack = { nav.popBackStack() }) }
            composable("orders") { OrdersScreen() }
            composable("profile") { ProfileScreen() }
        }
    }
}
