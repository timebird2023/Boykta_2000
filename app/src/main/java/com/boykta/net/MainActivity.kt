package com.boykta.net

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.rememberNavController
import com.boykta.net.ads.AdsManager
import com.boykta.net.navigation.NavGraph
import com.boykta.net.ui.theme.Background
import com.boykta.net.ui.theme.BoykataNetTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen (Android 12+ native splash)
        installSplashScreen()

        super.onCreate(savedInstanceState)

        // Initialize Start.io SDK once
        AdsManager.init(this)

        enableEdgeToEdge()

        // Force status bar and navigation bar to match our dark theme
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.isAppearanceLightStatusBars     = false
        insetsController.isAppearanceLightNavigationBars = false

        setContent {
            BoykataNetTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Background
                ) {
                    val navController = rememberNavController()
                    NavGraph(navController = navController)
                }
            }
        }
    }
}
