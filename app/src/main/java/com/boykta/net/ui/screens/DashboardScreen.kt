package com.boykta.net.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.boykta.net.navigation.Screen
import com.boykta.net.ui.theme.*
import com.boykta.net.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(navController: NavController, vm: DashboardViewModel = viewModel()) {
    val state by vm.uiState.collectAsState()
    var historyExpanded by remember { mutableStateOf(false) }

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
                Column {
                    Text("boykta net", style = MaterialTheme.typography.titleMedium.copy(color = Primary, fontWeight = FontWeight.Bold))
                    if (state.accountName.isNotBlank())
                        Text(state.accountName, style = MaterialTheme.typography.bodyMedium)
                }
                IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                    Icon(Icons.Outlined.Settings, contentDescription = "الإعدادات", tint = TextSecondary)
                }
            }
        }

        // ── Balance card ────────────────────────────────────────────────────
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Border)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("الرصيد الرئيسي", style = MaterialTheme.typography.labelMedium)
                    if (state.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Primary, strokeWidth = 2.dp)
                    } else {
                        Text(
                            text = if (state.mainBalance != null) "%.2f دج".format(state.mainBalance) else "--",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Primary
                        )
                    }
                    if (state.phoneDisplay.isNotBlank())
                        Text(state.phoneDisplay, style = MaterialTheme.typography.bodyMedium)

                    // Active packages
                    if (state.productBalances.isNotEmpty()) {
                        HorizontalDivider(color = Border, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 6.dp))
                        Text("الباقات النشطة", style = MaterialTheme.typography.labelMedium)
                        state.productBalances.forEach { p ->
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                Text(p.commercialName?.ar ?: p.packageCode ?: "", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                Text(p.remaining ?: "", style = MaterialTheme.typography.bodyMedium, color = Primary)
                            }
                        }
                    }

                    // History accordion
                    HorizontalDivider(color = Border, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 6.dp))
                    Row(
                        Modifier.fillMaxWidth().clickable { historyExpanded = !historyExpanded },
                        Arrangement.SpaceBetween,
                        Alignment.CenterVertically
                    ) {
                        Text("سجل التفعيلات الأخيرة", style = MaterialTheme.typography.labelMedium)
                        Icon(
                            if (historyExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                            contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp)
                        )
                    }
                    if (historyExpanded) {
                        state.subscriptionHistory.take(10).forEach { h ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), Arrangement.SpaceBetween) {
                                Text(h.commercialName?.ar ?: h.packageCode ?: "", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                Text("%.0f دج".format(h.packageFee ?: 0.0), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            }
                        }
                    }
                }
            }
        }

        // ── Service grid ────────────────────────────────────────────────────
        item { Text("الخدمات", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp)) }

        item {
            val services = listOf(
                Triple("العروض",    Icons.Outlined.ShoppingCart, Screen.Offers.route),
                Triple("فليكسي",   Icons.Outlined.SwapHoriz,    Screen.Flexy.route),
                Triple("رسائل",    Icons.Outlined.Chat,          Screen.FreeSms.route),
                Triple("امشِ واربح", Icons.Outlined.DirectionsRun, Screen.WalkWin.route)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                services.forEach { (label, icon, route) ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceVariant)
                            .clickable { navController.navigate(route) }
                            .padding(vertical = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(icon, contentDescription = label, tint = Primary, modifier = Modifier.size(26.dp))
                        Text(label, style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                    }
                }
            }
        }

        // Refresh
        item {
            TextButton(onClick = { vm.loadAll() }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Refresh, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("تحديث", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
