package com.w3nto.app.ui.navigation

sealed class Screen(val route: String) {
    data object Landing : Screen("landing")
    data object ConnectWallet : Screen("connect_wallet")
    data object Discover : Screen("discover")
    data object Favorites : Screen("favorites")
    data object MyProfile : Screen("my_profile")
    data object Settings : Screen("settings")
    data class PublicProfile(
        val username: String = "{username}",
        val pageSlug: String? = null
    ) : Screen("profile/{username}?page={pageSlug}") {
        fun createRoute(username: String, pageSlug: String? = null): String {
            return if (pageSlug != null) {
                "profile/$username?page=$pageSlug"
            } else {
                "profile/$username"
            }
        }
    }
}
