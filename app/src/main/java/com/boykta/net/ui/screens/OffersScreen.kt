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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.boykta.net.ads.AdsManager
import com.boykta.net.data.models.PAID_OFFERS
import com.boykta.net.data.models.PaidOffer
import com.boykta.net.ui.components.ConfirmModal
import com.boykta.net.ui.components.ErrorModal
import com.boykta.net.ui.components.SuccessModal
import com.boykta.net.ui.theme.*
import com.boykta.net.viewmodel.OfferActivationState
import com.boykta.net.viewmodel.OffersViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OffersScreen(navController: NavController, vm: OffersViewModel = viewModel()) {
    val context = LocalContext.current
    val activity = context as? Activity
    val state by vm.activationState.collectAsState()

    var selectedOffer by remember { mutableStateOf<PaidOffer?>(null) }
    var showConfirm   by remember { mutableStateOf(false) }
    var showSuccess   by remember { mutableStateOf(false) }
    var showError     by remember { mutableStateOf(false) }
    var errorMsg      by remember { mutableStateOf("") }

    // Pre-load ad in background while user browses
    LaunchedEffect(Unit) { activity?.let { AdsManager.preload(it) } }

    LaunchedEffect(state) {
        when (val s = state) {
            is OfferActivationState.Success -> { showSuccess = true }
            is OfferActivationState.Error   -> { errorMsg = s.message; showError = true }
            else -> {}
        }
    }

    if (showConfirm && selectedOffer != null) {
        ConfirmModal(
            title    = "تأكيد التفعيل",
            subtitle = "${selectedOffer!!.name} — ${selectedOffer!!.amount} بـ ${selectedOffer!!.price} دج (${selectedOffer!!.duration})",
            onConfirm = {
                showConfirm = false
                vm.activateOffer(selectedOffer!!)
            },
            onDismiss = { showConfirm = false; vm.resetState() }
        )
    }

    if (showSuccess) {
        SuccessModal {
            showSuccess = false
            vm.resetState()
            // Show interstitial after the user closes the success modal
            activity?.let { AdsManager.showInterstitial(it) }
        }
    }

    if (showError) {
        ErrorModal(message = errorMsg) { showError = false; vm.resetState() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("عروض الإنترنت", style = MaterialTheme.typography.titleMedium) },
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
        if (state is OfferActivationState.Loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(PAID_OFFERS) { offer ->
                    OfferCard(offer = offer) {
                        selectedOffer = offer
                        showConfirm   = true
                    }
                }
            }
        }
    }
}

@Composable
private fun OfferCard(offer: PaidOffer, onClick: () -> Unit) {
    Card(
        modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Title row
            Text(offer.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)

            // Detail rows: الحجم / السعر / المدة
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // الحجم
                DetailChip(label = "الحجم", value = offer.amount, modifier = Modifier.weight(1f))
                // السعر
                DetailChip(label = "السعر", value = "${offer.price} دج", isAccent = true, modifier = Modifier.weight(1f))
                // المدة
                DetailChip(label = "المدة", value = offer.duration, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DetailChip(
    label: String,
    value: String,
    isAccent: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                if (isAccent) Primary.copy(alpha = 0.12f) else SurfaceVariant,
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isAccent) Primary else TextSecondary
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = if (isAccent) Primary else TextPrimary,
            fontWeight = if (isAccent) FontWeight.Bold else FontWeight.Normal
        )
    }
}
