package ru.fastoran.mini.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Orange = Color(0xFFF36B2B)
@Composable
fun FastoranTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Orange,
            onPrimary = Color.White,
            secondary = Color(0xFFFFA36F),
            background = Color(0xFFFFFBF8),
            surface = Color.White
        ),
        content = content
    )
}
