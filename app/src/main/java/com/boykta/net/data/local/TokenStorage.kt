package com.boykta.net.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "boykta_prefs")

class TokenStorage(private val context: Context) {

    companion object {
        private val KEY_ACCESS_TOKEN   = stringPreferencesKey("access_token")
        private val KEY_REFRESH_TOKEN  = stringPreferencesKey("refresh_token")
        private val KEY_MSISDN         = stringPreferencesKey("msisdn")
        private val KEY_PHONE_DISPLAY  = stringPreferencesKey("phone_display")
        private val KEY_ACCOUNT_NAME   = stringPreferencesKey("account_name")
        private val KEY_TOKEN_EXPIRY   = longPreferencesKey("token_expiry")
        private val KEY_ACCOUNTS_JSON  = stringPreferencesKey("accounts_json")
    }

    val accessToken: Flow<String?> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_ACCESS_TOKEN] }

    val refreshToken: Flow<String?> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_REFRESH_TOKEN] }

    val msisdn: Flow<String?> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_MSISDN] }

    val phoneDisplay: Flow<String?> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_PHONE_DISPLAY] }

    val accountName: Flow<String?> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_ACCOUNT_NAME] }

    val accountsJson: Flow<String?> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_ACCOUNTS_JSON] }

    suspend fun saveToken(
        accessToken: String,
        msisdn: String,
        phoneDisplay: String,
        accountName: String,
        refreshToken: String? = null,
        expiresInSeconds: Long = 3600L
    ) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ACCESS_TOKEN]  = accessToken
            prefs[KEY_MSISDN]        = msisdn
            prefs[KEY_PHONE_DISPLAY] = phoneDisplay
            prefs[KEY_ACCOUNT_NAME]  = accountName
            prefs[KEY_TOKEN_EXPIRY]  = System.currentTimeMillis() + expiresInSeconds * 1000
            if (refreshToken != null) prefs[KEY_REFRESH_TOKEN] = refreshToken
        }
    }

    /**
     * Update only the access and (optionally) refresh tokens after a silent token refresh.
     * Does not touch msisdn, accountName, or phoneDisplay.
     */
    suspend fun updateToken(newAccessToken: String, newRefreshToken: String?) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ACCESS_TOKEN] = newAccessToken
            prefs[KEY_TOKEN_EXPIRY] = System.currentTimeMillis() + 3600L * 1000
            if (newRefreshToken != null) prefs[KEY_REFRESH_TOKEN] = newRefreshToken
        }
    }

    suspend fun clearToken() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_ACCESS_TOKEN)
            prefs.remove(KEY_REFRESH_TOKEN)
            prefs.remove(KEY_MSISDN)
            prefs.remove(KEY_PHONE_DISPLAY)
            prefs.remove(KEY_ACCOUNT_NAME)
            prefs.remove(KEY_TOKEN_EXPIRY)
        }
    }

    /** Returns true if a token exists and has not expired. */
    suspend fun isTokenValid(): Boolean {
        return try {
            val prefs = context.dataStore.data
                .catch { emit(emptyPreferences()) }
                .first()
            val token  = prefs[KEY_ACCESS_TOKEN]
            val expiry = prefs[KEY_TOKEN_EXPIRY] ?: 0L
            !token.isNullOrBlank() && System.currentTimeMillis() < expiry
        } catch (e: Exception) {
            false
        }
    }

    suspend fun saveAccountsJson(json: String) {
        context.dataStore.edit { it[KEY_ACCOUNTS_JSON] = json }
    }
}
