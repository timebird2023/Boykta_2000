package com.boykta.net.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.boykta.net.ads.AdsManager
import com.boykta.net.data.api.ApiClient
import com.boykta.net.notifications.NotificationScheduler
import com.boykta.net.data.local.TokenStorage
import com.boykta.net.data.models.ActivateProductRequest
import com.boykta.net.data.models.PaidOffer
import com.boykta.net.ui.components.ConfirmModal
import com.boykta.net.ui.components.ErrorModal
import com.boykta.net.ui.components.SuccessModal
import com.boykta.net.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.TimeUnit

// ── ViewModel ─────────────────────────────────────────────────────────────────

sealed class WalkUiState {
    object Idle    : WalkUiState()
    object Loading : WalkUiState()
    object Success : WalkUiState()
    data class Error(val message: String) : WalkUiState()
}

class WalkWinViewModel(application: android.app.Application) : AndroidViewModel(application) {
    private val tokenStorage = TokenStorage(application)
    private val api = ApiClient.api

    private val _state = MutableStateFlow<WalkUiState>(WalkUiState.Idle)
    val state: StateFlow<WalkUiState> = _state.asStateFlow()

    /** Last activation time in ms (0 if never activated). */
    val lastActivationMs: StateFlow<Long> = tokenStorage.walkLastActivation
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    fun activateWalk2Gb() {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = WalkUiState.Loading
            val token  = tokenStorage.accessToken.firstOrNull()
                ?: run { _state.value = WalkUiState.Error("انتهت الجلسة"); return@launch }
            val msisdn = tokenStorage.msisdn.firstOrNull()
                ?: run { _state.value = WalkUiState.Error("انتهت الجلسة"); return@launch }
            val auth = "Bearer $token"
            try {
                api.checkWalkCampaign(auth, msisdn)
                val resp = api.activateWalkReward(auth, msisdn, ActivateProductRequest("GIFTWALKWIN2GO"))
                if (resp.isSuccessful || resp.code() in 200..201) {
                    tokenStorage.saveWalkActivationTime(System.currentTimeMillis())
                    _state.value = WalkUiState.Success
                } else {
                    val ar = Regex(""""ar"\s*:\s*"([^"]+)"""").find(resp.errorBody()?.string() ?: "")?.groupValues?.get(1) ?: ""
                    _state.value = WalkUiState.Error(ar.ifBlank { "فشل التفعيل (${resp.code()})." })
                }
            } catch (e: Exception) { _state.value = WalkUiState.Error("تعذّر الاتصال بالخادم.") }
        }
    }

    /** Activate the 100 DZD baseline offer so the user qualifies for Walk & Win. */
    fun activate100DzdOffer() {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = WalkUiState.Loading
            val token  = tokenStorage.accessToken.firstOrNull()
                ?: run { _state.value = WalkUiState.Error("انتهت الجلسة"); return@launch }
            val msisdn = tokenStorage.msisdn.firstOrNull()
                ?: run { _state.value = WalkUiState.Error("انتهت الجلسة"); return@launch }
            try {
                // 100 DZD / 2 GB / 24h offer — same as PAID_OFFERS entry "2"
                val resp = api.activateProduct("Bearer $token", msisdn, ActivateProductRequest("DOVINTSPEEDDAY1GoPRE"))
                if (resp.isSuccessful || resp.code() in 200..201) {
                    _state.value = WalkUiState.Success
                } else {
                    val ar = Regex(""""ar"\s*:\s*"([^"]+)"""").find(resp.errorBody()?.string() ?: "")?.groupValues?.get(1) ?: ""
                    _state.value = WalkUiState.Error(ar.ifBlank { "فشل تفعيل العرض (${resp.code()})." })
                }
            } catch (e: Exception) { _state.value = WalkUiState.Error("تعذّر الاتصال بالخادم.") }
        }
    }

    fun reset() { _state.value = WalkUiState.Idle }
}

// ── Screen ────────────────────────────────────────────────────────────────────

