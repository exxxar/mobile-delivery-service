package ru.fastoran.mini.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.fastoran.mini.ui.theme.Orange

@Composable
fun CartScreen(onCheckout: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Корзина", style = MaterialTheme.typography.headlineMedium)
        Icon(Icons.Default.ShoppingBag, null, tint = Orange, modifier = Modifier.size(54.dp).align(Alignment.CenterHorizontally))
        Text("Корзина пока пустая", modifier = Modifier.align(Alignment.CenterHorizontally))
        Text("Добавьте блюда из меню. Здесь будут отображаться товары, варианты, добавки и итоговая сумма.")
        Button(onClick = onCheckout, modifier = Modifier.fillMaxWidth()) { Text("Перейти к оформлению") }
    }
}
