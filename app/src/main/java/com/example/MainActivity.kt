package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.WaterSortScreen
import com.example.ui.theme.SpaceDeep
import com.example.ui.theme.WaterSortTheme
import com.example.viewmodel.WaterSortViewModel

/**
 * Main Activity hosting the Water Sort native Android application.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Kyun: User requirement B.6 — Dark status bar aur navigation bar style taaki light-mode phones par bhi icons clearly dikhein
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        setContent {
            WaterSortTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = SpaceDeep
                ) {
                    val viewModel: WaterSortViewModel = viewModel()
                    WaterSortScreen(viewModel = viewModel)
                }
            }
        }
    }
}
