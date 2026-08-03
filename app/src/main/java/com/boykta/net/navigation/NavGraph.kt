package com.boykta.net.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.boykta.net.data.api.SessionManager
import com.boykta.net.ui.screens.*

@Composable
fun NavGraph(navController: NavHostController) {

    // When the server invalidates our token (e.g. Facebook bot logs in with same number),
    // the Authenticator in ApiClient emits this signal after a failed refresh attempt.
    LaunchedEffect(Unit) {
        SessionManager.sessionExpired.collect {
            navController.navigate(Screen.Auth.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(navController = navController, startDestination = Screen.Splash.route) {

        composable(Screen.Splash.route) {
            SplashScreen(navController = navController)
        }

        composable(Screen.Auth.route) {
            AuthScreen(navController = navController)
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(navController = navController)
        }

        composable(Screen.Offers.route) {
            OffersScreen(navController = navController)
        }

        composable(Screen.Flexy.route) {
            FlexyScreen(navController = navController)
        }

        composable(Screen.FreeSms.route) {
            FreeSmsScreen(navController = navController)
        }

        composable(Screen.WalkWin.route) {
            WalkWinScreen(navController = navController)
        }

        composable(Screen.Settings.route) {
            SettingsScreen(navController = navController)
        }

        composable(Screen.Mgm.route) {
            MgmScreen(navController = navController)
        }

        composable(Screen.PrivacyPolicy.route) {
            PrivacyPolicyScreen(navController = navController)
        }
    }
}
