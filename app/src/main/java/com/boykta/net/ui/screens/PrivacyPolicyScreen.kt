package com.boykta.net.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.boykta.net.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("سياسة الخصوصية", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "رجوع", tint = TextPrimary)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PolicySection(
                title = "مقدمة",
                body = "مرحباً بك في تطبيق boykta net. نحن نحترم خصوصيتك ونلتزم بحماية بياناتك الشخصية. توضح هذه السياسة كيفية جمع بياناتك واستخدامها وحمايتها عند استخدامك للتطبيق."
            )

            PolicySection(
                title = "البيانات التي نجمعها",
                body = """
• رقم الهاتف واسم الحساب: يُستخدمان للمصادقة مع خدمات جيزي فقط.
• رمز التحقق (OTP): يُقرأ تلقائياً من الرسائل القصيرة لتسهيل تسجيل الدخول، ولا يُخزَّن أو يُرسَل لأي جهة خارجية.
• رمز الدخول (Token): يُحفظ محلياً على جهازك فقط لإدارة الجلسة.
• وقت آخر تفعيل لخدمة "امشِ واربح": يُخزَّن محلياً لعرض العداد التنازلي.
                """.trimIndent()
            )

            PolicySection(
                title = "كيف نستخدم بياناتك",
                body = """
• تنفيذ طلباتك عبر واجهة برمجة تطبيقات جيزي (Djezzy API) مباشرةً.
• عرض رصيدك وباقاتك وسجلاتك داخل التطبيق.
• جدولة إشعارات محلية (مثل تذكير 2 جيجابايت الأسبوعية) — تعمل محلياً دون إرسال أي بيانات.
                """.trimIndent()
            )

            PolicySection(
                title = "مشاركة البيانات مع أطراف ثالثة",
                body = """
• لا نشارك بياناتك الشخصية مع أي طرف ثالث لأغراض تجارية.
• يستخدم التطبيق مكتبة إعلانات Start.io لعرض إعلانات بيانية. قد تجمع هذه المكتبة بيانات مجهولة المصدر وفقاً لسياسة خصوصيتها الخاصة.
• يتم إرسال طلبات الخدمة مباشرةً إلى خوادم جيزي (apim.djezzy.dz) دون المرور بخوادم وسيطة خاصة بنا.
                """.trimIndent()
            )

            PolicySection(
                title = "تخزين البيانات وأمانها",
                body = """
• جميع بياناتك (الرمز، رقم الهاتف، إعدادات الخدمات) مخزنة محلياً على جهازك باستخدام DataStore المشفر.
• لا يوجد خادم خلفي (Backend) خاص بنا يجمع أو يخزن بياناتك.
• يمكنك حذف جميع بياناتك في أي وقت عبر خيار "تسجيل الخروج / حذف الرقم" في الإعدادات.
                """.trimIndent()
            )

            PolicySection(
                title = "الأذونات المطلوبة",
                body = """
• RECEIVE_SMS / READ_SMS: لقراءة رمز OTP تلقائياً من الرسائل القصيرة.
• INTERNET: للتواصل مع خدمات جيزي.
• POST_NOTIFICATIONS: لإرسال تذكيرات محلية (مثل تذكير 2 جيجابايت).
                """.trimIndent()
            )

            PolicySection(
                title = "التواصل معنا",
                body = "إذا كانت لديك أي استفسارات حول سياسة الخصوصية، يمكنك التواصل معنا عبر صفحة المطور على Facebook:\nfacebook.com/boyktanet"
            )

            PolicySection(
                title = "تاريخ آخر تحديث",
                body = "أغسطس 2026"
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PolicySection(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = Primary,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
        )
        HorizontalDivider(color = Border, thickness = 0.5.dp)
    }
}
