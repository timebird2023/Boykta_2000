package com.boykta.net.data.api

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * App-wide singleton that emits a one-shot event when the session is fully expired
 * (i.e. the refresh token itself is invalid — happens when the Facebook bot logs in
 * with the same phone number and invalidates all existing tokens).
 *
 * Observe this in NavGraph / MainActivity and navigate to Auth when it fires.
 */
object SessionManager {
    private val _sessionExpired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionExpired: SharedFlow<Unit> = _sessionExpired.asSharedFlow()

    /** Call from ApiClient's Authenticator when refresh fails — token is truly dead. */
    fun notifySessionExpired() {
        _sessionExpired.tryEmit(Unit)
    }
}
