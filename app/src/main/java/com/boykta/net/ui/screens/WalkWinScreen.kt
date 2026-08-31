package com.boykta.net.ui.screens

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
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
import com.boykta.net.data.local.TokenStorage
import com.boykta.net.data.models.ActivateProductRequest
import com.boykta.net.ui.components.ConfirmModal
import com.boykta.net.ui.components.ErrorModal
import com.boykta.net.ui.components.SuccessModal
import com.boykta.net.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ── ViewModel for Walk & Win ──────────────────────────────────────────────────
sealed class WalkUiState {
    object Idle    : WalkUiState()
    object Loading : WalkUiState()
    object Success : WalkUiState()
    data class Error(val message: String) : WalkUiState()
}

class WalkWinViewModel(application: android.app.Application) : AndroidViewModel(application) {
    private val tokenStorage = TokenStorage(application)
    private val api = ApiClient.api

    private val _uiState = MutableStateFlow<WalkUiState>(WalkUiState.Idle)
    val uiState: StateFlow<WalkUiState> = _uiState.asStateFlow()

    val lastActivationTime: StateFlow<Long> = tokenStorage.walkLastActivation
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    fun activateWalk2Gb() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = WalkUiState.Loading
            val token = tokenStorage.accessToken.firstOrNull() ?: ""
            val msisdn = tokenStorage.msisdn.firstOrNull() ?: ""

            if (token.isBlank() || msisdn.isBlank()) {
                _uiState.value = WalkUiState.Error("انتهت الجلسة. يرجى تسجيل الدخول مجدداً.")
                return@launch
            }

