package com.boykta.net.ui.screens

import android.app.Activity
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.boykta.net.data.api.ApiClient
import com.boykta.net.data.local.SavedAccount
import com.boykta.net.data.local.TokenStorage
import com.boykta.net.data.models.*
import com.boykta.net.navigation.Screen
import com.boykta.net.ui.components.ConfirmModal
import com.boykta.net.ui.components.ErrorModal
import com.boykta.net.ui.components.SuccessModal
import com.boykta.net.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ── ViewModel for Settings ───────────────────────────────────────────────────
class SettingsViewModel(application: android.app.Application) : AndroidViewModel(application) {
    private val tokenStorage = TokenStorage(application)
    private val api = ApiClient.api

    val isDarkTheme: StateFlow<Boolean> = tokenStorage.isDarkTheme
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private val _accounts = MutableStateFlow<List<SavedAccount>>(emptyList())
    val accounts: StateFlow<List<SavedAccount>> = _accounts.asStateFlow()

    private val _activeMsisdn = MutableStateFlow("")
    val activeMsisdn: StateFlow<String> = _activeMsisdn.asStateFlow()

    // Network services states
    private val _hideNumber = MutableStateFlow<Boolean?>(null)
    val hideNumber: StateFlow<Boolean?> = _hideNumber.asStateFlow()

    private val _callWait = MutableStateFlow<Boolean?>(null)
    val callWait: StateFlow<Boolean?> = _callWait.asStateFlow()

    private val _ranatiActive = MutableStateFlow<Boolean?>(null)
    val ranatiActive: StateFlow<Boolean?> = _ranatiActive.asStateFlow()

