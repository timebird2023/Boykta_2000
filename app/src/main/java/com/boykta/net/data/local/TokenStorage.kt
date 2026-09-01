package com.boykta.net.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "boykta_prefs")

/** Represents one saved Djezzy account (max 5). */
data class SavedAccount(
    val msisdn: String,
    val phoneDisplay: String,
    val accountName: String,
    val accessToken: String,
    val refreshToken: String?,
    val tokenExpiry: Long
)

class TokenStorage(private val context: Context) {

    companion object {
        private val KEY_ACCESS_TOKEN   = stringPreferencesKey("access_token")
        private val KEY_REFRESH_TOKEN  = stringPreferencesKey("refresh_token")
        private val KEY_MSISDN         = stringPreferencesKey("msisdn")
        private val KEY_PHONE_DISPLAY  = stringPreferencesKey("phone_display")
        private val KEY_ACCOUNT_NAME   = stringPreferencesKey("account_name")
        private val KEY_TOKEN_EXPIRY   = longPreferencesKey("token_expiry")
        private val KEY_ACCOUNTS_JSON  = stringPreferencesKey("accounts_json")
        private val KEY_WALK_LAST_ACT  = longPreferencesKey("walk_last_activation")
        private val KEY_NET_SVC_JSON   = stringPreferencesKey("net_svc_states")
        private val KEY_MGM_INVITES    = stringPreferencesKey("mgm_invites_json")
        private val KEY_DARK_THEME     = booleanPreferencesKey("dark_theme")

        const val MAX_ACCOUNTS = 5
        private val gson = Gson()
    }

    // ── Active account flows ──────────────────────────────────────────────────

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

    // ── Multi-account helpers ─────────────────────────────────────────────────

