package com.boykta.net.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.boykta.net.ui.theme.*

private const val FB_PAGE_URL = "https://www.facebook.com/boyktanet"
private val FacebookBlue = Color(0xFF1877F2)

/**
 * Success modal shown after every successful API operation.
 * Includes a "متابعة الصفحة" button that opens the Facebook page.
 */
@Composable
fun SuccessModal(onDismiss: () -> Unit) {
    val context = LocalContext.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBg, RoundedCornerShape(20.dp))
                .border(1.dp, Primary.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                .padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ── Glowing success icon ───────────────────────────────────────
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            Brush.radialGradient(
                                listOf(Success.copy(alpha = 0.25f), Success.copy(alpha = 0f))
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = Success,
                        modifier = Modifier.size(44.dp)
                    )
                }

                // ── Title ─────────────────────────────────────────────────────
                Text(
                    "تمت العملية بنجاح! ✓",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                // ── Subtitle ──────────────────────────────────────────────────
                Text(
                    "تابع صفحتنا على فيسبوك ليصلك كل جديد من عروض وأكواد مجانية.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(Modifier.height(2.dp))

                // ── Facebook page button ───────────────────────────────────────
                Button(
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(FB_PAGE_URL))
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FacebookBlue,
                        contentColor   = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Outlined.OpenInBrowser,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("متابعة الصفحة", fontWeight = FontWeight.SemiBold)
                }

                // ── Dismiss button ─────────────────────────────────────────────
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Border)
                ) {
                    Text("حسناً")
                }
            }
        }
    }
}

/** Error modal — displays the Arabic error message from the API */
@Composable
fun ErrorModal(message: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBg, RoundedCornerShape(20.dp))
                .border(1.dp, Error.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                .padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ErrorIcon()
                Text(
                    "حدث خطأ",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Error,
                    textAlign = TextAlign.Center
                )
                Text(
                    message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SurfaceVariant,
                        contentColor   = TextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("إغلاق") }
            }
        }
    }
}

/** Confirmation modal before activating an offer */
@Composable
fun ConfirmModal(
    title: String,
    subtitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBg, RoundedCornerShape(20.dp))
                .border(1.dp, Border, RoundedCornerShape(20.dp))
                .padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(title,    style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium,  color = TextSecondary)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick  = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(10.dp),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                    ) { Text("إلغاء") }
                    Button(
                        onClick  = onConfirm,
                        modifier = Modifier.weight(1f),
                        colors   = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = OnPrimary),
                        shape    = RoundedCornerShape(10.dp)
                    ) { Text("تأكيد", fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}

// ── Icon composables ──────────────────────────────────────────────────────────

@Composable
private fun ErrorIcon() {
    Box(
        modifier = Modifier
            .size(56.dp)
            .background(Error.copy(alpha = 0.15f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text("✕", color = Error, style = MaterialTheme.typography.displayLarge)
    }
}
