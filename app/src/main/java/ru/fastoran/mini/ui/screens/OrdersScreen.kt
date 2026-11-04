package ru.fastoran.mini.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun OrdersScreen() {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Мои заказы", style = MaterialTheme.typography.headlineMedium)
        Text("Здесь будут история заказов, статусы и повтор заказа.")
    }
}
