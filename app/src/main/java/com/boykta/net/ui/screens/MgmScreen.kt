package com.boykta.net.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
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
import com.boykta.net.data.models.MgmInviteRequest
import com.boykta.net.ui.components.ErrorModal
import com.boykta.net.ui.components.SuccessModal
import com.boykta.net.ui.theme.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ── Data classes ─────────────────────────────────────────────────────────────

data class MgmInvite(
    val phone: String,
    val accepted: Boolean = false,
    val sentAt: Long = System.currentTimeMillis()
)

// ── Inline ViewModel ──────────────────────────────────────────────────────────

sealed class MgmUiState {
    object Idle    : MgmUiState()
    object Loading : MgmUiState()
    object Success : MgmUiState()
    data class Error(val message: String) : MgmUiState()
}

class MgmViewModel(application: android.app.Application) : AndroidViewModel(application) {
    private val tokenStorage = TokenStorage(application)
    private val api = ApiClient.api
    private val gson = Gson()

    private val _state = MutableStateFlow<MgmUiState>(MgmUiState.Idle)
    val state: StateFlow<MgmUiState> = _state.asStateFlow()

    private val _invites = MutableStateFlow<List<MgmInvite>>(emptyList())
    val invites: StateFlow<List<MgmInvite>> = _invites.asStateFlow()

    /** Count of accepted invites. */
    val successfulCount: Int get() = _invites.value.count { it.accepted }

    init { loadInvitesFromApi() }

    /**
     * Fetches the real invitation list from the server.
     * Falls back to locally-stored invitations if the API call fails.
     */
    fun loadInvitesFromApi() {
        viewModelScope.launch(Dispatchers.IO) {
            val token  = tokenStorage.accessToken.firstOrNull() ?: return@launch
            val msisdn = tokenStorage.msisdn.firstOrNull() ?: return@launch
            try {
                val resp = api.getMgmInvitations("Bearer $token", msisdn)
                if (resp.isSuccessful) {
                    val serverItems = resp.body()?.data?.invitations ?: emptyList()
                    val mapped = serverItems.map { item ->
                        MgmInvite(
                            phone    = formatMsisdn(item.msisdnReceiver ?: ""),
                            accepted = item.status?.uppercase() == "DONE",
                            sentAt   = 0L
                        )
                    }
                    saveInvites(mapped)
                    return@launch
                }
            } catch (_: Exception) { }
            // Fallback: load from DataStore
            loadInvitesLocal()
        }
    }

    private fun loadInvitesLocal() {
        viewModelScope.launch {
            tokenStorage.mgmInvitesJson.firstOrNull()?.let { json ->
                try {
                    val type = object : TypeToken<List<MgmInvite>>() {}.type
                    _invites.value = gson.fromJson(json, type) ?: emptyList()
                } catch (_: Exception) { }
            }
        }
    }

    /** Convert 213xxxxxxxxx → 0xxxxxxxxx for display */
    private fun formatMsisdn(msisdn: String): String =
        if (msisdn.startsWith("213")) "0${msisdn.removePrefix("213")}" else msisdn

    private suspend fun saveInvites(list: List<MgmInvite>) {
        _invites.value = list
        tokenStorage.saveMgmInvites(gson.toJson(list))
    }

