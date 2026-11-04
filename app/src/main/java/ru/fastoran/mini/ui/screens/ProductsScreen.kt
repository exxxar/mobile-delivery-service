package ru.fastoran.mini.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.fastoran.mini.ui.theme.Orange

private data class DemoProduct(val name: String, val price: String)
@Composable
fun ProductsScreen(onAddToCart: () -> Unit) {
    val products = listOf(
        DemoProduct("Бургер классический", "390 ₽"),
        DemoProduct("Пицца Пепперони", "590 ₽"),
        DemoProduct("Картофель фри", "190 ₽"),
        DemoProduct("Комбо-набор", "750 ₽")
    )
    LazyColumn(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Меню партнёра", style = MaterialTheme.typography.headlineMedium) }
        items(products) { product ->
            Card(shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Fastfood, null, tint = Orange, modifier = Modifier.size(42.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(product.name, style = MaterialTheme.typography.titleMedium)
                        Text(product.price, color = Orange)
                    }
                    FilledIconButton(onClick = onAddToCart) { Icon(Icons.Default.Add, null) }
                }
            }
        }
    }
}