private val WEEK_MS = TimeUnit.DAYS.toMillis(7)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalkWinScreen(navController: NavController, vm: WalkWinViewModel = viewModel()) {
    val context        = LocalContext.current
    val activity       = context as? Activity
    val uiState        by vm.state.collectAsState()
    val lastActivation by vm.lastActivationMs.collectAsState()

    var showSuccess     by remember { mutableStateOf(false) }
    var showError       by remember { mutableStateOf(false) }
    var errorMsg        by remember { mutableStateOf("") }
    var showConfirm100  by remember { mutableStateOf(false) }

    // Live countdown — updates every second
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            now = System.currentTimeMillis()
        }
    }

    val hasRecentActivation = lastActivation > 0 && (now - lastActivation) < WEEK_MS
    val remainingMs = if (hasRecentActivation) WEEK_MS - (now - lastActivation) else 0L

    fun formatCountdown(ms: Long): String {
        if (ms <= 0) return "0د 0ث"
        val days  = TimeUnit.MILLISECONDS.toDays(ms)
        val hours = TimeUnit.MILLISECONDS.toHours(ms) % 24
        val mins  = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
        val secs  = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
        return when {
            days > 0  -> String.format(Locale.US, "%d يوم %02d:%02d:%02d", days, hours, mins, secs)
            hours > 0 -> String.format(Locale.US, "%02d:%02d:%02d", hours, mins, secs)
            else      -> String.format(Locale.US, "%02d:%02d", mins, secs)
        }
    }

    // Ensure notification channel exists
    LaunchedEffect(Unit) { NotificationScheduler.createChannel(context) }

    LaunchedEffect(uiState) {
        when (val s = uiState) {
            is WalkUiState.Success -> {
                showSuccess = true
                // Schedule a local reminder 7 days from now
                NotificationScheduler.scheduleWalkWinReminder(context)
            }
            is WalkUiState.Error   -> { errorMsg = s.message; showError = true }
            else -> {}
        }
    }

    if (showSuccess) SuccessModal { showSuccess = false; vm.reset(); activity?.let { AdsManager.showInterstitial(it) } }
    if (showError)   ErrorModal(errorMsg) { showError = false; vm.reset() }

    if (showConfirm100) {
        ConfirmModal(
            title    = "تفعيل عرض 100 دج",
            subtitle = "سيتم تفعيل عرض 2 جيجابايت بقيمة 100 دج. هذا سيؤهلك للحصول على مكافأة امشِ واربح.",
            onConfirm = { showConfirm100 = false; vm.activate100DzdOffer() },
            onDismiss = { showConfirm100 = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("امشِ واربح 2 جيجابايت", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "رجوع", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background, titleContentColor = TextPrimary)
            )
        },
        containerColor = Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))

            // ── Icon ──────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(Primary.copy(alpha = 0.12f), RoundedCornerShape(48.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.DirectionsRun, null, tint = Primary, modifier = Modifier.size(52.dp))
            }

            Text(
                "2 جيجابايت أسبوعية مجاناً",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )

            // ── Countdown timer (if recently activated) ───────────────────────
            if (hasRecentActivation) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.4f))
                ) {
                    Column(
                        Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Outlined.Timer, null, tint = Primary, modifier = Modifier.size(18.dp))
                            Text(
                                "الوقت المتبقي للتفعيل التالي",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                        }
                        Text(
                            formatCountdown(remainingMs),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Primary
                        )
                    }
                }
            }

            // ── Info card ─────────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Border)
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "كيف تعمل هذه الخدمة؟",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "للاستفادة من 2 جيجابايت الأسبوعية المجانية، يجب عليك أولاً تفعيل عرض بقيمة 100 دج أو أكثر خلال الشهر الحالي، ثم اضغط على زر التفعيل أدناه.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Start
                    )
                    Text(
                        "يمكن التفعيل مرة واحدة كل 7 أيام.",
                        style = MaterialTheme.typography.labelMedium,
                        color = Primary.copy(alpha = 0.8f)
                    )
                }
            }

            // ── Activate 2GB button ───────────────────────────────────────────
            Button(
                onClick  = { vm.activateWalk2Gb() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled  = uiState !is WalkUiState.Loading && !hasRecentActivation,
                colors   = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = OnPrimary),
                shape    = RoundedCornerShape(14.dp)
            ) {
                if (uiState is WalkUiState.Loading)
                    CircularProgressIndicator(Modifier.size(22.dp), color = OnPrimary, strokeWidth = 2.dp)
                else
                    Text(
                        if (hasRecentActivation) "تم التفعيل — انتظر الأسبوع القادم"
                        else "تفعيل 2 جيجابايت الآن",
                        fontWeight = FontWeight.Bold
                    )
            }

            // ── Divider ───────────────────────────────────────────────────────
            HorizontalDivider(color = Border)

            // ── 100 DZD shortcut ──────────────────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "لم تفعّل عرض 100 دج بعد؟",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                OutlinedButton(
                    onClick  = { showConfirm100 = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled  = uiState !is WalkUiState.Loading,
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = Primary)
                ) {
                    Text("تفعيل عرض 100 دج الآن", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
