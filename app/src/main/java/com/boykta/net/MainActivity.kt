package com.boykta.net

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.rememberNavController
import com.boykta.net.ads.AdsManager
import com.boykta.net.data.local.TokenStorage
import com.boykta.net.navigation.NavGraph
import com.boykta.net.ui.theme.BoykataNetTheme
import com.boykta.net.ui.theme.DarkAppColors
import com.boykta.net.ui.theme.LightAppColors

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        AdsManager.init(this)
        com.boykta.net.data.api.ApiClient.init(this)

        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)

        setContent {
            val tokenStorage  = remember { TokenStorage(this@MainActivity) }
            val isDarkTheme   by tokenStorage.isDarkTheme.collectAsState(initial = true)

            // Sync status-bar / nav-bar icon colours with the active theme
            SideEffect {
                insetsController.isAppearanceLightStatusBars     = !isDarkTheme
                insetsController.isAppearanceLightNavigationBars = !isDarkTheme
            }

            BoykataNetTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color    = if (isDarkTheme) DarkAppColors.background else LightAppColors.background
                ) {
                    val navController = rememberNavController()
                    NavGraph(navController = navController)
                }
            }
        }
    }
}
