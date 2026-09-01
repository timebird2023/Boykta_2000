package com.boykta.net.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.boykta.net.R
import com.boykta.net.data.local.TokenStorage
import com.boykta.net.navigation.Screen
import com.boykta.net.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull

@Composable
fun SplashScreen(navController: NavController) {
    val context = navController.context
    val tokenStorage = remember { TokenStorage(context) }

    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "logo_alpha"
    )

    LaunchedEffect(Unit) {
        delay(800)
        val isValidLocally = tokenStorage.isTokenValid()
        if (isValidLocally) {
            val token = tokenStorage.accessToken.firstOrNull() ?: ""
            val refreshToken = tokenStorage.refreshToken.firstOrNull() ?: ""
            val msisdn = tokenStorage.msisdn.firstOrNull() ?: ""

            if (msisdn.isNotBlank() && (token.isNotBlank() || refreshToken.isNotBlank())) {
                // Persistent session: Trust local session and let ApiClient auto-refresh in background.
                // Never kick the user to login screen due to weak internet during splash!
                navController.navigate(Screen.Dashboard.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
                return@LaunchedEffect
            }
        }

        // Navigate to Auth screen only if user has never logged in or explicitly logged out
        navController.navigate(Screen.Auth.route) {
            popUpTo(Screen.Splash.route) { inclusive = true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.alpha(alpha)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_splash_icon),
                contentDescription = "boykta net",
                modifier = Modifier
                    .size(130.dp)
                    .clip(CircleShape)
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = "boykta net",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = Primary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "بوت خدمات جازي الذكي",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                fontSize = 15.sp
            )
        }
    }
}
