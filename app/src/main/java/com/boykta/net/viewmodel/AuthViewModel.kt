package com.boykta.net.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.boykta.net.data.api.ApiClient
import com.boykta.net.data.local.TokenStorage
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
        return if (digits.startsWith("0")) "213${digits.drop(1)}"
        else if (digits.startsWith("213")) digits
        else "213$digits"
    }

    fun requestOtp(phone: String) {
        if (phone.length < 10) {
            _uiState.value = AuthUiState.Error("رقم الهاتف يجب أن يتكون من 10 أرقام")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                pendingMsisdn = toMsisdn(phone)
                val response = api.requestOtp(
                    msisdn   = pendingMsisdn,
                    clientId = ApiClient.CLIENT_ID,
                    scope    = ApiClient.SCOPE_OTP
                )
                if (response.isSuccessful || response.code() == 200 || response.code() == 201) {
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
        if (otp.length < 4) {
            _uiState.value = AuthUiState.Error("رمز التحقق غير صحيح")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val response = api.verifyOtp(
                    otp          = otp,
                    msisdn       = pendingMsisdn,
                    clientId     = ApiClient.CLIENT_ID,
                    clientSecret = ApiClient.CLIENT_SECRET,
                    grantType    = ApiClient.GRANT_TYPE
                )
                if (response.isSuccessful) {
                    val token = response.body()?.accessToken
                    val expiresIn = response.body()?.expiresIn ?: 3600L
                    if (!token.isNullOrBlank()) {
                        tokenStorage.saveToken(
                            accessToken  = token,
                            msisdn       = pendingMsisdn,
                            phoneDisplay = phoneDisplay,
                            accountName  = accountName.ifBlank { phoneDisplay },
                            expiresInSeconds = expiresIn
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
