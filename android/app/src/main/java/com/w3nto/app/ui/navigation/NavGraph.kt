package com.w3nto.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.w3nto.app.data.repository.AuthRepository
import com.w3nto.app.ui.components.BottomNavBar
import com.w3nto.app.ui.screens.auth.ConnectWalletScreen
import com.w3nto.app.ui.screens.discover.DiscoverScreen
import com.w3nto.app.ui.screens.favorites.FavoritesScreen
import com.w3nto.app.ui.screens.landing.LandingScreen
import com.w3nto.app.ui.screens.profile.PublicProfileScreen
import com.w3nto.app.wallet.WalletManager
import kotlinx.coroutines.flow.StateFlow

private val bottomNavRoutes = setOf(
    Screen.Discover.route,
    Screen.Favorites.route
)

@Composable
fun NavGraph(
    walletManager: WalletManager,
    authRepository: AuthRepository,
    activityResultSender: ActivityResultSender,
    pendingDeepLinkRoute: StateFlow<String?> = kotlinx.coroutines.flow.MutableStateFlow(null),
    onDeepLinkConsumed: () -> Unit = {}
) {
    val navController = rememberNavController()
    val firebaseUser by authRepository.authStateFlow().collectAsState(initial = authRepository.currentUser)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val deepLinkRoute by pendingDeepLinkRoute.collectAsState()

    val startDestination = if (firebaseUser != null) {
        Screen.Discover.route
    } else {
        Screen.Landing.route
    }

    // Auto-navigate to Discover when auth completes while on Landing/ConnectWallet
    LaunchedEffect(firebaseUser) {
        if (firebaseUser != null) {
            val route = navController.currentDestination?.route
            if (route == Screen.Landing.route || route == Screen.ConnectWallet.route) {
                navController.navigate(Screen.Discover.route) {
                    popUpTo(Screen.Landing.route) { inclusive = true }
                }
            }
        }
    }

    // Handle deep link navigation — delay one frame to ensure NavHost graph is initialized
    LaunchedEffect(deepLinkRoute) {
        val route = deepLinkRoute ?: return@LaunchedEffect
        // Wait for NavHost to complete its first composition
        kotlinx.coroutines.delay(100)
        try {
            navController.navigate(route) {
                launchSingleTop = true
            }
        } catch (e: Exception) {
            android.util.Log.e("NavGraph", "Deep link navigation failed: ${e.message}")
        }
        onDeepLinkConsumed()
    }

    Scaffold(
        bottomBar = {
            if (currentRoute in bottomNavRoutes) {
                BottomNavBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(Screen.Discover.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Landing.route) {
                LandingScreen(
                    onConnectWallet = {
                        navController.navigate(Screen.ConnectWallet.route)
                    }
                )
            }

            composable(Screen.ConnectWallet.route) {
                ConnectWalletScreen(
                    walletManager = walletManager,
                    activityResultSender = activityResultSender,
                    onConnected = {
                        navController.navigate(Screen.Discover.route) {
                            popUpTo(Screen.Landing.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Discover.route) {
                DiscoverScreen(
                    onProfileClick = { username ->
                        navController.navigate(Screen.PublicProfile().createRoute(username))
                    }
                )
            }

            composable(Screen.Favorites.route) {
                FavoritesScreen(
                    onPageClick = { username, pageSlug ->
                        navController.navigate(Screen.PublicProfile().createRoute(username, pageSlug))
                    }
                )
            }

            composable(
                route = "profile/{username}?page={pageSlug}",
                arguments = listOf(
                    navArgument("username") { type = NavType.StringType },
                    navArgument("pageSlug") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val username = backStackEntry.arguments?.getString("username") ?: return@composable
                val pageSlug = backStackEntry.arguments?.getString("pageSlug")
                PublicProfileScreen(
                    username = username,
                    pageSlug = pageSlug,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
