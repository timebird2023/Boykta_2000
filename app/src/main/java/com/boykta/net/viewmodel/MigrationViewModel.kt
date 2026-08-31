package com.boykta.net.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.boykta.net.data.api.ApiClient
import com.boykta.net.data.local.TokenStorage
import com.boykta.net.data.models.MigrationExecuteRequest
import com.boykta.net.data.models.MigrationOptionItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MigrationUiState(
    val isLoading: Boolean = false,
    val isExecuting: Boolean = false,
    val options: List<MigrationOptionItem> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val selectedOption: MigrationOptionItem? = null
)

class MigrationViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenStorage = TokenStorage(application)
    private val api = ApiClient.api

    private val _uiState = MutableStateFlow(MigrationUiState())
    val uiState: StateFlow<MigrationUiState> = _uiState.asStateFlow()

    init {
        loadMigrationOptions()
    }

    fun loadMigrationOptions() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val token = tokenStorage.accessToken.firstOrNull() ?: ""
            val msisdn = tokenStorage.msisdn.firstOrNull() ?: ""

            if (token.isBlank() || msisdn.isBlank()) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "انتهت صلاحية الجلسة. سجّل الدخول مجدداً.") }
                return@launch
            }

            try {
                val resp = api.getMigrationOptions("Bearer $token", msisdn)
                if (resp.isSuccessful) {
                    val list = resp.body()?.data ?: emptyList()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            options = list,
                            errorMessage = if (list.isEmpty()) "لا توجد خيارات تحويل متاحة لخطك حالياً." else null
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "تعذّر جلب خيارات تحويل الشريحة (${resp.code()})."
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "تعذّر الاتصال بالخادم. تأكد من اتصال الإنترنت."
                    )
                }
            }
        }
    }

    fun selectOptionForConfirm(option: MigrationOptionItem) {
        _uiState.update { it.copy(selectedOption = option) }
    }

    fun dismissConfirm() {
        _uiState.update { it.copy(selectedOption = null) }
    }

    fun executeMigration(optionId: String, targetPlanName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isExecuting = true, errorMessage = null, selectedOption = null) }
            val token = tokenStorage.accessToken.firstOrNull() ?: ""
            val msisdn = tokenStorage.msisdn.firstOrNull() ?: ""

            if (token.isBlank() || msisdn.isBlank()) {
                _uiState.update { it.copy(isExecuting = false, errorMessage = "انتهت صلاحية الجلسة.") }
                return@launch
            }

            try {
                val resp = api.executeMigration("Bearer $token", msisdn, MigrationExecuteRequest(optionId))
                if (resp.isSuccessful || resp.code() in 200..202) {
                    _uiState.update {
                        it.copy(
                            isExecuting = false,
                            successMessage = "تم تحويل شريحتك إلى $targetPlanName بنجاح! 🎉 قد يستغرق التطبيق بضع دقائق ليظهر على خطك."
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isExecuting = false,
                            errorMessage = "تعذّر تنفيذ التحويل (${resp.code()}). تحقق من رصيدك أو حاول لاحقاً."
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExecuting = false,
                        errorMessage = "تعذّر الاتصال بالخادم."
                    )
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