    /** Returns the list of all saved accounts (up to MAX_ACCOUNTS). */
    suspend fun getAllAccounts(): List<SavedAccount> {
        return try {
            val prefs = context.dataStore.data.catch { emit(emptyPreferences()) }.first()
            val json  = prefs[KEY_ACCOUNTS_JSON] ?: return emptyList()
            val type  = object : TypeToken<List<SavedAccount>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }

    /** Save or update an account in the multi-account list, and make it active. */
    suspend fun saveAccount(
        accessToken: String,
        msisdn: String,
        phoneDisplay: String,
        accountName: String,
        refreshToken: String? = null,
        expiresInSeconds: Long = 3600L
    ) {
        val accounts = getAllAccounts().toMutableList()
        val newAccount = SavedAccount(
            msisdn       = msisdn,
            phoneDisplay = phoneDisplay,
            accountName  = accountName.ifBlank { phoneDisplay },
            accessToken  = accessToken,
            refreshToken = refreshToken,
            tokenExpiry  = System.currentTimeMillis() + expiresInSeconds * 1000
        )
        val idx = accounts.indexOfFirst { it.msisdn == msisdn }
        if (idx >= 0) {
            accounts[idx] = newAccount
        } else {
            if (accounts.size >= MAX_ACCOUNTS) {
                accounts.removeAt(0)
            }
            accounts.add(newAccount)
        }
        context.dataStore.edit { prefs ->
            prefs[KEY_ACCOUNTS_JSON] = gson.toJson(accounts)
            prefs[KEY_ACCESS_TOKEN]  = accessToken
            prefs[KEY_MSISDN]        = msisdn
            prefs[KEY_PHONE_DISPLAY] = phoneDisplay
            prefs[KEY_ACCOUNT_NAME]  = accountName.ifBlank { phoneDisplay }
            prefs[KEY_TOKEN_EXPIRY]  = newAccount.tokenExpiry
            if (refreshToken != null) prefs[KEY_REFRESH_TOKEN] = refreshToken
        }
    }

    /** Convenience alias used by AuthViewModel / legacy code. */
    suspend fun saveToken(
        accessToken: String,
        msisdn: String,
        phoneDisplay: String,
        accountName: String,
        refreshToken: String? = null,
        expiresInSeconds: Long = 3600L
    ) = saveAccount(accessToken, msisdn, phoneDisplay, accountName, refreshToken, expiresInSeconds)

    /** Switch the active account to the given msisdn (must already be saved). */
    suspend fun switchAccount(msisdn: String): Boolean {
        val account = getAllAccounts().firstOrNull { it.msisdn == msisdn } ?: return false
        context.dataStore.edit { prefs ->
            prefs[KEY_ACCESS_TOKEN]  = account.accessToken
            prefs[KEY_MSISDN]        = account.msisdn
            prefs[KEY_PHONE_DISPLAY] = account.phoneDisplay
            prefs[KEY_ACCOUNT_NAME]  = account.accountName
            prefs[KEY_TOKEN_EXPIRY]  = account.tokenExpiry
            if (account.refreshToken != null) prefs[KEY_REFRESH_TOKEN] = account.refreshToken
            else prefs.remove(KEY_REFRESH_TOKEN)
        }
        return true
    }

    /** Remove an account by msisdn. If it was the active account, clear active state. */
    suspend fun removeAccount(msisdn: String) {
        val accounts = getAllAccounts().filter { it.msisdn != msisdn }
        context.dataStore.edit { prefs ->
            prefs[KEY_ACCOUNTS_JSON] = gson.toJson(accounts)
            val activeMsisdn = prefs[KEY_MSISDN]
            if (activeMsisdn == msisdn) {
                prefs.remove(KEY_ACCESS_TOKEN)
                prefs.remove(KEY_REFRESH_TOKEN)
                prefs.remove(KEY_MSISDN)
                prefs.remove(KEY_PHONE_DISPLAY)
                prefs.remove(KEY_ACCOUNT_NAME)
                prefs.remove(KEY_TOKEN_EXPIRY)
                accounts.firstOrNull()?.let { first ->
                    prefs[KEY_ACCESS_TOKEN]  = first.accessToken
                    prefs[KEY_MSISDN]        = first.msisdn
                    prefs[KEY_PHONE_DISPLAY] = first.phoneDisplay
                    prefs[KEY_ACCOUNT_NAME]  = first.accountName
                    prefs[KEY_TOKEN_EXPIRY]  = first.tokenExpiry
                    if (first.refreshToken != null) prefs[KEY_REFRESH_TOKEN] = first.refreshToken
                }
            }
        }
    }

    /** Update only the access and refresh tokens after a silent token refresh. */
    suspend fun updateToken(newAccessToken: String, newRefreshToken: String?) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ACCESS_TOKEN] = newAccessToken
            prefs[KEY_TOKEN_EXPIRY] = System.currentTimeMillis() + 3600L * 1000
            if (newRefreshToken != null) prefs[KEY_REFRESH_TOKEN] = newRefreshToken
            val msisdn = prefs[KEY_MSISDN] ?: return@edit
            val json   = prefs[KEY_ACCOUNTS_JSON] ?: return@edit
            val type   = object : TypeToken<List<SavedAccount>>() {}.type
            val list: MutableList<SavedAccount> = try { gson.fromJson(json, type) ?: mutableListOf() } catch (e: Exception) { mutableListOf() }
            val idx = list.indexOfFirst { it.msisdn == msisdn }
            if (idx >= 0) {
                list[idx] = list[idx].copy(
                    accessToken  = newAccessToken,
                    refreshToken = newRefreshToken ?: list[idx].refreshToken,
                    tokenExpiry  = System.currentTimeMillis() + 3600L * 1000
                )
                prefs[KEY_ACCOUNTS_JSON] = gson.toJson(list)
            }
        }
    }

    /** Clear only the ACTIVE account's token (does not remove from accounts list). */
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

    /** Returns true if a valid session exists (either valid access token OR refresh token). */
    suspend fun isTokenValid(): Boolean {
        return try {
            val prefs  = context.dataStore.data.catch { emit(emptyPreferences()) }.first()
            val token  = prefs[KEY_ACCESS_TOKEN]
            val refreshToken = prefs[KEY_REFRESH_TOKEN]
            val expiry = prefs[KEY_TOKEN_EXPIRY] ?: 0L
            (!token.isNullOrBlank() && System.currentTimeMillis() < expiry) || !refreshToken.isNullOrBlank()
        } catch (e: Exception) { false }
    }

    // ── Theme preference ─────────────────────────────────────────────────────

    /** true = dark theme (default), false = light theme */
    val isDarkTheme: Flow<Boolean> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_DARK_THEME] ?: true }

    suspend fun setDarkTheme(dark: Boolean) {
        context.dataStore.edit { it[KEY_DARK_THEME] = dark }
    }

    // ── Walk & Win ────────────────────────────────────────────────────────────

    val walkLastActivation: Flow<Long> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_WALK_LAST_ACT] ?: 0L }

    suspend fun saveWalkActivationTime(timeMs: Long) {
        context.dataStore.edit { it[KEY_WALK_LAST_ACT] = timeMs }
    }

    // ── Network service states ────────────────────────────────────────────────

    suspend fun getNetworkServiceStates(): Map<String, Boolean> {
        return try {
            val prefs = context.dataStore.data.catch { emit(emptyPreferences()) }.first()
            val json  = prefs[KEY_NET_SVC_JSON] ?: return emptyMap()
            val type  = object : TypeToken<Map<String, Boolean>>() {}.type
            gson.fromJson(json, type) ?: emptyMap()
        } catch (e: Exception) { emptyMap() }
    }

    suspend fun setNetworkServiceState(serviceId: String, enabled: Boolean) {
        val states = getNetworkServiceStates().toMutableMap()
        states[serviceId] = enabled
        context.dataStore.edit { it[KEY_NET_SVC_JSON] = gson.toJson(states) }
    }

    // ── MGM invitations local storage ─────────────────────────────────────────

    val mgmInvitesJson: Flow<String?> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_MGM_INVITES] }

    suspend fun saveMgmInvites(json: String) {
        context.dataStore.edit { it[KEY_MGM_INVITES] = json }
    }

    @Deprecated("Use saveAccount instead")
    suspend fun saveAccountsJson(json: String) {
        context.dataStore.edit { it[KEY_ACCOUNTS_JSON] = json }
    }
}
