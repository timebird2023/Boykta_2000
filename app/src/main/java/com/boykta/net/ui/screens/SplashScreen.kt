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
import com.boykta.net.data.api.ApiClient
import com.boykta.net.data.local.TokenStorage
import com.boykta.net.navigation.Screen
import com.boykta.net.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

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
        delay(900)

        val isValidLocally = tokenStorage.isTokenValid()
        if (isValidLocally) {
            val token = tokenStorage.accessToken.firstOrNull() ?: ""
            val msisdn = tokenStorage.msisdn.firstOrNull() ?: ""

            if (token.isNotBlank() && msisdn.isNotBlank()) {
                // Perform live verification with Djezzy API (handled with OkHttp Authenticator)
                val isServerLive = withContext(Dispatchers.IO) {
                    try {
                        val response = ApiClient.api.getMainBalance("Bearer $token", msisdn)
                        response.isSuccessful || response.code() in 200..202
                    } catch (_: Exception) {
                        // In case of offline or intermittent network, trust local valid session
                        true
                    }
                }

                if (isServerLive) {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                    return@LaunchedEffect
                }
            }
        }

        // Clean up expired session silently and navigate to Auth
        try { tokenStorage.clearToken() } catch (_: Exception) {}
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
