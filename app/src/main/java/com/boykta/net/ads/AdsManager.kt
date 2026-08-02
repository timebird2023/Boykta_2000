package com.boykta.net.ads

import android.app.Activity
import com.startapp.sdk.adsbase.StartAppAd
import com.startapp.sdk.adsbase.StartAppSDK

/**
 * Wrapper around the Start.io SDK.
 * Call [init] once from MainActivity.
 * Call [preload] while the user browses offers.
 * Call [showInterstitial] after the user closes the success modal.
 */
object AdsManager {

    private const val APP_ID = "207841284"

    private var interstitialAd: StartAppAd? = null

    fun init(activity: Activity) {
        StartAppSDK.init(activity, APP_ID, false)
        StartAppSDK.setUserConsent(activity, "pas", System.currentTimeMillis() / 1000, true)
    }

    /** Pre-load the interstitial in the background so it's ready instantly. */
    fun preload(activity: Activity) {
        interstitialAd = StartAppAd(activity)
        interstitialAd?.loadAd()
    }

    /** Call this after the user dismisses the success modal. */
    fun showInterstitial(activity: Activity, onClosed: (() -> Unit)? = null) {
        val ad = interstitialAd ?: StartAppAd(activity)
        ad.showAd()
        // Pre-load the next ad immediately for the subsequent activation
        interstitialAd = StartAppAd(activity)
        interstitialAd?.loadAd()
        onClosed?.invoke()
    }
}
