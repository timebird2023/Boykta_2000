package com.boykta.net.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.core.content.FileProvider
import java.io.File
import com.boykta.net.data.api.ApiClient
import com.boykta.net.data.local.TokenStorage
import com.boykta.net.data.models.NetworkServiceItem
import com.boykta.net.data.models.NetworkServiceRequest
import com.boykta.net.data.models.RanatiActivateBody
import com.boykta.net.data.models.RanatiActivateItem
import com.boykta.net.data.models.RanatiDeleteBody
import com.boykta.net.data.models.RanatiDeleteData
import com.boykta.net.navigation.Screen
import com.boykta.net.ui.components.ConfirmModal
import com.boykta.net.ui.components.ErrorModal
import com.boykta.net.ui.components.SuccessModal
import com.boykta.net.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ── ViewModel ─────────────────────────────────────────────────────────────────

sealed class NetworkUiState {
    object Idle    : NetworkUiState()
    object Loading : NetworkUiState()
    object Success : NetworkUiState()
    data class Error(val message: String) : NetworkUiState()
}

data class NetworkServiceState(
    val appelMasqueEnabled: Boolean? = null,
    val callWaitEnabled: Boolean?    = null,
    val ranatiActive: Boolean?       = null,
    val isLoading: Boolean           = false
)

class NetworkServicesViewModel(application: android.app.Application) : AndroidViewModel(application) {
    private val tokenStorage = TokenStorage(application)
    private val api = ApiClient.api

    private val _state = MutableStateFlow<NetworkUiState>(NetworkUiState.Idle)
    val state: StateFlow<NetworkUiState> = _state.asStateFlow()

    private val _svcState = MutableStateFlow(NetworkServiceState())
    val svcState: StateFlow<NetworkServiceState> = _svcState.asStateFlow()

    init { loadNetworkServicesFromApi() }

    fun loadNetworkServicesFromApi() {
        viewModelScope.launch(Dispatchers.IO) {
            _svcState.update { it.copy(isLoading = true) }
            val token  = tokenStorage.accessToken.firstOrNull() ?: run { loadStoredStates(); return@launch }
            val msisdn = tokenStorage.msisdn.firstOrNull() ?: run { loadStoredStates(); return@launch }
            val auth   = "Bearer $token"

            // ── Network services (APPELMASQUE / CALLWAIT) ─────────────────────
            // API returns: { "data": [ {"id":"APPELMASQUE","isActive":false}, ... ] }
            try {
                val resp = api.getNetworkServices(auth, msisdn)
                if (resp.isSuccessful) {
                    val items = resp.body()?.data ?: emptyList()
                    var appelMasque: Boolean? = null
                    var callWait: Boolean? = null
                    for (item in items) {
                        val active = item.isActive == true
                        when (item.id?.uppercase()) {
                            "APPELMASQUE" -> appelMasque = active
                            "CALLWAIT"    -> callWait    = active
                        }
                    }
                    if (appelMasque != null) tokenStorage.setNetworkServiceState("APPELMASQUE", appelMasque)
                    if (callWait    != null) tokenStorage.setNetworkServiceState("CALLWAIT",    callWait)
                    _svcState.update { it.copy(appelMasqueEnabled = appelMasque, callWaitEnabled = callWait) }
                }
            } catch (_: Exception) { loadStoredStates() }

            // ── Ranati subscription state ─────────────────────────────────────
            // Confirmed path: data.relationships.rbt-subscriptions.data
            //   [] = not subscribed, [...] = subscribed
            try {
                val ranatiResp = api.checkRanatiSubscription(auth, msisdn)
                val ranatiActive = when {
                    ranatiResp.code() == 404 -> false
                    ranatiResp.isSuccessful  -> {
                        val rbtData = ranatiResp.body()
                            ?.data
                            ?.relationships
                            ?.rbtSubscriptions
                            ?.data
                        rbtData != null && rbtData.isNotEmpty()
                    }
                    else -> null
                }
                if (ranatiActive != null) _svcState.update { it.copy(ranatiActive = ranatiActive) }
            } catch (_: Exception) { }

            _svcState.update { it.copy(isLoading = false) }
        }
    }

    private fun loadStoredStates() {
        viewModelScope.launch {
            val stored = tokenStorage.getNetworkServiceStates()
            _svcState.update {
                it.copy(
                    appelMasqueEnabled = stored["APPELMASQUE"],
                    callWaitEnabled    = stored["CALLWAIT"],
                    isLoading          = false
                )
            }
        }
    }

