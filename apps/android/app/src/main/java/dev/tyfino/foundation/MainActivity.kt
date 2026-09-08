package dev.tyfino.foundation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.tyfino.foundation.app.TyfinoApp
import dev.tyfino.foundation.ui.theme.TyfinoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TyfinoTheme {
                TyfinoApp()
            }
        }
    }
}
