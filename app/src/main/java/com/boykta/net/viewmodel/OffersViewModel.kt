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
                    else    -> api.activateProduct(auth, msisdn, ActivateProductRequest(offer.packageCode))
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
                            ar.ifBlank { "فشل التفعيل (${response.code()}). حاول مرة أخرى." }
                        )
                    }
                }
            } catch (e: Exception) {
                _activationState.value = OfferActivationState.Error("تعذّر الاتصال بالخادم.")
            }
        }
    }

    /**
     * Shake offer activation with retry — mirrors the Python bot logic exactly:
     *  1. GET /shake/{msisdn} → retry up to 10 times until data.code == packageCode
     *  2. POST /shake/{msisdn} with packageCode → retry up to 10 times on any error except 402
     *
     * Returns null only if the session has expired (401 from OkHttp Authenticator).
     */
    private suspend fun activateShakeWithRetry(
        auth: String,
        msisdn: String,
        offer: PaidOffer
    ): retrofit2.Response<ApiResponse<Any>>? {
        val pkgCode = offer.packageCode
        val maxAttempts = 10

        // ── Step 1: GET until data.code == packageCode ──────────────────────
        var found = false
        var getAttempts = 0
        while (!found && getAttempts < maxAttempts) {
            getAttempts++
            try {
                val r = api.checkShake(auth, msisdn)
                if (r.code() == 401) return null   // session expired
                if (r.isSuccessful) {
                    // Parse the nested JSON to check data.code
                    val body = r.body()
                    @Suppress("UNCHECKED_CAST")
                    val dataMap = body?.data as? Map<*, *>
                    if (dataMap?.get("code") == pkgCode) {
                        found = true
                        break
                    }
                }
            } catch (_: Exception) { }
            delay(200)
        }

        // If GET never found the offer, still proceed to POST
        // (the offer may appear after the POST triggers server-side allocation)

        // ── Step 2: POST until success or 402 ───────────────────────────────
        var postAttempts = 0
        while (postAttempts < maxAttempts) {
            postAttempts++
            try {
                delay(200)
                val r = api.activateShake(auth, msisdn, ActivateProductRequest(pkgCode))
                if (r.code() == 401) return null      // session expired
                if (r.code() == 402) return r          // insufficient balance — stop
                if (r.isSuccessful || r.code() in 200..202) return r  // success
                // Any other error → retry
            } catch (_: Exception) { }
            delay(200)
        }

        // Exhausted retries — return last attempt result (fall through to caller)
        return api.activateShake(auth, msisdn, ActivateProductRequest(pkgCode))
    }

    /** Extract the Arabic message from error JSON: {"message":{"ar":"..."}} */
    private fun extractArabicMessage(json: String): String {
        return try {
            Regex(""""ar"\s*:\s*"([^"]+)"""").find(json)?.groupValues?.get(1) ?: ""
        } catch (e: Exception) { "" }
    }

    fun resetState() { _activationState.value = OfferActivationState.Idle }
}
