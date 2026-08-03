package com.boykta.net.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.boykta.net.ads.AdsManager
import com.boykta.net.ui.components.ErrorModal
import com.boykta.net.ui.components.SuccessModal
import com.boykta.net.ui.theme.*
import com.boykta.net.viewmodel.DataPackage
import com.boykta.net.viewmodel.FlexyUiState
import com.boykta.net.viewmodel.FlexyViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlexyScreen(navController: NavController, vm: FlexyViewModel = viewModel()) {
    val context      = LocalContext.current
    val activity     = context as? Activity
    val actionState  by vm.actionState.collectAsState()
    val historyState by vm.historyState.collectAsState()

    // 0 = تحويل إنترنت, 1 = تحويل رصيد
    var selectedTab      by remember { mutableIntStateOf(0) }
    var receiver         by remember { mutableStateOf("") }
    var amount           by remember { mutableStateOf("") }
    var pin              by remember { mutableStateOf("") }
    var selectedPackage  by remember { mutableStateOf(DataPackage.GB1) }
    var showSuccess      by remember { mutableStateOf(false) }
    var showError        by remember { mutableStateOf(false) }
    var errorMsg         by remember { mutableStateOf("") }

    LaunchedEffect(actionState) {
        when (val s = actionState) {
            is FlexyUiState.Success -> { showSuccess = true }
            is FlexyUiState.Error   -> { errorMsg = s.message; showError = true }
            else -> {}
        }
    }

    if (showSuccess) {
        SuccessModal {
            showSuccess = false
            vm.resetState()
            activity?.let { AdsManager.showInterstitial(it) }
        }
    }
    if (showError) ErrorModal(message = errorMsg) { showError = false; vm.resetState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تحويل الرصيد والإنترنت", style = MaterialTheme.typography.titleMedium) },
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

            // ── Tab row — إنترنت first, then رصيد ───────────────────────────
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor   = Background,
                contentColor     = Primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick  = { selectedTab = 0; receiver = ""; selectedPackage = DataPackage.GB1 }
                ) {
                    Text("تحويل إنترنت", modifier = Modifier.padding(12.dp))
                }
                Tab(
                    selected = selectedTab == 1,
                    onClick  = { selectedTab = 1; receiver = ""; amount = ""; pin = "" }
                ) {
                    Text("تحويل رصيد", modifier = Modifier.padding(12.dp))
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // ── Receiver field ────────────────────────────────────────────
                item {
                    AppTextField(
                        value         = receiver,
                        onValueChange = { receiver = it },
                        label         = "رقم المستلم",
                        placeholder   = "07XXXXXXXX",
                        keyboardType  = KeyboardType.Phone,
                        enabled       = actionState !is FlexyUiState.Loading
                    )
                }

                if (selectedTab == 0) {
                    // ── Internet package selector ─────────────────────────────
                    item {
                        Text(
                            "اختر الحجم:",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DataPackage.values().forEach { pkg ->
                                val selected = selectedPackage == pkg
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (selected) Primary.copy(alpha = 0.15f) else SurfaceVariant)
                                        .border(
                                            width = if (selected) 1.5.dp else 0.5.dp,
                                            color = if (selected) Primary else Border,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable(enabled = actionState !is FlexyUiState.Loading) {
                                            selectedPackage = pkg
                                        }
                                        .padding(vertical = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text  = pkg.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (selected) Primary else TextPrimary,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    // ── Send internet button ──────────────────────────────────
                    item {
                        Button(
                            onClick  = { vm.transferData(receiver, selectedPackage) },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            enabled  = actionState !is FlexyUiState.Loading,
                            colors   = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = OnPrimary),
                            shape    = RoundedCornerShape(12.dp)
                        ) {
                            if (actionState is FlexyUiState.Loading)
                                CircularProgressIndicator(Modifier.size(20.dp), color = OnPrimary, strokeWidth = 2.dp)
                            else
                                Text("إرسال ${selectedPackage.label}", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // ── Internet transfer history ──────────────────────────────
                    val dataHistory = historyState.dataHistory
                    if (dataHistory.isNotEmpty()) {
                        item {
                            Text(
                                "سجل تحويلات الإنترنت",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        items(dataHistory.take(10)) { h ->
                            HistoryRow(
                                phone = h.msisdnBParty ?: "",
                                detail = h.date ?: ""
                            )
                        }
                    }

                } else {
                    // ── Credit transfer fields ────────────────────────────────
                    item {
                        AppTextField(
                            value         = amount,
                            onValueChange = { amount = it },
                            label         = "المبلغ (دج)",
                            placeholder   = "50",
                            keyboardType  = KeyboardType.Number,
                            enabled       = actionState !is FlexyUiState.Loading
                        )
                    }
                    item {
                        AppTextField(
                            value         = pin,
                            onValueChange = { pin = it },
                            label         = "الرقم السري",
                            placeholder   = "****",
                            keyboardType  = KeyboardType.NumberPassword,
                            enabled       = actionState !is FlexyUiState.Loading
                        )
                    }

                    // ── Send credit button ────────────────────────────────────
                    item {
                        Button(
                            onClick  = { vm.transferCredit(receiver, amount, pin) },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            enabled  = actionState !is FlexyUiState.Loading,
                            colors   = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = OnPrimary),
                            shape    = RoundedCornerShape(12.dp)
                        ) {
                            if (actionState is FlexyUiState.Loading)
                                CircularProgressIndicator(Modifier.size(20.dp), color = OnPrimary, strokeWidth = 2.dp)
                            else
                                Text(
                                    if (amount.isNotBlank())
                                        String.format(Locale.US, "إرسال %.0f دج", amount.toDoubleOrNull() ?: 0.0)
                                    else "إرسال الرصيد",
                                    fontWeight = FontWeight.SemiBold
                                )
                        }
                    }

                    // ── Credit transfer history ───────────────────────────────
                    val creditHistory = historyState.creditHistory
                    if (creditHistory.isNotEmpty()) {
                        item {
                            Text(
                                "سجل تحويلات الرصيد",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        items(creditHistory.take(10)) { h ->
                            HistoryRow(
                                phone  = h.msisdnBParty ?: "",
                                detail = if (h.amount != null) String.format(Locale.US, "%.0f دج", (h.amount as? Double) ?: 0.0)
                                         else h.date ?: ""
                            )
                        }
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun HistoryRow(phone: String, detail: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceVariant, RoundedCornerShape(10.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(phone,  style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
        Text(detail, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
    }
}
