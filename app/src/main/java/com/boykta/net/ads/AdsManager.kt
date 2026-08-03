package com.boykta.net.ads

import android.app.Activity
import android.util.Log
import com.startapp.sdk.adsbase.StartAppAd
import com.startapp.sdk.adsbase.StartAppSDK

/**
 * Wrapper around the Start.io SDK.
 * Call [init] once from MainActivity.
 * Call [preload] while the user browses offers.
 * Call [showInterstitial] after the user closes the success modal.
 *
 * Every SDK call is wrapped in try/catch so that a Start.io failure never
 * crashes the host Activity — the app continues working without ads.
 */
object AdsManager {

    private const val TAG = "AdsManager"
    private const val APP_ID = "207841284"

    private var interstitialAd: StartAppAd? = null
    private var initialized = false

    fun init(activity: Activity) {
        try {
            StartAppSDK.init(activity, APP_ID, false)
            StartAppSDK.setUserConsent(activity, "pas", System.currentTimeMillis() / 1000, true)
            initialized = true
        } catch (e: Exception) {
            Log.e(TAG, "Start.io init failed — ads disabled", e)
        }
    }

    /** Pre-load the interstitial in the background so it's ready instantly. */
    fun preload(activity: Activity) {
        if (!initialized) return
        try {
            interstitialAd = StartAppAd(activity)
            interstitialAd?.loadAd()
        } catch (e: Exception) {
            Log.e(TAG, "preload failed", e)
        }
    }

    /** Call this after the user dismisses the success modal. */
    fun showInterstitial(activity: Activity, onClosed: (() -> Unit)? = null) {
        try {
            if (initialized) {
                val ad = interstitialAd ?: StartAppAd(activity)
                ad.showAd()
                // Pre-load the next ad immediately
                interstitialAd = StartAppAd(activity)
                interstitialAd?.loadAd()
            }
        } catch (e: Exception) {
            Log.e(TAG, "showInterstitial failed", e)
        } finally {
            onClosed?.invoke()
        }
    }
}
