package ru.fastoran.mini.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ProfileScreen() {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Профиль", style = MaterialTheme.typography.headlineMedium)
        Text("Гостевой аккаунт")
        OutlinedButton(onClick = {}) { Text("Зарегистрироваться / войти") }
        OutlinedButton(onClick = {}) { Text("Мои адреса") }
    }
}
