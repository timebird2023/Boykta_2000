package com.boykta.net.ui.screens

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.boykta.net.data.receiver.SmsReceiver
import com.boykta.net.navigation.Screen
import com.boykta.net.ui.components.ErrorModal
import com.boykta.net.ui.theme.*
import com.boykta.net.viewmodel.AuthUiState
import com.boykta.net.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

@Composable
fun AuthScreen(navController: NavController, vm: AuthViewModel = viewModel()) {
    val context = LocalContext.current
    val uiState by vm.uiState.collectAsState()

    var accountName  by remember { mutableStateOf("") }
    var phone        by remember { mutableStateOf("") }
    var otp          by remember { mutableStateOf("") }
    var countdown    by remember { mutableIntStateOf(0) }
    var showError    by remember { mutableStateOf(false) }
    var errorMsg     by remember { mutableStateOf("") }

    val otpSent = uiState is AuthUiState.OtpSent || uiState is AuthUiState.Loading && otp.isEmpty()

    // OTP auto-read via SMS receiver
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                intent.getStringExtra(SmsReceiver.EXTRA_OTP_CODE)?.let { otp = it }
            }
        }
        context.registerReceiver(receiver, IntentFilter(SmsReceiver.ACTION_OTP_RECEIVED))
        onDispose { context.unregisterReceiver(receiver) }
    }

    // Countdown timer for resend
    LaunchedEffect(countdown) {
        if (countdown > 0) { delay(1000); countdown-- }
    }

    // React to state changes
    LaunchedEffect(uiState) {
        when (val s = uiState) {
            is AuthUiState.OtpSent -> countdown = 60
            is AuthUiState.Success -> navController.navigate(Screen.Dashboard.route) {
                popUpTo(Screen.Auth.route) { inclusive = true }
            }
            is AuthUiState.Error -> { errorMsg = s.message; showError = true }
            else -> {}
        }
    }

    if (showError) ErrorModal(message = errorMsg) { showError = false; vm.resetState() }

    Box(
        modifier = Modifier.fillMaxSize().background(Background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(40.dp))

            Text("boykta net", style = MaterialTheme.typography.displayLarge, color = Primary)
            Text("تسجيل الدخول", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)

            Spacer(Modifier.height(12.dp))

            // Account name
            AppTextField(
                value = accountName,
                onValueChange = { accountName = it },
                label = "اسم الحساب",
                placeholder = "مثال: رقم المنزل",
                keyboardType = KeyboardType.Text,
                enabled = uiState !is AuthUiState.Loading
            )

            // Phone number
            AppTextField(
                value = phone,
                onValueChange = { if (it.length <= 10) phone = it },
                label = "رقم الهاتف",
                placeholder = "07XXXXXXXX",
                keyboardType = KeyboardType.Phone,
                enabled = uiState !is AuthUiState.Loading && uiState !is AuthUiState.OtpSent
            )

            // OTP field — shown after OTP is sent
            if (uiState is AuthUiState.OtpSent) {
                AppTextField(
                    value = otp,
                    onValueChange = { if (it.length <= 8) otp = it },
                    label = "رمز التحقق",
                    placeholder = "أدخل الرمز المُرسَل برسالة SMS",
                    keyboardType = KeyboardType.NumberPassword,
                    enabled = uiState !is AuthUiState.Loading
                )

                // Resend button with countdown
                TextButton(
                    onClick = { if (countdown == 0) { vm.requestOtp(phone); countdown = 60 } },
                    enabled = countdown == 0
                ) {
                    Text(
                        text = if (countdown > 0) "إعادة الإرسال بعد ${countdown}ث" else "إعادة إرسال الرمز",
                        color = if (countdown > 0) TextHint else Primary
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // Primary action button
            Button(
                onClick = {
                    if (uiState is AuthUiState.OtpSent) {
                        vm.verifyOtp(otp, accountName, phone)
                    } else {
                        vm.requestOtp(phone)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = uiState !is AuthUiState.Loading,
                colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = OnPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (uiState is AuthUiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = OnPrimary, strokeWidth = 2.dp)
                } else {
                    Text(
                        text = if (uiState is AuthUiState.OtpSent) "تحقق وتسجيل الدخول" else "إرسال رمز التحقق",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    keyboardType: KeyboardType,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextSecondary) },
        placeholder = { Text(placeholder, color = TextHint) },
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor    = Primary,
            unfocusedBorderColor  = Border,
            focusedTextColor      = TextPrimary,
            unfocusedTextColor    = TextPrimary,
            cursorColor           = Primary,
            disabledBorderColor   = Border,
            disabledTextColor     = TextSecondary
        ),
        shape = RoundedCornerShape(10.dp)
    )
}
