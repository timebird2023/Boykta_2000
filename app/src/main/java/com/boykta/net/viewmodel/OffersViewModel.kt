package com.boykta.net.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.boykta.net.data.api.ApiClient
import com.boykta.net.data.local.TokenStorage
import com.boykta.net.data.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class OfferActivationState {
    object Idle     : OfferActivationState()
    object Loading  : OfferActivationState()
    object Success  : OfferActivationState()
    data class Error(val message: String) : OfferActivationState()
}

class OffersViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenStorage = TokenStorage(application)
    private val api = ApiClient.api

    private val _activationState = MutableStateFlow<OfferActivationState>(OfferActivationState.Idle)
    val activationState: StateFlow<OfferActivationState> = _activationState.asStateFlow()

    fun activateOffer(offer: PaidOffer) {
        viewModelScope.launch(Dispatchers.IO) {
            _activationState.value = OfferActivationState.Loading

            val token  = tokenStorage.accessToken.firstOrNull() ?: ""
            val msisdn = tokenStorage.msisdn.firstOrNull() ?: ""

            if (token.isBlank() || msisdn.isBlank()) {
                _activationState.value = OfferActivationState.Error("انتهت الجلسة. سجّل دخولك مجدداً.")
                return@launch
            }

            val auth = "Bearer $token"

            try {
                val response = when (offer.activationType) {
                    "shake" -> activateShakeWithRetry(auth, msisdn, offer)
                    else    -> activateProductWithRetry(auth, msisdn, offer)
                } ?: run {
                    _activationState.value = OfferActivationState.Error("انتهت صلاحية الجلسة. سجّل دخولك مجدداً.")
                    return@launch
                }

                when {
                    response.isSuccessful || response.code() in 200..202 -> {
                        _activationState.value = OfferActivationState.Success
                    }
                    response.code() == 402 -> {
                        _activationState.value = OfferActivationState.Error(
                            "رصيدك غير كافٍ لتفعيل ${offer.name}.\nالمطلوب: ${offer.price} دج على الأقل."
                        )
                    }
                    else -> {
                        val ar = extractArabicMessage(response.errorBody()?.string() ?: "")
                        _activationState.value = OfferActivationState.Error(
                            ar.ifBlank { "فشل التفعيل (${response.code()}). يرجى التأكد من توفر الرصيد والمحاولة ثانية." }
                        )
                    }
                }
            } catch (e: Exception) {
                _activationState.value = OfferActivationState.Error("تعذّر الاتصال بالخادم. تأكد من اتصال الإنترنت.")
            }
        }
    }

    /**
     * Resilient Shake offer activation:
     * 1. GET /shake/{msisdn} → retry until packageCode appears or loop limit.
     * 2. POST /shake/{msisdn} → retry on transient errors (429, 500, etc.) until 200/201 or 402.
     */
    private suspend fun activateShakeWithRetry(
        auth: String,
        msisdn: String,
        offer: PaidOffer
    ): retrofit2.Response<ApiResponse<Any>>? {
        val pkgCode = offer.packageCode
        val maxAttempts = 12

        // ── Step 1: GET check ────────────────────────────────────────────────
        var found = false
        var getAttempts = 0
        while (!found && getAttempts < maxAttempts) {
            getAttempts++
            try {
                val r = api.checkShake(auth, msisdn)
                if (r.code() == 401) return null
                if (r.isSuccessful) {
                    val body = r.body()
                    @Suppress("UNCHECKED_CAST")
                    val dataMap = body?.data as? Map<*, *>
                    if (dataMap?.get("code") == pkgCode) {
                        found = true
                        break
                    }
                }
            } catch (_: Exception) { }
            delay(150)
        }

        // ── Step 2: POST execution ───────────────────────────────────────────
        var postAttempts = 0
        while (postAttempts < maxAttempts) {
            postAttempts++
            try {
                val r = api.activateShake(auth, msisdn, ActivateProductRequest(pkgCode))
                if (r.code() == 401) return null
                if (r.code() == 402) return r // Stop immediately if balance is insufficient
                if (r.isSuccessful || r.code() in 200..202) return r
            } catch (_: Exception) { }
            delay(150)
        }

        return api.activateShake(auth, msisdn, ActivateProductRequest(pkgCode))
    }

    /**
     * Standard Product Activation with transient retry:
     */
    private suspend fun activateProductWithRetry(
        auth: String,
        msisdn: String,
        offer: PaidOffer
    ): retrofit2.Response<ApiResponse<Any>>? {
        val pkgCode = offer.packageCode
        val maxAttempts = 6
        var attempts = 0

        while (attempts < maxAttempts) {
            attempts++
            try {
                val r = api.activateProduct(auth, msisdn, ActivateProductRequest(pkgCode))
                if (r.code() == 401) return null
                if (r.code() == 402) return r
                if (r.isSuccessful || r.code() in 200..202) return r
            } catch (_: Exception) { }
            delay(150)
        }

        return api.activateProduct(auth, msisdn, ActivateProductRequest(pkgCode))
    }

    private fun extractArabicMessage(json: String): String {
        return try {
            Regex(""""ar"\s*:\s*"([^"]+)"""").find(json)?.groupValues?.get(1) ?: ""
        } catch (e: Exception) { "" }
    }

    fun resetState() { _activationState.value = OfferActivationState.Idle }
}
