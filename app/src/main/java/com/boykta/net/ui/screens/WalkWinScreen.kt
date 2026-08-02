package com.boykta.net.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.boykta.net.ads.AdsManager
import com.boykta.net.data.api.ApiClient
import com.boykta.net.data.local.TokenStorage
import com.boykta.net.data.models.ActivateProductRequest
import com.boykta.net.ui.components.ErrorModal
import com.boykta.net.ui.components.SuccessModal
import com.boykta.net.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ── Inline ViewModel ──────────────────────────────────────────────────────────

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

    fun activateWalk2Gb() {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = WalkUiState.Loading
            val token  = tokenStorage.accessToken.firstOrNull() ?: run { _state.value = WalkUiState.Error("انتهت الجلسة"); return@launch }
            val msisdn = tokenStorage.msisdn.firstOrNull() ?: return@launch
            val auth = "Bearer $token"
            try {
                // Check campaign eligibility first
                api.checkWalkCampaign(auth, msisdn)
                // Activate
                val resp = api.activateWalkReward(auth, msisdn, ActivateProductRequest("GIFTWALKWIN2GO"))
                if (resp.isSuccessful || resp.code() in 200..201) {
                    _state.value = WalkUiState.Success
                } else {
                    val ar = Regex(""""ar"\s*:\s*"([^"]+)"""").find(resp.errorBody()?.string() ?: "")?.groupValues?.get(1) ?: ""
                    _state.value = WalkUiState.Error(ar.ifBlank { "فشل التفعيل (${resp.code()})." })
                }
            } catch (e: Exception) { _state.value = WalkUiState.Error("تعذّر الاتصال بالخادم.") }
        }
    }

    fun reset() { _state.value = WalkUiState.Idle }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalkWinScreen(navController: NavController, vm: WalkWinViewModel = viewModel()) {
    val context  = LocalContext.current
    val activity = context as? Activity
    val uiState  by vm.state.collectAsState()

    var showSuccess by remember { mutableStateOf(false) }
    var showError   by remember { mutableStateOf(false) }
    var errorMsg    by remember { mutableStateOf("") }

    LaunchedEffect(uiState) {
        when (val s = uiState) {
            is WalkUiState.Success -> showSuccess = true
            is WalkUiState.Error   -> { errorMsg = s.message; showError = true }
            else -> {}
        }
    }

    if (showSuccess) SuccessModal { showSuccess = false; vm.reset(); activity?.let { AdsManager.showInterstitial(it) } }
    if (showError)   ErrorModal(errorMsg) { showError = false; vm.reset() }

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
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Primary.copy(alpha = 0.12f), RoundedCornerShape(50.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.DirectionsRun, contentDescription = null, tint = Primary, modifier = Modifier.size(52.dp))
            }

            Text(
                text = "2 جيجابايت أسبوعية مجاناً",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Border)
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "كيف تعمل هذه الخدمة؟",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "للاستفادة من 2 جيجابايت الأسبوعية المجانية، يجب عليك أولاً تفعيل عرض بقيمة 100 دج أو أكثر خلال الشهر الحالي. بعد ذلك اضغط على الزر أدناه لتفعيل مكافأتك.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "ملاحظة: ستتلقى إشعاراً بعد 7 أيام لتذكيرك بإعادة التفعيل.",
                        style = MaterialTheme.typography.labelMedium,
                        color = Primary.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { vm.activateWalk2Gb() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = uiState !is WalkUiState.Loading,
                colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = OnPrimary),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (uiState is WalkUiState.Loading)
                    CircularProgressIndicator(Modifier.size(22.dp), color = OnPrimary, strokeWidth = 2.dp)
                else
                    Text("تفعيل 2 جيجابايت الآن", fontWeight = FontWeight.Bold)
            }
        }
    }
}
