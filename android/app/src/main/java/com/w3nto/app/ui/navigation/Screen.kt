package com.w3nto.app.ui.navigation

sealed class Screen(val route: String) {
    data object Landing : Screen("landing")
    data object ConnectWallet : Screen("connect_wallet")
    data object Discover : Screen("discover")
    data object Favorites : Screen("favorites")
    data object MyProfile : Screen("my_profile")
    data object Settings : Screen("settings")
    data object Onboarding : Screen("onboarding")
    data class PublicProfile(
        val slug: String = "{slug}"
    ) : Screen("profile/{slug}") {
        fun createRoute(slug: String): String = "profile/$slug"
    }
}
