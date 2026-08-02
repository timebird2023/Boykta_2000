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
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.boykta.net.data.local.TokenStorage
import com.boykta.net.navigation.Screen
import com.boykta.net.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val tokenStorage = remember { TokenStorage(context) }
    val scope = rememberCoroutineScope()

    var showLogoutConfirm by remember { mutableStateOf(false) }
    val accountName  by tokenStorage.accountName.collectAsState(initial = "")
    val phoneDisplay by tokenStorage.phoneDisplay.collectAsState(initial = "")

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
            // Account info card
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

            Spacer(Modifier.height(8.dp))
            Text("عام", style = MaterialTheme.typography.labelMedium, color = TextSecondary)

            // Settings items
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

            Spacer(Modifier.height(8.dp))
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
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceVariant)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, contentDescription = null, tint = if (labelColor == TextPrimary) TextSecondary else labelColor, modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = labelColor)
            if (subtitle != null)
                Text(subtitle, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        }
        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = TextHint, modifier = Modifier.size(18.dp))
    }
}
