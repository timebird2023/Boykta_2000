package com.boykta.net.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.boykta.net.data.api.ApiClient
import com.boykta.net.data.local.TokenStorage
import com.boykta.net.data.models.OtpRegistrationBody
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class AuthUiState {
    object Idle         : AuthUiState()
    object Loading      : AuthUiState()
    object OtpSent      : AuthUiState()
    object Success      : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenStorage = TokenStorage(application)
    private val api = ApiClient.api

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // MSISDN derived from the entered phone — kept for the verify step
    private var pendingMsisdn = ""

    /**
     * Convert a local 10-digit number (07XXXXXXXX) to the international
     * format required by the Djezzy API (2137XXXXXXXX).
     */
    private fun toMsisdn(phone: String): String {
        val digits = phone.filter { it.isDigit() }
        return when {
            digits.startsWith("213") -> digits
            digits.startsWith("0")   -> "213${digits.drop(1)}"
            else                     -> "213$digits"
        }
    }

    fun requestOtp(phone: String) {
        if (phone.filter { it.isDigit() }.length < 10) {
            _uiState.value = AuthUiState.Error("رقم الهاتف يجب أن يتكون من 10 أرقام")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                pendingMsisdn = toMsisdn(phone)
                // Params go as query parameters; body is the consent JSON
                val response = api.requestOtp(
                    msisdn   = pendingMsisdn,
                    clientId = ApiClient.CLIENT_ID,
                    scope    = ApiClient.SCOPE_OTP,
                    body     = OtpRegistrationBody()
                )
                if (response.isSuccessful || response.code() in 200..201) {
                    _uiState.value = AuthUiState.OtpSent
                } else {
                    _uiState.value = AuthUiState.Error("فشل إرسال رمز التحقق (${response.code()}). أعد المحاولة.")
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error("تعذّر الاتصال بالخادم. تحقق من الإنترنت.")
            }
        }
    }

    fun verifyOtp(otp: String, accountName: String, phoneDisplay: String) {
        if (otp.filter { it.isDigit() }.length < 4) {
            _uiState.value = AuthUiState.Error("رمز التحقق غير صحيح")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                // Field 'mobileNumber' (not 'msisdn') + scope='djezzyAppV2' — confirmed from Python
                val response = api.verifyOtp(
                    otp          = otp,
                    msisdn       = pendingMsisdn,
                    scope        = ApiClient.SCOPE_DJEZZY,
                    clientId     = ApiClient.CLIENT_ID,
                    clientSecret = ApiClient.CLIENT_SECRET,
                    grantType    = ApiClient.GRANT_TYPE
                )
                if (response.isSuccessful) {
                    val body = response.body()
                    val token = body?.accessToken
                    if (!token.isNullOrBlank()) {
                        tokenStorage.saveToken(
                            accessToken      = token,
                            msisdn           = pendingMsisdn,
                            phoneDisplay     = phoneDisplay,
                            accountName      = accountName.ifBlank { phoneDisplay },
                            refreshToken     = body.refreshToken,    // save for auto-refresh
                            expiresInSeconds = body.expiresIn ?: 3600L
                        )
                        _uiState.value = AuthUiState.Success
                    } else {
                        _uiState.value = AuthUiState.Error("استجابة غير متوقعة من الخادم.")
                    }
                } else {
                    _uiState.value = AuthUiState.Error("رمز التحقق غير صحيح أو منتهي الصلاحية.")
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error("تعذّر الاتصال بالخادم.")
            }
        }
    }

    fun resetState() { _uiState.value = AuthUiState.Idle }
}
