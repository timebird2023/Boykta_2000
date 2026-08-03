package com.boykta.net.ui.screens

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.boykta.net.data.receiver.SmsReceiver
import com.boykta.net.navigation.Screen
import com.boykta.net.ui.components.ErrorModal
import com.boykta.net.ui.theme.*
import com.boykta.net.viewmodel.AuthUiState
import com.boykta.net.viewmodel.AuthViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(navController: NavController, vm: AuthViewModel = viewModel()) {
    val context   = LocalContext.current
    val scope     = rememberCoroutineScope()
    val uiState   by vm.uiState.collectAsState()

    var accountName by remember { mutableStateOf("") }
    var phone       by remember { mutableStateOf("") }
    var otpDigits   by remember { mutableStateOf("") }   // up to 6 chars
    var countdown   by remember { mutableIntStateOf(0) }
    var showError   by remember { mutableStateOf(false) }
    var errorMsg    by remember { mutableStateOf("") }

    // Shake animation state
    val shakeOffset   = remember { Animatable(0f) }
    val otpFocusReq   = remember { FocusRequester() }

    val otpSent = uiState is AuthUiState.OtpSent

    // ── SMS auto-read ─────────────────────────────────────────────────────────
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                intent.getStringExtra(SmsReceiver.EXTRA_OTP_CODE)?.let { code ->
                    otpDigits = code.take(6)
                }
            }
        }
        ContextCompat.registerReceiver(
            context, receiver,
            IntentFilter(SmsReceiver.ACTION_OTP_RECEIVED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        onDispose { context.unregisterReceiver(receiver) }
    }

    // ── Countdown timer ───────────────────────────────────────────────────────
    LaunchedEffect(countdown) {
        if (countdown > 0) { delay(1000); countdown-- }
    }

    // ── State reactions ───────────────────────────────────────────────────────
    LaunchedEffect(uiState) {
        when (val s = uiState) {
            is AuthUiState.OtpSent -> {
                countdown = 60
                // Auto-focus OTP field
                try { otpFocusReq.requestFocus() } catch (_: Exception) {}
            }
            is AuthUiState.Success -> navController.navigate(Screen.Dashboard.route) {
                popUpTo(Screen.Auth.route) { inclusive = true }
            }
            is AuthUiState.Error -> {
                // If OTP error → shake the OTP boxes
                if (otpSent) {
                    scope.launch {
                        for (i in 0..4) {
                            shakeOffset.animateTo(if (i % 2 == 0) 12f else -12f,
                                animationSpec = tween(60, easing = LinearEasing))
                        }
                        shakeOffset.animateTo(0f, spring())
                    }
                    otpDigits = ""
                }
                errorMsg  = s.message
                showError = true
            }
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
            Spacer(Modifier.height(48.dp))

            Text(
                "boykta net",
                style = MaterialTheme.typography.displayLarge,
                color = Primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                "تسجيل الدخول",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(Modifier.height(12.dp))

            // ── Account name ──────────────────────────────────────────────────
            AppTextField(
                value       = accountName,
                onValueChange = { accountName = it },
                label       = "اسم الحساب",
                placeholder = "مثال: رقم المنزل",
                keyboardType = KeyboardType.Text,
                enabled     = uiState !is AuthUiState.Loading
            )

            // ── Phone number ──────────────────────────────────────────────────
            AppTextField(
                value       = phone,
                onValueChange = { if (it.length <= 10) phone = it },
                label       = "رقم الهاتف",
                placeholder = "07XXXXXXXX",
                keyboardType = KeyboardType.Phone,
                enabled     = uiState !is AuthUiState.Loading && !otpSent
            )

            // ── 6-box OTP field ───────────────────────────────────────────────
            if (otpSent) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "أدخل رمز التحقق المُرسَل بـ SMS",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )

                    // Hidden input for keyboard capture
                    BasicTextField(
                        value = otpDigits,
                        onValueChange = { new ->
                            if (new.all { it.isDigit() } && new.length <= 6) otpDigits = new
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier
                            .size(1.dp)   // invisible but focusable
                            .focusRequester(otpFocusReq),
                        cursorBrush = SolidColor(Primary),
                        enabled = uiState !is AuthUiState.Loading
                    )

                    // 6 visual boxes
                    Row(
                        modifier = Modifier.offset(x = shakeOffset.value.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        (0..5).forEach { idx ->
                            val char = otpDigits.getOrNull(idx)?.toString() ?: ""
                            val isFilled = char.isNotEmpty()
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(CardBg)
                                    .border(
                                        width = if (idx == otpDigits.length) 2.dp else 1.dp,
                                        color = when {
                                            idx == otpDigits.length -> Primary
                                            isFilled                -> Primary.copy(alpha = 0.6f)
                                            else                    -> Border
                                        },
                                        shape = RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = char,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        }
                    }

                    // Resend timer
                    TextButton(
                        onClick = {
                            if (countdown == 0) {
                                vm.requestOtp(phone)
                                countdown = 60
                                otpDigits = ""
                            }
                        },
                        enabled = countdown == 0
                    ) {
                        Text(
                            text  = if (countdown > 0) "إعادة الإرسال بعد ${countdown}ث" else "إعادة إرسال الرمز",
                            color = if (countdown > 0) TextHint else Primary
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // ── Primary action button ─────────────────────────────────────────
            Button(
                onClick = {
                    if (otpSent) {
                        if (otpDigits.length == 6) vm.verifyOtp(otpDigits, accountName, phone)
                    } else {
                        vm.requestOtp(phone)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = uiState !is AuthUiState.Loading,
                colors  = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = OnPrimary),
                shape   = RoundedCornerShape(12.dp)
            ) {
                if (uiState is AuthUiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = OnPrimary, strokeWidth = 2.dp)
                } else {
                    Text(
                        text = if (otpSent) "تحقق وتسجيل الدخول" else "إرسال رمز التحقق",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(48.dp))
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
        value       = value,
        onValueChange = onValueChange,
        label       = { Text(label, color = TextSecondary) },
        placeholder = { Text(placeholder, color = TextHint) },
        modifier    = Modifier.fillMaxWidth(),
        enabled     = enabled,
        singleLine  = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = Primary,
            unfocusedBorderColor = Border,
            focusedTextColor     = TextPrimary,
            unfocusedTextColor   = TextPrimary,
            cursorColor          = Primary,
            disabledBorderColor  = Border,
            disabledTextColor    = TextSecondary
        ),
        shape = RoundedCornerShape(10.dp)
    )
}
