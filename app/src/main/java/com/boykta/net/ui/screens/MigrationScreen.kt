package com.boykta.net.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.boykta.net.ui.components.ConfirmModal
import com.boykta.net.ui.components.ErrorModal
import com.boykta.net.ui.components.SuccessModal
import com.boykta.net.ui.theme.*
import com.boykta.net.viewmodel.MigrationViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MigrationScreen(
    navController: NavController,
    vm: MigrationViewModel = viewModel()
) {
    val state by vm.uiState.collectAsState()

    var showSuccessModal by remember { mutableStateOf(false) }
    var showErrorModal by remember { mutableStateOf(false) }
    var errorMessageText by remember { mutableStateOf("") }
    var successMessageText by remember { mutableStateOf("") }

    LaunchedEffect(state.successMessage, state.errorMessage) {
        state.successMessage?.let {
            successMessageText = it
            showSuccessModal = true
        }
        state.errorMessage?.let {
            errorMessageText = it
            showErrorModal = true
        }
    }

    if (showSuccessModal) {
        SuccessModal(message = successMessageText) {
            showSuccessModal = false
            vm.clearMessages()
            navController.popBackStack()
        }
    }

    if (showErrorModal) {
        ErrorModal(errorMessageText) {
            showErrorModal = false
            vm.clearMessages()
        }
    }

    state.selectedOption?.let { option ->
        val toName = option.subscriptionTypeTo?.name?.ar ?: "العرض الجديد"
        val fromName = option.subscriptionTypeFrom?.name?.ar ?: "العرض الحالي"
        val fee = option.fee ?: 0.0
        val feeText = if (fee > 0) "برسوم: ${String.format(Locale.US, "%.0f", fee)} دج" else "مجاناً"

        ConfirmModal(
            title = "تأكيد تحويل الشريحة",
            subtitle = "هل أنت متأكد من رغبتك في تحويل شريحتك من ($fromName) إلى ($toName)؟\n$feeText",
            onConfirm = {
                option.id?.let { vm.executeMigration(it, toName) }
            },
            onDismiss = { vm.dismissConfirm() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "تحويل نوع الشريحة",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Outlined.ArrowBack,
                            contentDescription = "رجوع",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { vm.loadMigrationOptions() },
                        enabled = !state.isLoading && !state.isExecuting
                    ) {
                        Icon(
                            Icons.Outlined.Refresh,
                            contentDescription = "تحديث",
                            tint = Primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Background,
                    titleContentColor = TextPrimary
                )
            )
        },
        containerColor = Background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = Primary, strokeWidth = 3.dp)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "جاري جلب خيارات تحويل الشريحة...",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                state.options.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(Primary.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.SimCard,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "لا توجد خيارات تحويل متاحة",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "خطك الحالي لا يحتوي على عروض ترقية متاحة في الوقت الراهن.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = { vm.loadMigrationOptions() },
                            colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = OnPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Outlined.Refresh, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("إعادة المحاولة", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // ── Banner ────────────────────────────────────────────
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = CardBg),
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(18.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(Primary.copy(alpha = 0.15f), RoundedCornerShape(24.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Outlined.SwapCalls,
                                            contentDescription = null,
                                            tint = Primary,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            "التحويل بين عروض جيزي",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            "اختر العرض الذي ترغب في التحويل إليه واضغط للبدء.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }

                        // ── Items list ────────────────────────────────────────
                        items(state.options) { option ->
                            val toName = option.subscriptionTypeTo?.name?.ar
                                ?: option.subscriptionTypeTo?.name?.fr
                                ?: "عرض جديد"
                            val fromName = option.subscriptionTypeFrom?.name?.ar
                                ?: option.subscriptionTypeFrom?.name?.fr
                                ?: "العرض الحالي"
                            val desc = option.description?.ar ?: option.description?.fr ?: ""
                            val fee = option.fee ?: 0.0

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable(enabled = !state.isExecuting) {
                                        vm.selectOptionForConfirm(option)
                                    },
                                colors = CardDefaults.cardColors(containerColor = CardBg),
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Border)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .background(Success.copy(alpha = 0.15f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Outlined.Check,
                                                    null,
                                                    tint = Success,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Column {
                                                Text(
                                                    toName,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = TextPrimary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    "من: $fromName",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextSecondary
                                                )
                                            }
                                        }

                                        // Fee badge
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (fee == 0.0) Success.copy(alpha = 0.15f) else Primary.copy(alpha = 0.15f),
                                                    RoundedCornerShape(20.dp)
                                                )
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                if (fee == 0.0) "مجاني" else "${String.format(Locale.US, "%.0f", fee)} دج",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (fee == 0.0) Success else Primary
                                            )
                                        }
                                    }

                                    if (desc.isNotBlank()) {
                                        Text(
                                            desc,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }

                                    Button(
                                        onClick = { vm.selectOptionForConfirm(option) },
                                        modifier = Modifier.fillMaxWidth().height(44.dp),
                                        enabled = !state.isExecuting,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Primary,
                                            contentColor = OnPrimary
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Outlined.ArrowForward, null, Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("تحويل إلى $toName", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }

                        item { Spacer(Modifier.height(24.dp)) }
                    }
                }
            }

            if (state.isExecuting) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            CircularProgressIndicator(color = Primary, strokeWidth = 3.dp)
                            Text(
                                "جاري تنفيذ تحويل الشريحة...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