    fun sendInvite(phone: String) {
        if (phone.filter { it.isDigit() }.length < 9) {
            _state.value = MgmUiState.Error("رقم الهاتف غير صحيح"); return
        }
        val msisdnB = normalizeToLong(phone) ?: run {
            _state.value = MgmUiState.Error("رقم المستلم غير صحيح"); return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = MgmUiState.Loading
            val token  = tokenStorage.accessToken.firstOrNull() ?: run { _state.value = MgmUiState.Error("انتهت الجلسة"); return@launch }
            val msisdn = tokenStorage.msisdn.firstOrNull() ?: run { _state.value = MgmUiState.Error("انتهت الجلسة"); return@launch }
            try {
                val resp = api.sendMgmInvitation("Bearer $token", msisdn, MgmInviteRequest(msisdnB))
                if (resp.isSuccessful || resp.code() in 200..201) {
                    // Add to local list (not accepted yet)
                    val current = _invites.value.toMutableList()
                    val existing = current.indexOfFirst { it.phone == phone }
                    if (existing >= 0) {
                        current[existing] = current[existing].copy(sentAt = System.currentTimeMillis())
                    } else {
                        current.add(MgmInvite(phone = phone, accepted = false))
                    }
                    saveInvites(current)
                    _state.value = MgmUiState.Success
                } else {
                    val ar = Regex(""""ar"\s*:\s*"([^"]+)"""").find(resp.errorBody()?.string() ?: "")?.groupValues?.get(1) ?: ""
                    _state.value = MgmUiState.Error(ar.ifBlank { "فشل إرسال الدعوة (${resp.code()})" })
                }
            } catch (e: Exception) { _state.value = MgmUiState.Error("تعذّر الاتصال بالخادم.") }
        }
    }

    fun activateReward() {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = MgmUiState.Loading
            val token  = tokenStorage.accessToken.firstOrNull() ?: run { _state.value = MgmUiState.Error("انتهت الجلسة"); return@launch }
            val msisdn = tokenStorage.msisdn.firstOrNull() ?: run { _state.value = MgmUiState.Error("انتهت الجلسة"); return@launch }
            try {
                val resp = api.activateMgmReward("Bearer $token", msisdn)
                if (resp.isSuccessful || resp.code() in 200..201) {
                    // Mark first non-accepted invite as accepted
                    val current = _invites.value.toMutableList()
                    val idx = current.indexOfFirst { !it.accepted }
                    if (idx >= 0) {
                        current[idx] = current[idx].copy(accepted = true)
                        saveInvites(current)
                    }
                    _state.value = MgmUiState.Success
                } else {
                    val ar = Regex(""""ar"\s*:\s*"([^"]+)"""").find(resp.errorBody()?.string() ?: "")?.groupValues?.get(1) ?: ""
                    _state.value = MgmUiState.Error(ar.ifBlank { "فشل تفعيل المكافأة (${resp.code()})" })
                }
            } catch (e: Exception) { _state.value = MgmUiState.Error("تعذّر الاتصال بالخادم.") }
        }
    }

    fun removeInvite(phone: String) {
        viewModelScope.launch {
            val updated = _invites.value.filter { it.phone != phone }
            saveInvites(updated)
        }
    }

    private fun normalizeToLong(phone: String): Long? {
        val d = phone.filter { it.isDigit() }
        val msisdn = when {
            d.startsWith("213") -> d
            d.startsWith("0")   -> "213${d.drop(1)}"
            else                 -> "213$d"
        }
        return msisdn.toLongOrNull()
    }

    fun reset() { _state.value = MgmUiState.Idle }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MgmScreen(navController: NavController, vm: MgmViewModel = viewModel()) {
    val context  = LocalContext.current
    val activity = context as? Activity
    val uiState  by vm.state.collectAsState()
    val invites  by vm.invites.collectAsState()

    var phone       by remember { mutableStateOf("") }
    var showSuccess by remember { mutableStateOf(false) }
    var showError   by remember { mutableStateOf(false) }
    var errorMsg    by remember { mutableStateOf("") }

    val successCount = invites.count { it.accepted }
    val canInviteMore = invites.count { !it.accepted } + successCount < 5

    LaunchedEffect(uiState) {
        when (val s = uiState) {
            is MgmUiState.Success -> { showSuccess = true; phone = "" }
            is MgmUiState.Error   -> { errorMsg = s.message; showError = true }
            else -> {}
        }
    }

    if (showSuccess) SuccessModal {
        showSuccess = false
        vm.reset()
        activity?.let { AdsManager.showInterstitial(it) }
    }
    if (showError) ErrorModal(errorMsg) { showError = false; vm.reset() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("دعوات MGM", style = MaterialTheme.typography.titleMedium) },
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
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Info card ─────────────────────────────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Border)
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(44.dp).background(Primary.copy(alpha = 0.15f), RoundedCornerShape(22.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.PersonAdd, null, tint = Primary)
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                "دعوات ناجحة: $successCount / 5",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "ادعُ أصدقاءك للحصول على مكافآت الإنترنت",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // ── Input + actions ───────────────────────────────────────────────
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AppTextField(
                        value         = phone,
                        onValueChange = { phone = it },
                        label         = "رقم الصديق",
                        placeholder   = "07XXXXXXXX",
                        keyboardType  = KeyboardType.Phone,
                        enabled       = uiState !is MgmUiState.Loading
                    )

                    Button(
                        onClick  = { vm.sendInvite(phone) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        enabled  = uiState !is MgmUiState.Loading && canInviteMore,
                        colors   = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = OnPrimary),
                        shape    = RoundedCornerShape(12.dp)
                    ) {
                        if (uiState is MgmUiState.Loading)
                            CircularProgressIndicator(Modifier.size(20.dp), color = OnPrimary, strokeWidth = 2.dp)
                        else {
                            Icon(Icons.Outlined.PersonAdd, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (!canInviteMore) "وصلت للحد الأقصى" else "إرسال دعوة",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    HorizontalDivider(color = Border)

                    OutlinedButton(
                        onClick  = { vm.activateReward() },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        enabled  = uiState !is MgmUiState.Loading,
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = Primary)
                    ) {
                        if (uiState is MgmUiState.Loading)
                            CircularProgressIndicator(Modifier.size(20.dp), color = Primary, strokeWidth = 2.dp)
                        else {
                            Icon(Icons.Outlined.CardGiftcard, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("تفعيل مكافأة الدعوة", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // ── Invites list ──────────────────────────────────────────────────
            if (invites.isNotEmpty()) {
                item {
                    Text(
                        "قائمة الدعوات",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                }

                items(invites) { invite ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceVariant, RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(invite.phone, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    if (invite.accepted) Icons.Outlined.CheckCircle else Icons.Outlined.Schedule,
                                    contentDescription = null,
                                    tint = if (invite.accepted) Success else TextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    if (invite.accepted) "تم قبول الدعوة" else "في الانتظار",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (invite.accepted) Success else TextSecondary
                                )
                            }
                        }

                        if (!invite.accepted) {
                            IconButton(
                                onClick = { vm.removeInvite(invite.phone) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.DeleteOutline,
                                    contentDescription = "حذف",
                                    tint = Error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
