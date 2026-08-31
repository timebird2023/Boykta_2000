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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.boykta.net.ads.AdsManager
import com.boykta.net.navigation.Screen
import com.boykta.net.ui.theme.*
import com.boykta.net.viewmodel.DashboardViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    vm: DashboardViewModel = viewModel()
) {
    val state by vm.uiState.collectAsState()
    val accounts by vm.accounts.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity

    var showAccountSwitcher by remember { mutableStateOf(false) }
    var historyExpanded by remember { mutableStateOf(false) }

    // Spin animation for refresh button
    val infiniteTransition = rememberInfiniteTransition(label = "spin")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin_angle"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { showAccountSwitcher = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Primary.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.Person,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                state.accountName.ifBlank { state.phoneDisplay.ifBlank { "boykta net" } },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            if (state.phoneDisplay.isNotBlank()) {
                                Text(
                                    state.phoneDisplay,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }
                        Icon(
                            Icons.Outlined.ArrowDropDown,
                            contentDescription = "تبديل الحساب",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                        Icon(
                            Icons.Outlined.Settings,
                            contentDescription = "الإعدادات",
                            tint = TextPrimary
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Main Balance Card ─────────────────────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Primary.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            Arrangement.SpaceBetween,
                            Alignment.CenterVertically
                        ) {
                            Text(
                                "الرصيد الرئيسي",
                                style = MaterialTheme.typography.labelLarge,
                                color = TextSecondary
                            )
                            Box(
                                modifier = Modifier
                                    .background(Success.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "متصل",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Success,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Balance amount
                        val balText = state.mainBalance?.let {
                            String.format(Locale.US, "%.2f", it)
                        } ?: "--"

                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                balText,
                                fontSize = 38.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Primary
                            )
                            Text(
                                "دج",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextSecondary,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }

                        HorizontalDivider(color = Border.copy(alpha = 0.5f))

                        // Active Package info
                        val activeProduct = state.productBalances.firstOrNull()
                        val balItem = activeProduct?.balances?.firstOrNull()
                        val expiry = activeProduct?.expiryAt

                        Row(
                            Modifier.fillMaxWidth(),
                            Arrangement.SpaceBetween,
                            Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "الإنترنت المتبقي",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                                Text(
                                    balItem?.displayRemaining() ?: "لا توجد باقة نشطة",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (balItem != null) Primary else TextHint
                                )
                            }

                            if (!expiry.isNullOrBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.Schedule,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        "ينتهي: $expiry",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }

                        // Additional packages preview if any
                        if (state.productBalances.size > 1) {
                            state.productBalances.drop(1).forEach { product ->
                                val n = product.commercialName?.ar ?: "باقة إضافية"
                                val b = product.balances?.firstOrNull()
                                Row(
                                    Modifier.fillMaxWidth(),
                                    Arrangement.SpaceBetween,
                                    Alignment.CenterVertically
                                ) {
                                    Text(
                                        n,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        b?.displayRemaining() ?: "--",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Refresh Bar ───────────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "الخدمات السريعة",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    TextButton(
                        onClick = { vm.loadAll() },
                        enabled = !state.isLoading
                    ) {
                        Icon(
                            Icons.Outlined.Refresh,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier
                                .size(16.dp)
                                .then(if (state.isLoading) Modifier.rotate(spinAngle) else Modifier)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "تحديث الرصيد",
                            color = Primary,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ── Service Grid (6 items in 2x3 layout) ──────────────────────────
            item {
                val services = listOf(
                    Triple("امشِ واربح",  Icons.Outlined.DirectionsRun,  Screen.WalkWin.route),
                    Triple("عروض جيزي",   Icons.Outlined.ShoppingCart,    Screen.Offers.route),
                    Triple("تحويل الشريحة", Icons.Outlined.SwapCalls,      Screen.Migration.route),
                    Triple("جوائز MGM",   Icons.Outlined.CardGiftcard,    Screen.Mgm.route),
                    Triple("رسائل مجانية", Icons.Outlined.Chat,            Screen.FreeSms.route),
                    Triple("فليكسي",       Icons.Outlined.SwapHoriz,       Screen.Flexy.route)
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Row 1: 3 buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        services.take(3).forEach { (label, icon, route) ->
                            ServiceGridButton(label, icon, Modifier.weight(1f)) {
                                navController.navigate(route)
                            }
                        }
                    }

                    // Row 2: 3 buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        services.drop(3).forEach { (label, icon, route) ->
                            ServiceGridButton(label, icon, Modifier.weight(1f)) {
                                navController.navigate(route)
                            }
                        }
                    }
                }
            }

            // ── Subscription History Accordion ─────────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Border)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { historyExpanded = !historyExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.History,
                                    contentDescription = null,
                                    tint = Primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    "سجل التفعيلات الأخيرة",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                            Icon(
                                if (historyExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        if (historyExpanded) {
                            Spacer(Modifier.height(12.dp))
                            HorizontalDivider(color = Border.copy(alpha = 0.5f))
                            Spacer(Modifier.height(8.dp))

                            if (state.subscriptionHistory.isNotEmpty()) {
                                state.subscriptionHistory.take(8).forEach { h ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                h.commercialName?.ar ?: h.packageCode ?: "",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextPrimary
                                            )
                                            h.activationDate?.let { dt ->
                                                Text(
                                                    dt.split("T").firstOrNull() ?: dt,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextSecondary
                                                )
                                            }
                                        }
                                        Text(
                                            if (h.packageFee != null && h.packageFee > 0)
                                                String.format(Locale.US, "%.0f دج", h.packageFee)
                                            else "",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Primary
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    if (state.isLoading) "جاري التحميل..." else "لا توجد تفعيلات سابقة مسجلة.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(20.dp)) }
        }
    }

    // ── Account Switcher Bottom Sheet ─────────────────────────────────────────
    if (showAccountSwitcher) {
        ModalBottomSheet(
            onDismissRequest = { showAccountSwitcher = false },
            containerColor = CardBg,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "تبديل الحساب",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                accounts.forEach { acc ->
                    val isActive = acc.msisdn == state.msisdn
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isActive) Primary.copy(alpha = 0.12f) else SurfaceVariant)
                            .clickable {
                                vm.switchAccount(acc.msisdn)
                                showAccountSwitcher = false
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                acc.accountName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                acc.phoneDisplay,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }

                        if (isActive) {
                            Icon(
                                Icons.Outlined.CheckCircle,
                                contentDescription = "الحساب النشط",
                                tint = Primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        showAccountSwitcher = false
                        navController.navigate(Screen.Auth.route)
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary),
                    border = BorderStroke(1.dp, Primary)
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("إضافة رقم جديد", fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun ServiceGridButton(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceVariant)
            .border(1.dp, Border.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(Primary.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = Primary,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            fontSize = 12.sp
        )
    }
}
