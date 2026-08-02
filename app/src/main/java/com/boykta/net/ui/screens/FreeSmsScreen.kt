package com.boykta.net.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.boykta.net.ads.AdsManager
import com.boykta.net.data.api.ApiClient
import com.boykta.net.data.local.TokenStorage
import com.boykta.net.data.models.BipSmsRequest
import com.boykta.net.data.models.MgmInviteRequest
import com.boykta.net.ui.components.ErrorModal
import com.boykta.net.ui.components.SuccessModal
import com.boykta.net.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ── Inline ViewModel for this screen ─────────────────────────────────────────

sealed class SmsUiState {
    object Idle     : SmsUiState()
    object Loading  : SmsUiState()
    object Success  : SmsUiState()
    data class Error(val message: String) : SmsUiState()
}

class FreeSmsViewModel(application: android.app.Application) : AndroidViewModel(application) {
    private val tokenStorage = TokenStorage(application)
    private val api = ApiClient.api

    private val _state = MutableStateFlow<SmsUiState>(SmsUiState.Idle)
    val state: StateFlow<SmsUiState> = _state.asStateFlow()

    fun sendBip(phone: String, type: String) = callApi {
        val msisdnB = toMsisdn(phone)
        api.sendBipSms("Bearer $it", tokenStorage.msisdn.firstOrNull() ?: "", BipSmsRequest(msisdnB, type))
    }

    fun sendMgmInvite(phone: String) = callApi {
        val msisdnB = toMsisdn(phone)
        api.sendMgmInvitation("Bearer $it", tokenStorage.msisdn.firstOrNull() ?: "", MgmInviteRequest(msisdnB))
    }

    fun activateMgmReward() = callApi {
        api.activateMgmReward("Bearer $it", tokenStorage.msisdn.firstOrNull() ?: "")
    }

    private fun callApi(block: suspend (String) -> retrofit2.Response<*>) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = SmsUiState.Loading
            val token = tokenStorage.accessToken.firstOrNull() ?: run {
                _state.value = SmsUiState.Error("انتهت الجلسة"); return@launch
            }
            try {
                val resp = block(token)
                if (resp.isSuccessful || resp.code() in 200..201) _state.value = SmsUiState.Success
                else {
                    val ar = Regex(""""ar"\s*:\s*"([^"]+)"""").find(resp.errorBody()?.string() ?: "")?.groupValues?.get(1) ?: ""
                    _state.value = SmsUiState.Error(ar.ifBlank { "فشلت العملية (${resp.code()})" })
                }
            } catch (e: Exception) { _state.value = SmsUiState.Error("تعذّر الاتصال بالخادم.") }
        }
    }

    private fun toMsisdn(phone: String): Long {
        val d = phone.filter { it.isDigit() }
        return (if (d.startsWith("0")) "213${d.drop(1)}" else d).toLong()
    }

    fun reset() { _state.value = SmsUiState.Idle }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreeSmsScreen(navController: NavController, vm: FreeSmsViewModel = viewModel()) {
    val context  = LocalContext.current
    val activity = context as? Activity
    val uiState  by vm.state.collectAsState()

    var selectedTab  by remember { mutableIntStateOf(0) }
    var phone        by remember { mutableStateOf("") }
    var showSuccess  by remember { mutableStateOf(false) }
    var showError    by remember { mutableStateOf(false) }
    var errorMsg     by remember { mutableStateOf("") }

    LaunchedEffect(uiState) {
        when (val s = uiState) {
            is SmsUiState.Success -> showSuccess = true
            is SmsUiState.Error   -> { errorMsg = s.message; showError = true }
            else -> {}
        }
    }

    if (showSuccess) SuccessModal { showSuccess = false; vm.reset(); activity?.let { AdsManager.showInterstitial(it) } }
    if (showError)   ErrorModal(errorMsg) { showError = false; vm.reset() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("الخدمات المجانية", style = MaterialTheme.typography.titleMedium) },
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
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab, containerColor = Background, contentColor = Primary) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) { Text("كلمني / فليكسيلي", Modifier.padding(12.dp)) }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) { Text("دعوات MGM", Modifier.padding(12.dp)) }
            }

            Column(
                Modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                AppTextField(
                    value = phone, onValueChange = { phone = it },
                    label = "رقم المستلم", placeholder = "07XXXXXXXX",
                    keyboardType = KeyboardType.Phone,
                    enabled = uiState !is SmsUiState.Loading
                )

                if (selectedTab == 0) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { vm.sendBip(phone, "CALLME") },
                            modifier = Modifier.weight(1f).height(48.dp),
                            enabled = uiState !is SmsUiState.Loading,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary)
                        ) { Text("كلمني", fontWeight = FontWeight.SemiBold) }

                        Button(
                            onClick = { vm.sendBip(phone, "FLEXYLI") },
                            modifier = Modifier.weight(1f).height(48.dp),
                            enabled = uiState !is SmsUiState.Loading,
                            colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = OnPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) { Text("فليكسيلي", fontWeight = FontWeight.SemiBold) }
                    }
                } else {
                    Button(
                        onClick = { vm.sendMgmInvite(phone) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        enabled = uiState !is SmsUiState.Loading,
                        colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = OnPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (uiState is SmsUiState.Loading) CircularProgressIndicator(Modifier.size(20.dp), color = OnPrimary, strokeWidth = 2.dp)
                        else Text("إرسال دعوة", fontWeight = FontWeight.SemiBold)
                    }

                    Divider(color = Border)

                    Button(
                        onClick = { vm.activateMgmReward() },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        enabled = uiState !is SmsUiState.Loading,
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariant, contentColor = Primary),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("تفعيل مكافأة الدعوة", fontWeight = FontWeight.SemiBold) }
                }

                if (uiState is SmsUiState.Loading) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Primary)
                    }
                }
            }
        }
    }
}
