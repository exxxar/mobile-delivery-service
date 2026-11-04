package ru.fastoran.mini.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CheckoutScreen(onBack: () -> Unit) {
    var address by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Оформление заказа", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Адрес доставки") }, modifier = Modifier.fillMaxWidth())
        Text("Способ оплаты и расчёт доставки подключаются к Laravel API.")
        Button(onClick = onBack) { Text("Назад") }
    }
}