    fun toggleService(serviceId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = NetworkUiState.Loading
            val token  = tokenStorage.accessToken.firstOrNull()
                ?: run { _state.value = NetworkUiState.Error("انتهت الجلسة"); return@launch }
            val msisdn = tokenStorage.msisdn.firstOrNull()
                ?: run { _state.value = NetworkUiState.Error("انتهت الجلسة"); return@launch }

            val currentEnabled = when (serviceId) {
                "APPELMASQUE" -> _svcState.value.appelMasqueEnabled
                "CALLWAIT"    -> _svcState.value.callWaitEnabled
                else          -> null
            }
            val newActivate = currentEnabled != true

            try {
                val resp = api.toggleNetworkService(
                    "Bearer $token", msisdn,
                    NetworkServiceRequest(code = serviceId, activate = newActivate)
                )
                if (resp.isSuccessful || resp.code() in 200..201) {
                    tokenStorage.setNetworkServiceState(serviceId, newActivate)
                    when (serviceId) {
                        "APPELMASQUE" -> _svcState.update { it.copy(appelMasqueEnabled = newActivate) }
                        "CALLWAIT"    -> _svcState.update { it.copy(callWaitEnabled    = newActivate) }
                    }
                    _state.value = NetworkUiState.Success
                } else {
                    if (newActivate) {
                        val resp2 = api.toggleNetworkService(
                            "Bearer $token", msisdn,
                            NetworkServiceRequest(code = serviceId, activate = false)
                        )
                        if (resp2.isSuccessful || resp2.code() in 200..201) {
                            tokenStorage.setNetworkServiceState(serviceId, false)
                            when (serviceId) {
                                "APPELMASQUE" -> _svcState.update { it.copy(appelMasqueEnabled = false) }
                                "CALLWAIT"    -> _svcState.update { it.copy(callWaitEnabled    = false) }
                            }
                            _state.value = NetworkUiState.Success
                            return@launch
                        }
                    }
                    val ar = Regex(""""ar"\s*:\s*"([^"]+)"""").find(resp.errorBody()?.string() ?: "")?.groupValues?.get(1) ?: ""
                    _state.value = NetworkUiState.Error(ar.ifBlank { "فشلت العملية (${resp.code()})" })
                }
            } catch (e: Exception) { _state.value = NetworkUiState.Error("تعذّر الاتصال بالخادم.") }
        }
    }

    fun enableRanati() {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = NetworkUiState.Loading
            val token  = tokenStorage.accessToken.firstOrNull()
                ?: run { _state.value = NetworkUiState.Error("انتهت الجلسة"); return@launch }
            val msisdn = tokenStorage.msisdn.firstOrNull()
                ?: run { _state.value = NetworkUiState.Error("انتهت الجلسة"); return@launch }
            try {
                val resp = api.subscribeRanati(
                    "Bearer $token", msisdn,
                    RanatiActivateBody(listOf(RanatiActivateItem(id = msisdn)))
                )
                if (resp.isSuccessful || resp.code() in 200..204) {
                    _svcState.update { it.copy(ranatiActive = true) }
                    _state.value = NetworkUiState.Success
                } else {
                    val ar = Regex(""""ar"\s*:\s*"([^"]+)"""").find(resp.errorBody()?.string() ?: "")?.groupValues?.get(1) ?: ""
                    _state.value = NetworkUiState.Error(ar.ifBlank { "فشل تفعيل رناتي (${resp.code()})" })
                }
            } catch (e: Exception) { _state.value = NetworkUiState.Error("تعذّر الاتصال بالخادم.") }
        }
    }

