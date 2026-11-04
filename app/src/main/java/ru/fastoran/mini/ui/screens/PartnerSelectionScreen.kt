package ru.fastoran.mini.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.fastoran.mini.ui.theme.Orange

@Composable
fun PartnerSelectionScreen(onPartnerSelected: () -> Unit) {
    val partners = listOf("Ресторан-партнёр", "Кафе рядом", "Пиццерия")
    LazyColumn(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("Выберите партнёра", style = MaterialTheme.typography.headlineMedium) }
        item { Text("Сначала выберите заведение, у которого хотите заказать", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(partners) { name ->
            Card(Modifier.fillMaxWidth().clickable { onPartnerSelected() }, shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Storefront, null, tint = Orange, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(name, style = MaterialTheme.typography.titleMedium)
                        Text("Меню, доставка и самовывоз", style = MaterialTheme.typography.bodySmall)
                    }
                    Text("›", color = Orange, style = MaterialTheme.typography.headlineMedium)
                }
            }
        }
    }
}