    private val _loadingMap = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val loadingMap: StateFlow<Map<String, Boolean>> = _loadingMap.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _accounts.value = tokenStorage.getAllAccounts()
            _activeMsisdn.value = tokenStorage.msisdn.firstOrNull() ?: ""
            loadNetworkServices()
            checkRanati()
        }
    }

    fun toggleTheme(dark: Boolean) {
        viewModelScope.launch { tokenStorage.setDarkTheme(dark) }
    }

    private fun loadNetworkServices() {
        viewModelScope.launch(Dispatchers.IO) {
            val token = tokenStorage.accessToken.firstOrNull() ?: return@launch
            val msisdn = tokenStorage.msisdn.firstOrNull() ?: return@launch
            try {
                val resp = api.getNetworkServices("Bearer $token", msisdn)
                if (resp.isSuccessful) {
                    val list = resp.body()?.data ?: emptyList()
                    list.forEach { item ->
                        when (item.id) {
                            "APPELMASQUE" -> _hideNumber.value = item.isActive == true
                            "CALLWAIT"    -> _callWait.value    = item.isActive == true
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun toggleService(serviceId: String, currentVal: Boolean?) {
        viewModelScope.launch(Dispatchers.IO) {
            val target = !(currentVal ?: false)
            _loadingMap.update { it + (serviceId to true) }
            val token = tokenStorage.accessToken.firstOrNull() ?: ""
            val msisdn = tokenStorage.msisdn.firstOrNull() ?: ""
            try {
                val resp = api.toggleNetworkService("Bearer $token", msisdn, NetworkServiceRequest(serviceId, target))
                if (resp.isSuccessful) {
                    when (serviceId) {
                        "APPELMASQUE" -> _hideNumber.value = target
                        "CALLWAIT"    -> _callWait.value    = target
                    }
                    _message.value = "تم تحديث الخدمة بنجاح."
                } else {
                    _errorMessage.value = "تعذّر تغيير حالة الخدمة (${resp.code()})."
                }
            } catch (e: Exception) {
                _errorMessage.value = "تعذّر الاتصال بالخادم."
            } finally {
                _loadingMap.update { it - serviceId }
            }
        }
    }

    fun checkRanati() {
        viewModelScope.launch(Dispatchers.IO) {
            val token = tokenStorage.accessToken.firstOrNull() ?: return@launch
            val msisdn = tokenStorage.msisdn.firstOrNull() ?: return@launch
            _loadingMap.update { it + ("RANATI" to true) }
            try {
                val resp = api.checkRanatiSubscription("Bearer $token", msisdn)
                if (resp.isSuccessful) {
                    val subs = resp.body()?.data?.relationships?.rbtSubscriptions?.data
                    _ranatiActive.value = !subs.isNullOrEmpty()
                }
            } catch (_: Exception) {
            } finally {
                _loadingMap.update { it - "RANATI" }
            }
        }
    }

    fun cancelRanati() {
        viewModelScope.launch(Dispatchers.IO) {
            val token = tokenStorage.accessToken.firstOrNull() ?: return@launch
            val msisdn = tokenStorage.msisdn.firstOrNull() ?: return@launch
            _loadingMap.update { it + ("RANATI" to true) }
            try {
                val resp = api.deleteRanati("Bearer $token", msisdn, RanatiDeleteBody(RanatiDeleteData(id = msisdn)))
                if (resp.isSuccessful || resp.code() in 200..204) {
                    _ranatiActive.value = false
                    _message.value = "تم إلغاء خدمة رناتي وحماية رصيدك بنجاح."
                } else {
                    _errorMessage.value = "تعذّر إلغاء الخدمة (${resp.code()})."
                }
            } catch (e: Exception) {
                _errorMessage.value = "تعذّر الاتصال بالخادم."
            } finally {
                _loadingMap.update { it - "RANATI" }
            }
        }
    }

    fun switchAccount(msisdn: String) {
        viewModelScope.launch {
            val ok = tokenStorage.switchAccount(msisdn)
            if (ok) loadData()
        }
    }

    fun removeAccount(msisdn: String) {
        viewModelScope.launch {
            tokenStorage.removeAccount(msisdn)
            loadData()
        }
    }

    fun logout() {
        viewModelScope.launch {
            tokenStorage.clearToken()
        }
    }

    fun clearMessages() {
        _message.value = null
        _errorMessage.value = null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    vm: SettingsViewModel = viewModel()
) {
    val isDark by vm.isDarkTheme.collectAsState()
    val accounts by vm.accounts.collectAsState()
    val activeMsisdn by vm.activeMsisdn.collectAsState()
    val hideNumber by vm.hideNumber.collectAsState()
    val callWait by vm.callWait.collectAsState()
    val ranatiActive by vm.ranatiActive.collectAsState()
    val loadingMap by vm.loadingMap.collectAsState()
    val msg by vm.message.collectAsState()
    val errMsg by vm.errorMessage.collectAsState()

    var showLogoutConfirm by remember { mutableStateOf(false) }
    var accountToRemove by remember { mutableStateOf<String?>(null) }

    if (!msg.isNullOrBlank()) {
        SuccessModal(msg ?: "") { vm.clearMessages() }
    }
    if (!errMsg.isNullOrBlank()) {
        ErrorModal(errMsg ?: "") { vm.clearMessages() }
    }

    if (showLogoutConfirm) {
        ConfirmModal(
            title = "تسجيل الخروج",
            subtitle = "هل تريد حقاً تسجيل الخروج من الحساب النشط؟",
            onConfirm = {
                showLogoutConfirm = false
                vm.logout()
                navController.navigate(Screen.Auth.route) {
                    popUpTo(0) { inclusive = true }
                }
            },
            onDismiss = { showLogoutConfirm = false }
        )
    }

    accountToRemove?.let { msisdn ->
        ConfirmModal(
            title = "حذف الرقم",
            subtitle = "هل تريد إزالة هذا الرقم من قائمة الحسابات المحفوظة؟",
            onConfirm = {
                vm.removeAccount(msisdn)
                accountToRemove = null
            },
            onDismiss = { accountToRemove = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("الإعدادات والخدمات", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
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
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Appearance Section ────────────────────────────────────────────
            item {
                Text("المظهر والتصميم", style = MaterialTheme.typography.labelLarge, color = TextSecondary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Border)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                if (isDark) Icons.Outlined.DarkMode else Icons.Outlined.LightMode,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text("الوضع الداكن (Dark Mode)", style = MaterialTheme.typography.bodyLarge, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                Text(if (isDark) "المظهر الليلي مفعّل" else "المظهر النهاري مفعّل", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                        }
                        Switch(
                            checked = isDark,
                            onCheckedChange = { vm.toggleTheme(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = OnPrimary,
                                checkedTrackColor = Primary,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = SurfaceVariant
                            )
                        )
                    }
                }
            }

            // ── SIM & Network Services Section ────────────────────────────────
            item {
                Text("خدمات الخط والشبكة", style = MaterialTheme.typography.labelLarge, color = TextSecondary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Border)
                ) {
                    Column(Modifier.padding(8.dp)) {
                        // SIM Migration
                        SettingsRowItem(
                            icon = Icons.Outlined.SwapCalls,
                            title = "تحويل نوع الشريحة",
                            subtitle = "التحويل بين عروض جيزي المختلفة",
                            onClick = { navController.navigate(Screen.Migration.route) }
                        )

                        HorizontalDivider(color = Border.copy(alpha = 0.5f))

                        // Hide Number
                        NetworkToggleRow(
                            icon = Icons.Outlined.VisibilityOff,
                            title = "إخفاء رقمي (Appel Masqué)",
                            subtitle = "إجراء المكالمات برقم مجهول",
                            isActive = hideNumber,
                            isLoading = loadingMap["APPELMASQUE"] == true,
                            onToggle = { vm.toggleService("APPELMASQUE", hideNumber) }
                        )

                        HorizontalDivider(color = Border.copy(alpha = 0.5f))

                        // Call Waiting
                        NetworkToggleRow(
                            icon = Icons.Outlined.PhoneInTalk,
                            title = "انتظار المكالمات (Double Appel)",
                            subtitle = "تنبيه عند ورود مكالمة أخرى",
                            isActive = callWait,
                            isLoading = loadingMap["CALLWAIT"] == true,
                            onToggle = { vm.toggleService("CALLWAIT", callWait) }
                        )

                        HorizontalDivider(color = Border.copy(alpha = 0.5f))

                        // Ranati Cancel
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Outlined.MusicNote, null, tint = Accent, modifier = Modifier.size(24.dp))
                                Column {
                                    Text("خدمة رناتي", style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        when (ranatiActive) {
                                            true  -> "الخدمة مفعلة (قد تخصم رصيداً)"
                                            false -> "معطلة (رصيدك في أمان)"
                                            null  -> "جاري الفحص..."
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (ranatiActive == true) Accent else TextSecondary
                                    )
                                }
                            }

                            if (loadingMap["RANATI"] == true) {
                                CircularProgressIndicator(color = Primary, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else if (ranatiActive == true) {
                                Button(
                                    onClick = { vm.cancelRanati() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color.White),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("إلغاء الاشتراك", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                IconButton(onClick = { vm.checkRanati() }) {
                                    Icon(Icons.Outlined.Refresh, null, tint = TextSecondary)
                                }
                            }
                        }
                    }
                }
            }

            // ── Saved Accounts Section ────────────────────────────────────────
            item {
                Text("الحسابات والأرقام المحفوظة", style = MaterialTheme.typography.labelLarge, color = TextSecondary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Border)
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        accounts.forEach { acc ->
                            val isActive = acc.msisdn == activeMsisdn
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isActive) Primary.copy(alpha = 0.1f) else SurfaceVariant)
                                    .clickable { if (!isActive) vm.switchAccount(acc.msisdn) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        if (isActive) Icons.Outlined.CheckCircle else Icons.Outlined.PersonOutline,
                                        contentDescription = null,
                                        tint = if (isActive) Primary else TextSecondary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Column {
                                        Text(acc.accountName, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                                        Text(acc.phoneDisplay, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                    }
                                }

                                if (accounts.size > 1 && !isActive) {
                                    IconButton(
                                        onClick = { accountToRemove = acc.msisdn },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Outlined.DeleteOutline, contentDescription = "إزالة", tint = Accent, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = { navController.navigate(Screen.Auth.route) },
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = OnPrimary)
                        ) {
                            Icon(Icons.Outlined.Add, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("إضافة رقم جديد", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // ── App & Legal Section ───────────────────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Border)
                ) {
                    Column(Modifier.padding(8.dp)) {
                        SettingsRowItem(
                            icon = Icons.Outlined.PrivacyTip,
                            title = "سياسة الخصوصية",
                            subtitle = "حماية البيانات واستخدام البوت",
                            onClick = { navController.navigate(Screen.PrivacyPolicy.route) }
                        )

                        HorizontalDivider(color = Border.copy(alpha = 0.5f))

                        SettingsRowItem(
                            icon = Icons.Outlined.Logout,
                            title = "تسجيل الخروج",
                            subtitle = "الخروج من الحساب النشط",
                            titleColor = Accent,
                            onClick = { showLogoutConfirm = true }
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun SettingsRowItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    titleColor: Color = TextPrimary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = if (titleColor != TextPrimary) titleColor else Primary, modifier = Modifier.size(24.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = titleColor, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = TextHint, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun NetworkToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isActive: Boolean?,
    isLoading: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = !isLoading, onClick = onToggle)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
        }

        if (isLoading) {
            CircularProgressIndicator(color = Primary, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Box(
                modifier = Modifier
                    .background(
                        if (isActive == true) Success.copy(alpha = 0.15f) else TextHint.copy(alpha = 0.15f),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    when (isActive) {
                        true  -> "مفعّل"
                        false -> "معطّل"
                        null  -> "؟"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive == true) Success else TextSecondary
                )
            }
        }
    }
}
