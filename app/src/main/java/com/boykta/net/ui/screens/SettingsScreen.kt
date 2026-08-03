package com.boykta.net.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.boykta.net.data.api.ApiClient
import com.boykta.net.data.local.TokenStorage
import com.boykta.net.data.models.NetworkServiceRequest
import com.boykta.net.data.models.RanatiDeleteBody
import com.boykta.net.data.models.RanatiDeleteData
import com.boykta.net.navigation.Screen
import com.boykta.net.ui.components.ErrorModal
import com.boykta.net.ui.components.SuccessModal
import com.boykta.net.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ── Inline ViewModel for network services and Ranati ─────────────────────────

sealed class NetworkUiState {
    object Idle    : NetworkUiState()
    object Loading : NetworkUiState()
    object Success : NetworkUiState()
    data class Error(val message: String) : NetworkUiState()
}

class NetworkServicesViewModel(application: android.app.Application) : AndroidViewModel(application) {
    private val tokenStorage = TokenStorage(application)
    private val api = ApiClient.api

    private val _state = MutableStateFlow<NetworkUiState>(NetworkUiState.Idle)
    val state: StateFlow<NetworkUiState> = _state.asStateFlow()

    /**
     * Toggle a network service.
     * serviceId: "APPELMASQUE" | "CALLWAIT"
     * action:    "ACTIVATE" | "DEACTIVATE"
     */
    fun toggleService(serviceId: String, action: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = NetworkUiState.Loading
            val token  = tokenStorage.accessToken.firstOrNull() ?: run { _state.value = NetworkUiState.Error("انتهت الجلسة"); return@launch }
            val msisdn = tokenStorage.msisdn.firstOrNull() ?: run { _state.value = NetworkUiState.Error("انتهت الجلسة"); return@launch }
            try {
                val resp = api.toggleNetworkService(
                    "Bearer $token", msisdn,
                    NetworkServiceRequest(serviceId = serviceId, action = action)
                )
                if (resp.isSuccessful || resp.code() in 200..201) {
                    _state.value = NetworkUiState.Success
                } else {
                    val ar = Regex(""""ar"\s*:\s*"([^"]+)"""").find(resp.errorBody()?.string() ?: "")?.groupValues?.get(1) ?: ""
                    _state.value = NetworkUiState.Error(ar.ifBlank { "فشلت العملية (${resp.code()})" })
                }
            } catch (e: Exception) { _state.value = NetworkUiState.Error("تعذّر الاتصال بالخادم.") }
        }
    }

    /**
     * Disable the Ranati (RBT) ring-back tone subscription.
     * Step 1: GET to check subscription. Step 2: DELETE to unsubscribe.
     */
    fun disableRanati() {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = NetworkUiState.Loading
            val token  = tokenStorage.accessToken.firstOrNull() ?: run { _state.value = NetworkUiState.Error("انتهت الجلسة"); return@launch }
            val msisdn = tokenStorage.msisdn.firstOrNull() ?: run { _state.value = NetworkUiState.Error("انتهت الجلسة"); return@launch }
            try {
                // Check subscription first
                api.checkRanatiSubscription("Bearer $token", msisdn)
                // Then delete
                val resp = api.deleteRanati(
                    "Bearer $token", msisdn,
                    RanatiDeleteBody(RanatiDeleteData(id = msisdn))
                )
                if (resp.isSuccessful || resp.code() in 200..204) {
                    _state.value = NetworkUiState.Success
                } else {
                    val ar = Regex(""""ar"\s*:\s*"([^"]+)"""").find(resp.errorBody()?.string() ?: "")?.groupValues?.get(1) ?: ""
                    _state.value = NetworkUiState.Error(ar.ifBlank { "فشل إلغاء رناتي (${resp.code()})" })
                }
            } catch (e: Exception) { _state.value = NetworkUiState.Error("تعذّر الاتصال بالخادم.") }
        }
    }

