package com.boykta.net.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.boykta.net.data.api.ApiClient
import com.boykta.net.data.local.TokenStorage
import com.boykta.net.data.models.*
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = false,
    val mainBalance: Double? = null,
    val productBalances: List<ProductBalance> = emptyList(),
    val subscriptionHistory: List<SubscriptionHistoryItem> = emptyList(),
    val errorMessage: String? = null,
    val accountName: String = "",
    val phoneDisplay: String = "",
    val msisdn: String = ""
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenStorage = TokenStorage(application)
    private val api = ApiClient.api

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init { loadAll() }

    fun loadAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            var token       = ""
            var msisdn      = ""
            var phoneDisplay = ""
            var accountName  = ""

            tokenStorage.accessToken.firstOrNull()?.let  { token       = it }
            tokenStorage.msisdn.firstOrNull()?.let       { msisdn      = it }
            tokenStorage.phoneDisplay.firstOrNull()?.let { phoneDisplay = it }
            tokenStorage.accountName.firstOrNull()?.let  { accountName  = it }

            if (token.isBlank() || msisdn.isBlank()) {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }

            val auth = "Bearer $token"

            try {
                // Fire all three requests in parallel
                val balanceDeferred  = async { api.getMainBalance(auth, msisdn) }
                val productsDeferred = async { api.getProductBalances(auth, msisdn) }
                val historyDeferred  = async { api.getSubscriptionHistory(auth, msisdn) }

                val balanceResp  = balanceDeferred.await()
                val productsResp = productsDeferred.await()
                val historyResp  = historyDeferred.await()

                // Real API response: { data: { products: [...] } }
                // (Old flat list was wrong — use ConnectedProductsData wrapper)
                val products = productsResp.body()?.data?.products ?: emptyList()

                _uiState.update { state ->
                    state.copy(
                        isLoading           = false,
                        mainBalance         = balanceResp.body()?.data?.mainBalance,
                        productBalances     = products,
                        subscriptionHistory = historyResp.body()?.data ?: emptyList(),
                        accountName         = accountName,
                        phoneDisplay        = phoneDisplay,
                        msisdn              = msisdn
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "تعذّر تحميل البيانات. اسحب للتحديث.") }
            }
        }
    }
}
