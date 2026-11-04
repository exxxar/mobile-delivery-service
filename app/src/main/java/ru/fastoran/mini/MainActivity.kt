package ru.fastoran.mini

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import ru.fastoran.mini.ui.FastoranApp
import ru.fastoran.mini.ui.theme.FastoranTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FastoranTheme { FastoranApp() } }
    }
}
