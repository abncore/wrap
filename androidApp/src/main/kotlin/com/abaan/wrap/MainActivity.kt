package com.abaan.wrap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.abaan.wrap.ui.WrapApp
import com.abaan.wrap.ui.theme.WrapTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WrapTheme {
                WrapApp()
            }
        }
    }
}
