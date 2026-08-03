package com.boykta.net.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.boykta.net.data.local.SavedAccount
import com.boykta.net.navigation.Screen
import com.boykta.net.ui.theme.*
import com.boykta.net.viewmodel.DashboardViewModel
import java.util.Locale

@Composable
fun DashboardScreen(navController: NavController, vm: DashboardViewModel = viewModel()) {
    val state    by vm.uiState.collectAsState()
    val accounts by vm.accounts.collectAsState()

    var showAccountMenu   by remember { mutableStateOf(false) }
    var historyExpanded   by remember { mutableStateOf(false) }

    // Spinning refresh icon animation
    val infiniteTransition = rememberInfiniteTransition(label = "refresh_spin")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label = "spin"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // ── Header ──────────────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Account selector dropdown
                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceVariant)
                            .clickable { showAccountMenu = true }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Person,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                state.accountName.ifBlank { "حسابي" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                            if (state.phoneDisplay.isNotBlank()) {
                                Text(
                                    state.phoneDisplay,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                        Icon(
                            Icons.Outlined.KeyboardArrowDown,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showAccountMenu,
                        onDismissRequest = { showAccountMenu = false },
                        modifier = Modifier.background(CardBg)
                    ) {
                        accounts.forEach { acc ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            acc.accountName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (acc.msisdn == state.msisdn) Primary else TextPrimary,
                                            fontWeight = if (acc.msisdn == state.msisdn) FontWeight.Bold else FontWeight.Normal
                                        )
                                        Text(
                                            acc.phoneDisplay,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary
                                        )
                                    }
                                },
                                onClick = {
                                    showAccountMenu = false
                                    vm.switchAccount(acc.msisdn)
                                }
                            )
                        }
                        HorizontalDivider(color = Border)
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Outlined.Add, null, tint = Primary, modifier = Modifier.size(16.dp))
                                    Text("إضافة رقم جديد", style = MaterialTheme.typography.bodyMedium, color = Primary)
                                }
                            },
                            onClick = {
                                showAccountMenu = false
                                navController.navigate(Screen.Auth.route)
                            },
                            enabled = accounts.size < 5
                        )
                    }
                }

                IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                    Icon(Icons.Outlined.Settings, contentDescription = "الإعدادات", tint = TextSecondary)
                }
            }
        }

        // ── Small balance card ───────────────────────────────────────────────
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Border)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "الرصيد الرئيسي",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Primary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = if (state.mainBalance != null)
                                    String.format(Locale.US, "%.2f دج", state.mainBalance)
                                else "--",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = Primary
                            )
                        }
                    }
                    Icon(
                        Icons.Outlined.AccountBalanceWallet,
                        contentDescription = null,
                        tint = Primary.copy(alpha = 0.4f),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        // ── Large active package card ────────────────────────────────────────
        item {
            val firstProduct = state.productBalances.firstOrNull()
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Border)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "الباقة النشطة",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                        Icon(
                            Icons.Outlined.Wifi,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    if (state.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Primary, strokeWidth = 2.dp)
                    } else if (firstProduct == null) {
                        Text(
                            "لا توجد باقات إنترنت نشطة",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    } else {
                        val name    = firstProduct.commercialName?.ar ?: "باقة إنترنت"
                        val expiry  = firstProduct.expiryAt
                        val balance = firstProduct.balances?.firstOrNull()

                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )

                        if (balance != null) {
                            Text(
                                text = balance.displayRemaining(),
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = Primary
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
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    "ينتهي: $expiry",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary
                                )
                            }
                        }

                        // Additional packages
                        if (state.productBalances.size > 1) {
                            HorizontalDivider(color = Border, thickness = 0.5.dp)
                            state.productBalances.drop(1).forEach { product ->
                                val n = product.commercialName?.ar ?: "باقة"
                                val b = product.balances?.firstOrNull()
                                Row(
                                    Modifier.fillMaxWidth(),
                                    Arrangement.SpaceBetween,
                                    Alignment.CenterVertically
                                ) {
                                    Text(n, style = MaterialTheme.typography.bodySmall, color = TextSecondary, modifier = Modifier.weight(1f))
                                    Text(
                                        b?.displayRemaining() ?: "--",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Refresh button ───────────────────────────────────────────────────
        item {
            TextButton(
                onClick = { vm.loadAll() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Outlined.Refresh,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier
                        .size(16.dp)
                        .then(if (state.isLoading) Modifier.rotate(spinAngle) else Modifier)
                )
                Spacer(Modifier.width(6.dp))
                Text("تحديث", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
            }
        }

        // ── Service grid (5 buttons) ─────────────────────────────────────────
        item {
            val services = listOf(
                Triple("امشِ واربح",  Icons.Outlined.DirectionsRun,  Screen.WalkWin.route),
                Triple("+دعوة",       Icons.Outlined.PersonAdd,       Screen.Mgm.route),
                Triple("رسائل",       Icons.Outlined.Chat,            Screen.FreeSms.route),
                Triple("فليكسي",      Icons.Outlined.SwapHoriz,       Screen.Flexy.route),
                Triple("العروض",      Icons.Outlined.ShoppingCart,    Screen.Offers.route)
            )

            // First row: 3 buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                services.take(3).forEach { (label, icon, route) ->
                    ServiceGridButton(label, icon, Modifier.weight(1f)) {
                        navController.navigate(route)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Second row: 2 buttons (centered)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Spacer(Modifier.weight(0.5f))
                services.drop(3).forEach { (label, icon, route) ->
                    ServiceGridButton(label, icon, Modifier.weight(1f)) {
                        navController.navigate(route)
                    }
                }
                Spacer(Modifier.weight(0.5f))
            }
        }

        // ── Subscription history ─────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { historyExpanded = !historyExpanded }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "سجل التفعيلات الأخيرة",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Icon(
                    if (historyExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            if (historyExpanded && state.subscriptionHistory.isNotEmpty()) {
                state.subscriptionHistory.take(10).forEach { h ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                h.commercialName?.ar ?: h.packageCode ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary
                            )
                        }
                        Text(
                            if (h.packageFee != null && h.packageFee > 0)
                                String.format(Locale.US, "%.0f دج", h.packageFee)
                            else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            } else if (historyExpanded && !state.isLoading) {
                Text(
                    "لا توجد تفعيلات حديثة",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
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
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceVariant)
            .clickable(onClick = onClick)
            .padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, contentDescription = label, tint = Primary, modifier = Modifier.size(26.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextPrimary)
    }
}
