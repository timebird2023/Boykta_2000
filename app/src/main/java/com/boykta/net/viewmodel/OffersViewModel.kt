package com.boykta.net.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.boykta.net.data.api.ApiClient
import com.boykta.net.data.local.TokenStorage
import com.boykta.net.data.models.*
import kotlinx.coroutines.Dispatchers
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
                    "shake" -> {
                        // Check eligibility first, then activate
                        api.checkShake(auth, msisdn)
                        api.activateShake(auth, msisdn, ActivateProductRequest(offer.packageCode))
                    }
                    else -> api.activateProduct(auth, msisdn, ActivateProductRequest(offer.packageCode))
                }

                if (response.isSuccessful || response.code() in 200..201) {
                    _activationState.value = OfferActivationState.Success
                } else {
                    val errorBody = response.errorBody()?.string() ?: ""
                    val ar = extractArabicMessage(errorBody)
                    _activationState.value = OfferActivationState.Error(
                        ar.ifBlank { "فشل التفعيل (${response.code()}). حاول مرة أخرى." }
                    )
                }
            } catch (e: Exception) {
                _activationState.value = OfferActivationState.Error("تعذّر الاتصال بالخادم.")
            }
        }
    }

    /** Extract the Arabic message from error JSON: {"message":{"ar":"..."}} */
    private fun extractArabicMessage(json: String): String {
        return try {
            val regex = Regex(""""ar"\s*:\s*"([^"]+)"""")
            regex.find(json)?.groupValues?.get(1) ?: ""
        } catch (e: Exception) { "" }
    }

    fun resetState() { _activationState.value = OfferActivationState.Idle }
}
