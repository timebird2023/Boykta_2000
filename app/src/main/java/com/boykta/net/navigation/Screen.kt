package com.boykta.net.navigation

sealed class Screen(val route: String) {
    object Splash         : Screen("splash")
    object Auth           : Screen("auth")
    object Dashboard      : Screen("dashboard")
    object Offers         : Screen("offers")
    object Flexy          : Screen("flexy")
    object FreeSms        : Screen("free_sms")
    object WalkWin        : Screen("walk_win")
    object Settings       : Screen("settings")
    object Mgm            : Screen("mgm")
    object PrivacyPolicy  : Screen("privacy_policy")
}
