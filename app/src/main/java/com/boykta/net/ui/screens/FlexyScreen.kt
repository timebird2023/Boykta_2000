package com.boykta.net.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
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
import com.boykta.net.viewmodel.FlexyUiState
import com.boykta.net.viewmodel.FlexyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlexyScreen(navController: NavController, vm: FlexyViewModel = viewModel()) {
    val context  = LocalContext.current
    val activity = context as? Activity
    val actionState  by vm.actionState.collectAsState()
    val historyState by vm.historyState.collectAsState()

    var selectedTab  by remember { mutableIntStateOf(0) }
    var receiver     by remember { mutableStateOf("") }
    var amount       by remember { mutableStateOf("") }
    var pin          by remember { mutableStateOf("") }
    var showSuccess  by remember { mutableStateOf(false) }
    var showError    by remember { mutableStateOf(false) }
    var errorMsg     by remember { mutableStateOf("") }

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

            // Tab row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Background,
                contentColor = Primary
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0; receiver = ""; amount = ""; pin = "" }) {
                    Text("تحويل رصيد", modifier = Modifier.padding(12.dp))
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1; receiver = ""; amount = "" }) {
                    Text("تحويل إنترنت", modifier = Modifier.padding(12.dp))
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Input fields
                item {
                    AppTextField(
                        value = receiver, onValueChange = { receiver = it },
                        label = "رقم المستلم", placeholder = "07XXXXXXXX",
                        keyboardType = KeyboardType.Phone,
                        enabled = actionState !is FlexyUiState.Loading
                    )
                }

                if (selectedTab == 0) {
                    item {
                        AppTextField(
                            value = amount, onValueChange = { amount = it },
                            label = "المبلغ (دج)", placeholder = "50",
                            keyboardType = KeyboardType.Number,
                            enabled = actionState !is FlexyUiState.Loading
                        )
                    }
                    item {
                        AppTextField(
                            value = pin, onValueChange = { pin = it },
                            label = "الرقم السري", placeholder = "****",
                            keyboardType = KeyboardType.NumberPassword,
                            enabled = actionState !is FlexyUiState.Loading
                        )
                    }
                }

                item {
                    Button(
                        onClick = {
                            if (selectedTab == 0) vm.transferCredit(receiver, amount, pin)
                            else vm.transferData(receiver)
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        enabled = actionState !is FlexyUiState.Loading,
                        colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = OnPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (actionState is FlexyUiState.Loading)
                            CircularProgressIndicator(Modifier.size(20.dp), color = OnPrimary, strokeWidth = 2.dp)
                        else
                            Text(if (selectedTab == 0) "إرسال الرصيد" else "إرسال 1 جيجابايت", fontWeight = FontWeight.SemiBold)
                    }
                }

                // History
                val history = if (selectedTab == 0) historyState.creditHistory else historyState.dataHistory
                if (history.isNotEmpty()) {
                    item { Text("السجل", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp)) }
                    items(history) { h ->
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .background(SurfaceVariant, RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(h.msisdnBParty ?: "", style = MaterialTheme.typography.bodyMedium)
                            Text(h.date ?: "", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        }
                    }
                }
            }
        }
    }
}