    fun disableRanati() {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = NetworkUiState.Loading
            val token  = tokenStorage.accessToken.firstOrNull()
                ?: run { _state.value = NetworkUiState.Error("انتهت الجلسة"); return@launch }
            val msisdn = tokenStorage.msisdn.firstOrNull()
                ?: run { _state.value = NetworkUiState.Error("انتهت الجلسة"); return@launch }
            try {
                val resp = api.deleteRanati(
                    "Bearer $token", msisdn,
                    RanatiDeleteBody(RanatiDeleteData(id = msisdn))
                )
                if (resp.isSuccessful || resp.code() in 200..204) {
                    _svcState.update { it.copy(ranatiActive = false) }
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
    val context      = LocalContext.current
    val tokenStorage = remember { TokenStorage(context) }
    val scope        = rememberCoroutineScope()
    val netState     by netVm.state.collectAsState()
    val svcState     by netVm.svcState.collectAsState()

    var showLogoutConfirm        by remember { mutableStateOf(false) }
    var showRanatiDisableConfirm by remember { mutableStateOf(false) }
    var showRanatiEnableConfirm  by remember { mutableStateOf(false) }
    var showSuccess              by remember { mutableStateOf(false) }
    var showError                by remember { mutableStateOf(false) }
    var errorMsg                 by remember { mutableStateOf("") }

    val accountName  by tokenStorage.accountName.collectAsState(initial = "")
    val phoneDisplay by tokenStorage.phoneDisplay.collectAsState(initial = "")
    val isDarkTheme  by tokenStorage.isDarkTheme.collectAsState(initial = true)

    LaunchedEffect(netState) {
        when (val s = netState) {
            is NetworkUiState.Success -> showSuccess = true
            is NetworkUiState.Error   -> { errorMsg = s.message; showError = true }
            else -> {}
        }
    }

    if (showSuccess) SuccessModal { showSuccess = false; netVm.reset() }
    if (showError)   ErrorModal(errorMsg) { showError = false; netVm.reset() }

    if (showLogoutConfirm) {
        ConfirmModal(
            title    = "تسجيل الخروج",
            subtitle = "هل تريد حذف هذا الرقم وتسجيل الخروج؟",
            onConfirm = {
                scope.launch {
                    tokenStorage.removeAccount(tokenStorage.msisdn.firstOrNull() ?: "")
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                }
            },
            onDismiss = { showLogoutConfirm = false }
        )
    }

    if (showRanatiDisableConfirm) {
        ConfirmModal(
            title    = "إلغاء رناتي",
            subtitle = "هل أنت متأكد من إلغاء اشتراك رناتي؟ ستتوقف نغمة الرد الخاصة بك.",
            onConfirm = { showRanatiDisableConfirm = false; netVm.disableRanati() },
            onDismiss = { showRanatiDisableConfirm = false }
        )
    }

    if (showRanatiEnableConfirm) {
        ConfirmModal(
            title    = "تفعيل رناتي",
            subtitle = "هل تريد تفعيل خدمة رناتي (نغمة الرد)؟",
            onConfirm = { showRanatiEnableConfirm = false; netVm.enableRanati() },
            onDismiss = { showRanatiEnableConfirm = false }
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
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ── Account card ──────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors   = CardDefaults.cardColors(containerColor = CardBg),
                shape    = RoundedCornerShape(14.dp),
                border   = BorderStroke(1.dp, Border)
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
                        Icon(Icons.Outlined.Person, null, tint = Primary)
                    }
                    Column {
                        Text(
                            accountName ?: "—",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(phoneDisplay ?: "—", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // ── Network services ──────────────────────────────────────────────
            SectionHeader("خدمات الشبكة")

            SmartToggleItem(
                icon      = Icons.Outlined.VisibilityOff,
                label     = "إخفاء رقمك",
                subtitle  = "APPELMASQUE — يخفي رقمك عند الاتصال",
                enabled   = svcState.appelMasqueEnabled,
                isLoading = svcState.isLoading || netState is NetworkUiState.Loading,
                onClick   = { netVm.toggleService("APPELMASQUE") }
            )

            SmartToggleItem(
                icon      = Icons.Outlined.CallSplit,
                label     = "الانتظار المزدوج",
                subtitle  = "CALLWAIT — الرد على مكالمة أثناء أخرى",
                enabled   = svcState.callWaitEnabled,
                isLoading = svcState.isLoading || netState is NetworkUiState.Loading,
                onClick   = { netVm.toggleService("CALLWAIT") }
            )

            // ── Ranati card ────────────────────────────────────────────────────
            RanatiCard(
                ranatiActive = svcState.ranatiActive,
                isLoading    = svcState.isLoading || netState is NetworkUiState.Loading,
                onEnable     = { showRanatiEnableConfirm  = true },
                onDisable    = { showRanatiDisableConfirm = true },
                onRefresh    = { netVm.loadNetworkServicesFromApi() }
            )

            Spacer(Modifier.height(4.dp))

            // ── المظهر ────────────────────────────────────────────────────────
            SectionHeader("المظهر")

            SmartToggleItem(
                icon      = if (isDarkTheme) Icons.Outlined.DarkMode else Icons.Outlined.LightMode,
                label     = if (isDarkTheme) "الوضع المظلم" else "الوضع النهاري",
                subtitle  = "اضغط للتبديل بين المظهرين",
                enabled   = isDarkTheme,
                isLoading = false,
                onClick   = { scope.launch { tokenStorage.setDarkTheme(!isDarkTheme) } }
            )

            Spacer(Modifier.height(4.dp))

            SectionHeader("عام")

            SettingsItem(
                icon = Icons.Outlined.Share,
                label = "مشاركة التطبيق",
                subtitle = "مشاركة ملف APK",
                onClick = {
                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            @Suppress("DEPRECATION")
                            val sourceApk = File(
                                context.packageManager
                                    .getPackageInfo(context.packageName, 0)
                                    .applicationInfo.sourceDir
                            )
                            val destFile = File(context.cacheDir, "boykta_net.apk")
                            sourceApk.copyTo(destFile, overwrite = true)
                            val apkUri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                destFile
                            )
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/vnd.android.package-archive"
                                putExtra(Intent.EXTRA_STREAM, apkUri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "مشاركة التطبيق"))
                        } catch (_: Exception) {
                            val fallback = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "تطبيق boykta net — خدمات جيزي بسهولة\nhttps://www.facebook.com/boyktanet")
                            }
                            context.startActivity(Intent.createChooser(fallback, "مشاركة"))
                        }
                    }
                }
            )

            SettingsItem(
                icon = Icons.Outlined.Policy,
                label = "سياسة الخصوصية",
                onClick = { navController.navigate(Screen.PrivacyPolicy.route) }
            )

            SettingsItem(
                icon = Icons.Outlined.Code,
                label = "المطور",
                subtitle = "facebook.com/boyktanet",
                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/boyktanet"))) }
            )

            Spacer(Modifier.height(4.dp))
            SectionHeader("الحساب")

            SettingsItem(
                icon = Icons.Outlined.Logout,
                label = "تسجيل الخروج / حذف الرقم",
                labelColor = Error,
                onClick = { showLogoutConfirm = true }
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Section header ────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelMedium,
        color = TextSecondary,
        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
    )
}

// ── Ranati card (full redesign) ───────────────────────────────────────────────

@Composable
private fun RanatiCard(
    ranatiActive: Boolean?,
    isLoading: Boolean,
    onEnable: () -> Unit,
    onDisable: () -> Unit,
    onRefresh: () -> Unit
) {
    val isActive = ranatiActive == true

    // Pulsing glow animation when active
    val infiniteTransition = rememberInfiniteTransition(label = "ranati_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )

    val borderColor = when {
        isLoading -> Border
        isActive  -> Primary.copy(alpha = 0.6f)
        else      -> Border
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(
            containerColor = when {
                isActive -> Primary.copy(alpha = 0.07f)
                ranatiActive == false -> Accent.copy(alpha = 0.04f)
                else -> CardBg
            }
        ),
        shape  = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // ── Header row ────────────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (isActive) Primary.copy(alpha = 0.18f) else Accent.copy(alpha = 0.12f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isActive) Icons.Outlined.MusicNote else Icons.Outlined.MusicOff,
                        contentDescription = null,
                        tint = if (isActive) Primary else Accent,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(Modifier.weight(1f)) {
                    Text(
                        "رناتي (نغمة الرد)",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        when (ranatiActive) {
                            true  -> "مشترك في رناتي — نغمة الرد مفعّلة"
                            false -> "غير مشترك — اضغط لتفعيل الخدمة"
                            null  -> "جارٍ التحقق من الحالة..."
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = when (ranatiActive) {
                            true  -> Primary.copy(alpha = 0.8f)
                            false -> Accent.copy(alpha = 0.8f)
                            null  -> TextSecondary
                        }
                    )
                }

                // Status badge / loader
                when {
                    isLoading || ranatiActive == null ->
                        CircularProgressIndicator(Modifier.size(20.dp), color = Primary, strokeWidth = 2.dp)
                    isActive ->
                        Box(
                            modifier = Modifier
                                .background(Success.copy(alpha = pulseAlpha * 0.18f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(Modifier.size(6.dp).background(Success.copy(alpha = pulseAlpha), CircleShape))
                                Text("مفعّل", style = MaterialTheme.typography.labelSmall, color = Success, fontWeight = FontWeight.Bold)
                            }
                        }
                    else ->
                        Box(
                            modifier = Modifier
                                .background(Accent.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("معطّل", style = MaterialTheme.typography.labelSmall, color = Accent, fontWeight = FontWeight.Bold)
                        }
                }
            }

            // ── Action buttons ────────────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                // Refresh (always shown)
                OutlinedButton(
                    onClick  = onRefresh,
                    enabled  = !isLoading,
                    modifier = Modifier.weight(1f).height(38.dp),
                    shape    = RoundedCornerShape(10.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    border   = BorderStroke(1.dp, Border)
                ) {
                    Icon(Icons.Outlined.Refresh, null, Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("تحديث", style = MaterialTheme.typography.labelMedium)
                }

                when {
                    ranatiActive == null -> { /* unknown state — refresh only */ }
                    isActive ->
                        // Cancel / disable
                        Button(
                            onClick  = onDisable,
                            enabled  = !isLoading,
                            modifier = Modifier.weight(2f).height(38.dp),
                            shape    = RoundedCornerShape(10.dp),
                            colors   = ButtonDefaults.buttonColors(
                                containerColor = Accent.copy(alpha = 0.85f),
                                contentColor   = Color.White
                            )
                        ) {
                            Icon(Icons.Outlined.Cancel, null, Modifier.size(15.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("إلغاء الاشتراك", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        }
                    else ->
                        // Activate
                        Button(
                            onClick  = onEnable,
                            enabled  = !isLoading,
                            modifier = Modifier.weight(2f).height(38.dp),
                            shape    = RoundedCornerShape(10.dp),
                            colors   = ButtonDefaults.buttonColors(
                                containerColor = Primary.copy(alpha = 0.9f),
                                contentColor   = Color.Black
                            )
                        ) {
                            Icon(Icons.Outlined.MusicNote, null, Modifier.size(15.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("تفعيل رناتي", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        }
                }
            }
        }
    }
}

// ── Smart toggle item ─────────────────────────────────────────────────────────

@Composable
private fun SmartToggleItem(
    icon: ImageVector,
    label: String,
    subtitle: String,
    enabled: Boolean?,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (enabled == true) Primary.copy(alpha = 0.08f) else SurfaceVariant
            )
            .border(
                1.dp,
                if (enabled == true) Primary.copy(alpha = 0.4f) else Border,
                RoundedCornerShape(12.dp)
            )
            .clickable(enabled = !isLoading, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    if (enabled == true) Primary.copy(alpha = 0.15f) else SurfaceVariant,
                    RoundedCornerShape(18.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon, contentDescription = null,
                tint = if (enabled == true) Primary else TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
        Column(Modifier.weight(1f)) {
            Text(label,    style = MaterialTheme.typography.bodyLarge,  color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        }
        when {
            isLoading -> CircularProgressIndicator(Modifier.size(18.dp), color = Primary, strokeWidth = 2.dp)
            else -> {
                val stateText = when (enabled) { true -> "مفعّل"; false -> "معطّل"; null -> "؟" }
                val stateColor = when (enabled) {
                    true  -> Success
                    false -> Error.copy(alpha = 0.8f)
                    null  -> TextHint
                }
                Box(
                    modifier = Modifier
                        .background(stateColor.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(stateText, style = MaterialTheme.typography.labelSmall, color = stateColor, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ── Generic settings item ─────────────────────────────────────────────────────

@Composable
private fun SettingsItem(
    icon: ImageVector,
    label: String,
    subtitle: String? = null,
    labelColor: Color = Color.Unspecified,
    isLoading: Boolean = false,
    onClick: () -> Unit
) {
    val isDefault = labelColor == Color.Unspecified
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
        Icon(icon, null, tint = if (isDefault) TextSecondary else labelColor, modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = if (isDefault) TextPrimary else labelColor)
            if (subtitle != null)
                Text(subtitle, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        }
        if (isLoading)
            CircularProgressIndicator(Modifier.size(18.dp), color = Primary, strokeWidth = 2.dp)
        else
            Icon(Icons.Outlined.ChevronRight, null, tint = TextHint, modifier = Modifier.size(18.dp))
    }
}