            val auth = "Bearer $token"
            try {
                // Pre-check campaign status
                try { api.checkWalkCampaign(auth, msisdn) } catch (_: Exception) {}

                val resp = api.activateWalkReward(auth, msisdn, ActivateProductRequest("GIFTWALKWIN2GO"))
                if (resp.isSuccessful || resp.code() in 200..202) {
                    val now = System.currentTimeMillis()
                    tokenStorage.saveWalkActivationTime(now)
                    _uiState.value = WalkUiState.Success
                } else if (resp.code() == 403) {
                    _uiState.value = WalkUiState.Error(
                        "العرض غير متاح حالياً على خطك.\n\nوفقاً لقانون جازي (Walk & Win)، يجب تعبئة رصيد بقيمة 100 دج على الأقل وتفعيل باقة خلال الشهر للاستفادة من 2 جيجابايت المجانية أسبوعياً."
                    )
                } else if (resp.code() in listOf(402, 404, 405, 409)) {
                    _uiState.value = WalkUiState.Error("لم يكتمل الأسبوع بعد منذ آخر تفعيل. يرجى الانتظار حتى انتهاء المدة.")
                } else {
                    _uiState.value = WalkUiState.Error("تعذّر تفعيل الباقة (${resp.code()}). يرجى المحاولة لاحقاً.")
                }
            } catch (e: Exception) {
                _uiState.value = WalkUiState.Error("تعذّر الاتصال بالخادم. تحقق من اتصال الإنترنت.")
            }
        }
    }

    fun activate100DzdOffer() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = WalkUiState.Loading
            val token = tokenStorage.accessToken.firstOrNull() ?: ""
            val msisdn = tokenStorage.msisdn.firstOrNull() ?: ""

            if (token.isBlank() || msisdn.isBlank()) {
                _uiState.value = WalkUiState.Error("انتهت الجلسة.")
                return@launch
            }

            try {
                val resp = api.activateProduct("Bearer $token", msisdn, ActivateProductRequest("DOVINTSPEEDDAY1GoPRE"))
                if (resp.isSuccessful || resp.code() in 200..202) {
                    _uiState.value = WalkUiState.Success
                } else if (resp.code() == 402) {
                    _uiState.value = WalkUiState.Error("رصيدك غير كافٍ. المطلوب 100 دج على الأقل.")
                } else {
                    _uiState.value = WalkUiState.Error("تعذّر تفعيل عرض 100 دج (${resp.code()}).")
                }
            } catch (e: Exception) {
                _uiState.value = WalkUiState.Error("تعذّر الاتصال بالخادم.")
            }
        }
    }

    fun reset() {
        _uiState.value = WalkUiState.Idle
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalkWinScreen(
    navController: NavController,
    vm: WalkWinViewModel = viewModel()
) {
    val uiState by vm.uiState.collectAsState()
    val lastActivation by vm.lastActivationTime.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity

    var showSuccess by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }
    var showConfirm100 by remember { mutableStateOf(false) }

    // 7 days cooldown in ms
    val sevenDaysMs = 7 * 24 * 60 * 60 * 1000L
    var now by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            now = System.currentTimeMillis()
        }
    }

    val remainingMs = (lastActivation + sevenDaysMs) - now
    val hasRecentActivation = lastActivation > 0 && remainingMs > 0

    LaunchedEffect(uiState) {
        when (uiState) {
            is WalkUiState.Success -> showSuccess = true
            is WalkUiState.Error   -> {
                errorMsg = (uiState as WalkUiState.Error).message
                showError = true
            }
            else -> {}
        }
    }

    if (showSuccess) {
        SuccessModal(
            message = "تم تفعيل باقة 2 جيجابايت أسبوعية بنجاح! 🥳🎉"
        ) {
            showSuccess = false
            vm.reset()
            activity?.let { AdsManager.showInterstitial(it) }
        }
    }

    if (showError) {
        ErrorModal(errorMsg) {
            showError = false
            vm.reset()
        }
    }

    if (showConfirm100) {
        ConfirmModal(
            title = "تفعيل عرض 100 دج",
            subtitle = "سيتم تفعيل عرض 2 جيجابايت بقيمة 100 دج لتأهيل خطك لباقة 2 جيجابايت الأسبوعية المجانية.",
            onConfirm = {
                showConfirm100 = false
                vm.activate100DzdOffer()
            },
            onDismiss = { showConfirm100 = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("امشِ واربح 2 جيجابايت", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
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
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Big Neon Icon ─────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(Primary.copy(alpha = 0.15f), RoundedCornerShape(48.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.DirectionsRun,
                    null,
                    tint = Primary,
                    modifier = Modifier.size(52.dp)
                )
            }

            Text(
                "2 جيجابايت أسبوعية مجاناً",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.ExtraBold
            )

            // ── Countdown Timer Card ──────────────────────────────────────────
            if (hasRecentActivation) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Primary.copy(alpha = 0.4f))
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
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = Primary
                        )
                    }
                }
            }

            // ── Instructions / Rule Card ──────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Border)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Outlined.Info, null, tint = Primary, modifier = Modifier.size(20.dp))
                        Text(
                            "شروط الاستفادة من الخدمة",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        "للاستفادة من 2 جيجابايت المجانية كل أسبوع، يجب تعبئة رصيد بقيمة 100 دج أو أكثر خلال الشهر وتفعيل عرض، لتفعيل الباقة المجانية مرة واحدة كل 7 أيام.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Start
                    )

                    Box(
                        modifier = Modifier
                            .background(Primary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            "صلاحية الباقة: 7 أيام كاملة",
                            style = MaterialTheme.typography.labelSmall,
                            color = Primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ── Main Action Button ────────────────────────────────────────────
            Button(
                onClick  = { vm.activateWalk2Gb() },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                enabled  = uiState !is WalkUiState.Loading && !hasRecentActivation,
                colors   = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = OnPrimary),
                shape    = RoundedCornerShape(14.dp)
            ) {
                if (uiState is WalkUiState.Loading)
                    CircularProgressIndicator(Modifier.size(22.dp), color = OnPrimary, strokeWidth = 2.dp)
                else
                    Text(
                        if (hasRecentActivation) "تم التفعيل — انتظر انتهاء الأسبوع"
                        else "تفعيل 2 جيجابايت مجاناً الآن",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
            }

            HorizontalDivider(color = Border.copy(alpha = 0.5f))

            // ── 100 DA Shortcut ───────────────────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "لم تقم بتفعيل عرض 100 دج هذا الشهر؟",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                OutlinedButton(
                    onClick  = { showConfirm100 = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    enabled  = uiState !is WalkUiState.Loading,
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = Primary),
                    border   = BorderStroke(1.dp, Primary)
                ) {
                    Text("تفعيل عرض 100 دج الآن", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

private fun formatCountdown(ms: Long): String {
    if (ms <= 0) return "00:00:00"
    val days = ms / (24 * 3600 * 1000)
    val hours = (ms % (24 * 3600 * 1000)) / (3600 * 1000)
    val minutes = (ms % (3600 * 1000)) / (60 * 1000)
    val seconds = (ms % (60 * 1000)) / 1000

    return if (days > 0) {
        "$days يوم و $hours ساعة"
    } else {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }
}