    fun reset() { _state.value = NetworkUiState.Idle }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController, netVm: NetworkServicesViewModel = viewModel()) {
    val context = LocalContext.current
    val tokenStorage = remember { TokenStorage(context) }
    val scope = rememberCoroutineScope()
    val netState by netVm.state.collectAsState()

    var showLogoutConfirm  by remember { mutableStateOf(false) }
    var showNetworkDialog  by remember { mutableStateOf(false) }
    var selectedServiceId  by remember { mutableStateOf("") }
    var selectedServiceLbl by remember { mutableStateOf("") }
    var showRanatiConfirm  by remember { mutableStateOf(false) }
    var showSuccess        by remember { mutableStateOf(false) }
    var showError          by remember { mutableStateOf(false) }
    var errorMsg           by remember { mutableStateOf("") }

    val accountName  by tokenStorage.accountName.collectAsState(initial = "")
    val phoneDisplay by tokenStorage.phoneDisplay.collectAsState(initial = "")

    LaunchedEffect(netState) {
        when (val s = netState) {
            is NetworkUiState.Success -> showSuccess = true
            is NetworkUiState.Error   -> { errorMsg = s.message; showError = true }
            else -> {}
        }
    }

    if (showSuccess) SuccessModal { showSuccess = false; netVm.reset() }
    if (showError)   ErrorModal(errorMsg) { showError = false; netVm.reset() }

    // ── Logout confirm ──────────────────────────────────────────────────────
    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("تسجيل الخروج", color = TextPrimary) },
            text  = { Text("هل تريد حذف هذا الرقم وتسجيل الخروج؟", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        tokenStorage.clearToken()
                        navController.navigate(Screen.Auth.route) {
                            popUpTo(Screen.Dashboard.route) { inclusive = true }
                        }
                    }
                }) { Text("نعم، خروج", color = Error) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) { Text("إلغاء", color = TextSecondary) }
            },
            containerColor = CardBg
        )
    }

    // ── Network service toggle dialog ────────────────────────────────────────
    if (showNetworkDialog) {
        AlertDialog(
            onDismissRequest = { showNetworkDialog = false },
            title = { Text(selectedServiceLbl, color = TextPrimary) },
            text  = { Text("اختر الإجراء المطلوب:", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { showNetworkDialog = false; netVm.toggleService(selectedServiceId, "ACTIVATE") }) {
                    Text("تفعيل", color = Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNetworkDialog = false; netVm.toggleService(selectedServiceId, "DEACTIVATE") }) {
                    Text("تعطيل", color = Error)
                }
            },
            containerColor = CardBg
        )
    }

    // ── Ranati disable confirm ────────────────────────────────────────────────
    if (showRanatiConfirm) {
        AlertDialog(
            onDismissRequest = { showRanatiConfirm = false },
            title = { Text("تعطيل رناتي", color = TextPrimary) },
            text  = { Text("هل أنت متأكد من إلغاء اشتراك رناتي؟ ستتوقف نغمة الرد الخاصة بك.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { showRanatiConfirm = false; netVm.disableRanati() }) {
                    Text("نعم، إلغاء الاشتراك", color = Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRanatiConfirm = false }) { Text("إلغاء", color = TextSecondary) }
            },
            containerColor = CardBg
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("الإعدادات", style = MaterialTheme.typography.titleMedium) },
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
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ── Account card ──────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Border)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier.size(44.dp)
                            .background(Primary.copy(alpha = 0.15f), RoundedCornerShape(22.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Person, contentDescription = null, tint = Primary)
                    }
                    Column {
                        Text(accountName ?: "—", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        Text(phoneDisplay ?: "—", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // ── Network services ──────────────────────────────────────────
            Text("خدمات الشبكة", style = MaterialTheme.typography.labelMedium, color = TextSecondary)

            SettingsItem(
                icon = Icons.Outlined.VisibilityOff,
                label = "إخفاء رقمك (APPELMASQUE)",
                subtitle = "إخفاء رقمك عند الاتصال",
                isLoading = netState is NetworkUiState.Loading,
                onClick = {
                    selectedServiceId  = "APPELMASQUE"
                    selectedServiceLbl = "إخفاء رقمك"
                    showNetworkDialog  = true
                }
            )

            SettingsItem(
                icon = Icons.Outlined.CallSplit,
                label = "الانتظار المزدوج (CALLWAIT)",
                subtitle = "الرد على مكالمة أثناء مكالمة أخرى",
                isLoading = netState is NetworkUiState.Loading,
                onClick = {
                    selectedServiceId  = "CALLWAIT"
                    selectedServiceLbl = "الانتظار المزدوج"
                    showNetworkDialog  = true
                }
            )

            SettingsItem(
                icon = Icons.Outlined.MusicOff,
                label = "تعطيل رناتي (RBT)",
                subtitle = "إلغاء نغمة الرد المخصصة",
                isLoading = netState is NetworkUiState.Loading,
                labelColor = Error.copy(alpha = 0.8f),
                onClick = { showRanatiConfirm = true }
            )

            Spacer(Modifier.height(4.dp))

            // ── General ───────────────────────────────────────────────────
            Text("عام", style = MaterialTheme.typography.labelMedium, color = TextSecondary)

            SettingsItem(
                icon = Icons.Outlined.Share,
                label = "مشاركة التطبيق",
                onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "تطبيق boykta net — خدمات جيزي بسهولة\nhttps://www.facebook.com/boyktanet")
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "مشاركة"))
                }
            )

            SettingsItem(
                icon = Icons.Outlined.Policy,
                label = "سياسة الخصوصية",
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/boyktanet")))
                }
            )

            SettingsItem(
                icon = Icons.Outlined.Code,
                label = "المطور",
                subtitle = "facebook.com/boyktanet",
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/boyktanet")))
                }
            )

            Spacer(Modifier.height(4.dp))
            Text("الحساب", style = MaterialTheme.typography.labelMedium, color = TextSecondary)

            SettingsItem(
                icon = Icons.Outlined.Logout,
                label = "تسجيل الخروج / حذف الرقم",
                labelColor = Error,
                onClick = { showLogoutConfirm = true }
            )
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    label: String,
    subtitle: String? = null,
    labelColor: androidx.compose.ui.graphics.Color = TextPrimary,
    isLoading: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceVariant)
            .clickable(enabled = !isLoading, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (labelColor == TextPrimary) TextSecondary else labelColor,
            modifier = Modifier.size(22.dp)
        )
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = labelColor)
            if (subtitle != null)
                Text(subtitle, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        }
        if (isLoading)
            CircularProgressIndicator(Modifier.size(18.dp), color = Primary, strokeWidth = 2.dp)
        else
            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = TextHint, modifier = Modifier.size(18.dp))
    }
}
