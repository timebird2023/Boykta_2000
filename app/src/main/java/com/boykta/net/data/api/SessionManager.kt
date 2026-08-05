package com.boykta.net.data.api

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * App-wide singleton that emits a one-shot event when the session is fully expired
 * (i.e. the refresh token itself is invalid — happens when the user logs in on another
 * device or when the token pair expires after ~24 h of inactivity).
 *
 * Observe sessionExpired in NavGraph and navigate to Auth when it fires.
 * Call consumeExpiredFlag() in AuthScreen to retrieve and clear the flag, then show
 * the Arabic warning message to the user.
 */
object SessionManager {
    private val _sessionExpired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionExpired: SharedFlow<Unit> = _sessionExpired.asSharedFlow()

    /** Set to true when notifySessionExpired() is called; cleared by consumeExpiredFlag(). */
    @Volatile private var _hadExpiry = false

    /**
     * Call from ApiClient's Authenticator when the refresh-token request itself returns
     * a non-2xx response — the token pair is truly dead.
     */
    fun notifySessionExpired() {
        _hadExpiry = true
        _sessionExpired.tryEmit(Unit)
    }

    /**
     * Called once by AuthScreen on launch.
     * Returns true (and clears the flag) if the user was redirected here due to token expiry,
     * so the screen can show the "عذراً، انتهت صلاحية جلستك" warning.
     */
    fun consumeExpiredFlag(): Boolean {
        val had = _hadExpiry
        _hadExpiry = false
        return had
    }
}
