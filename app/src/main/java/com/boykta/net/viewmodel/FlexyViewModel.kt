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

sealed class FlexyUiState {
    object Idle    : FlexyUiState()
    object Loading : FlexyUiState()
    object Success : FlexyUiState()
    data class Error(val message: String) : FlexyUiState()
}

data class FlexyHistoryState(
    val creditHistory: List<FlexyHistoryItem> = emptyList(),
    val dataHistory:   List<FlexyHistoryItem> = emptyList(),
    val isLoading: Boolean = false
)

class FlexyViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenStorage = TokenStorage(application)
    private val api = ApiClient.api

    private val _actionState = MutableStateFlow<FlexyUiState>(FlexyUiState.Idle)
    val actionState: StateFlow<FlexyUiState> = _actionState.asStateFlow()

    private val _historyState = MutableStateFlow(FlexyHistoryState())
    val historyState: StateFlow<FlexyHistoryState> = _historyState.asStateFlow()

    init { loadHistories() }

    fun loadHistories() {
        viewModelScope.launch(Dispatchers.IO) {
            _historyState.update { it.copy(isLoading = true) }
            val token  = tokenStorage.accessToken.firstOrNull() ?: return@launch
            val msisdn = tokenStorage.msisdn.firstOrNull() ?: return@launch
            val auth = "Bearer $token"
            try {
                val creditResp = api.getFlexyHistory(auth, msisdn, "CREDIT_SHARE")
                val dataResp   = api.getFlexyHistory(auth, msisdn, "DATA_SHARE")
                _historyState.update {
                    it.copy(
                        creditHistory = creditResp.body()?.data ?: emptyList(),
                        dataHistory   = dataResp.body()?.data   ?: emptyList(),
                        isLoading     = false
                    )
                }
            } catch (e: Exception) {
                _historyState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun transferCredit(receiverPhone: String, amount: String, pin: String) {
        val msisdnB = receiverPhone.filter { it.isDigit() }.let {
            if (it.startsWith("0")) "213${it.drop(1)}" else it
        }.toLongOrNull() ?: run {
            _actionState.value = FlexyUiState.Error("رقم المستلم غير صحيح"); return
        }
        val amountD = amount.toDoubleOrNull() ?: run {
            _actionState.value = FlexyUiState.Error("المبلغ غير صحيح"); return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _actionState.value = FlexyUiState.Loading
            val token  = tokenStorage.accessToken.firstOrNull() ?: return@launch
            val msisdn = tokenStorage.msisdn.firstOrNull() ?: return@launch
            try {
                val resp = api.transferCredit(
                    "Bearer $token", msisdn,
                    FlexyTransferRequest(msisdnB, amountD, pin)
                )
                handleResponse(resp)
            } catch (e: Exception) { _actionState.value = FlexyUiState.Error("تعذّر الاتصال بالخادم.") }
        }
    }

    fun transferData(receiverPhone: String) {
        val msisdnB = receiverPhone.filter { it.isDigit() }.let {
            if (it.startsWith("0")) "213${it.drop(1)}" else it
        }.toLongOrNull() ?: run {
            _actionState.value = FlexyUiState.Error("رقم المستلم غير صحيح"); return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _actionState.value = FlexyUiState.Loading
            val token  = tokenStorage.accessToken.firstOrNull() ?: return@launch
            val msisdn = tokenStorage.msisdn.firstOrNull() ?: return@launch
            try {
                val resp = api.transferData(
                    "Bearer $token", msisdn,
                    FlexyDataRequest(msisdnB, "TransferInternet1Go")
                )
                handleResponse(resp)
            } catch (e: Exception) { _actionState.value = FlexyUiState.Error("تعذّر الاتصال بالخادم.") }
        }
    }

    private fun handleResponse(resp: retrofit2.Response<ApiResponse<Any>>) {
        if (resp.isSuccessful || resp.code() in 200..201) {
            _actionState.value = FlexyUiState.Success
            loadHistories()
        } else {
            val ar = extractAr(resp.errorBody()?.string() ?: "")
            _actionState.value = FlexyUiState.Error(ar.ifBlank { "فشلت العملية (${resp.code()})." })
        }
    }

    private fun extractAr(json: String): String {
        return Regex(""""ar"\s*:\s*"([^"]+)"""").find(json)?.groupValues?.get(1) ?: ""
    }

    fun resetState() { _actionState.value = FlexyUiState.Idle }
}
