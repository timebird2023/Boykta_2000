package com.boykta.net.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.boykta.net.ui.theme.*

/** Success modal shown after every successful API operation */
@Composable
fun SuccessModal(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBg, RoundedCornerShape(16.dp))
                .padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SuccessIcon()
                Text(
                    text = "تمت العملية بنجاح",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "يرجى متابعة الصفحة ليصلك كل جديد.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = OnPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("حسناً", fontWeight = FontWeight.SemiBold)
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
                .background(CardBg, RoundedCornerShape(16.dp))
                .padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ErrorIcon()
                Text(
                    text = "حدث خطأ",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Error,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariant, contentColor = TextPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("إغلاق")
                }
            }
        }
    }
}

/** Confirmation modal before activating an offer */
@Composable
fun ConfirmModal(title: String, subtitle: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBg, RoundedCornerShape(16.dp))
                .padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(title,   style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                    ) { Text("إلغاء") }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = OnPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text("تأكيد", fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}

// ── Inline SVG-style icon composables ─────────────────────────────────────────

@Composable
private fun SuccessIcon() {
    Box(
        modifier = Modifier
            .size(56.dp)
            .background(Success.copy(alpha = 0.15f), RoundedCornerShape(28.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text("✓", color = Success, style = MaterialTheme.typography.displayLarge)
    }
}

@Composable
private fun ErrorIcon() {
    Box(
        modifier = Modifier
            .size(56.dp)
            .background(Error.copy(alpha = 0.15f), RoundedCornerShape(28.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text("✕", color = Error, style = MaterialTheme.typography.displayLarge)
    }
}
