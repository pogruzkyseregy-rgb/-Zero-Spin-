package com.zerospin.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import com.zerospin.app.ui.ZeroSpinApp
import com.zerospin.app.ui.theme.ZeroSpinTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ZeroSpinTheme {
                ZeroSpinApp()
            }
        }
    }
}
