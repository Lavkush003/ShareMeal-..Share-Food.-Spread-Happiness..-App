package com.sharemeal.app.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sharemeal.app.R
import com.sharemeal.app.core.session.SessionViewModel
import com.sharemeal.app.core.ui.Spacing
import com.sharemeal.app.core.ui.components.FeaturePlaceholderScreen
import com.sharemeal.app.core.ui.components.MockModeBanner
import com.sharemeal.app.domain.model.UserRole
import com.sharemeal.app.feature.authentication.ForgotPasswordScreen
import com.sharemeal.app.feature.authentication.LoginScreen
import com.sharemeal.app.feature.authentication.RoleSelectionScreen
import com.sharemeal.app.feature.authentication.WelcomeScreen
import com.sharemeal.app.feature.dashboard.DonorDashboardScreen
import com.sharemeal.app.feature.dashboard.NgoDashboardScreen
import com.sharemeal.app.feature.onboarding.OnboardingScreen
import com.sharemeal.app.feature.profile.ProfileScreen
import com.sharemeal.app.feature.registration.DonorRegistrationScreen
import com.sharemeal.app.feature.registration.NgoRegistrationScreen
import com.sharemeal.app.feature.splash.SplashDestination
import com.sharemeal.app.feature.splash.SplashScreen

@Composable
fun ShareMealApp(
    isMockMode: Boolean,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = Routes.SPLASH,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(Routes.SPLASH) {
                    SplashScreen(
                        onDestination = { destination ->
                            val route = when (destination) {
                                SplashDestination.ONBOARDING -> Routes.ONBOARDING
                                SplashDestination.WELCOME -> Routes.WELCOME
                                SplashDestination.DONOR_DASHBOARD -> Routes.DONOR_DASHBOARD
                                SplashDestination.NGO_DASHBOARD -> Routes.NGO_DASHBOARD
                            }
                            navController.navigate(route) {
                                popUpTo(Routes.SPLASH) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    )
                }

                composable(Routes.ONBOARDING) {
                    OnboardingScreen(
                        onFinished = {
                            navController.navigate(Routes.WELCOME) {
                                popUpTo(Routes.ONBOARDING) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    )
                }

                composable(Routes.WELCOME) {
                    WelcomeScreen(
                        onLogin = { navController.navigate(Routes.LOGIN) },
                        onRegister = { navController.navigate(Routes.REGISTER) }
                    )
                }

                composable(Routes.LOGIN) {
                    LoginScreen(
                        onLoggedIn = { role -> navController.navigateToDashboard(role) },
                        onForgotPassword = {
                            navController.navigate(Routes.FORGOT_PASSWORD)
                        },
                        onRegister = { navController.navigate(Routes.REGISTER) },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Routes.FORGOT_PASSWORD) {
                    ForgotPasswordScreen(
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Routes.REGISTER) {
                    RoleSelectionScreen(
                        onContinue = { role ->
                            val route = if (role == UserRole.DONOR) {
                                Routes.REGISTER_DONOR
                            } else {
                                Routes.REGISTER_NGO
                            }
                            navController.navigate(route)
                        },
                        onLogin = { navController.navigateToLoginFromRegistration() },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Routes.REGISTER_DONOR) {
                    DonorRegistrationScreen(
                        onRegistered = {
                            navController.navigateToDashboard(UserRole.DONOR)
                        },
                        onLogin = { navController.navigateToLoginFromRegistration() },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Routes.REGISTER_NGO) {
                    NgoRegistrationScreen(
                        onRegistered = {
                            navController.navigateToDashboard(UserRole.NGO)
                        },
                        onLogin = { navController.navigateToLoginFromRegistration() },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Routes.DONOR_DASHBOARD) {
                    SessionRoute(
                        requiredRole = UserRole.DONOR,
                        navController = navController
                    ) {
                        DonorDashboardScreen(
                            onAddDonation = { navController.navigate(Routes.DONOR_ADD_DONATION) },
                            onMyDonations = { navController.navigate(Routes.DONOR_DONATIONS) },
                            onHistory = { navController.navigate(Routes.DONOR_HISTORY) },
                            onNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                            onProfile = { navController.navigate(Routes.PROFILE) },
                            onSettings = { navController.navigate(Routes.SETTINGS) }
                        )
                    }
                }

                composable(Routes.NGO_DASHBOARD) {
                    SessionRoute(
                        requiredRole = UserRole.NGO,
                        navController = navController
                    ) {
                        NgoDashboardScreen(
                            onDiscovery = { navController.navigate(Routes.NGO_DISCOVERY) },
                            onRequests = { navController.navigate(Routes.NGO_REQUESTS) },
                            onPickups = { navController.navigate(Routes.NGO_PICKUPS) },
                            onHistory = { navController.navigate(Routes.NGO_HISTORY) },
                            onNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                            onProfile = { navController.navigate(Routes.PROFILE) },
                            onSettings = { navController.navigate(Routes.SETTINGS) }
                        )
                    }
                }

                composable(Routes.DONOR_ADD_DONATION) {
                    SessionRoute(
                        requiredRole = UserRole.DONOR,
                        navController = navController
                    ) {
                        FeaturePlaceholderScreen(
                            title = stringResource(R.string.placeholder_add_donation_title),
                            description = stringResource(R.string.placeholder_add_donation_body),
                            onBack = { navController.popBackStack() }
                        )
                    }
                }

                composable(Routes.DONOR_DONATIONS) {
                    SessionRoute(
                        requiredRole = UserRole.DONOR,
                        navController = navController
                    ) {
                        FeaturePlaceholderScreen(
                            title = stringResource(R.string.donor_action_donations_title),
                            description = stringResource(R.string.donor_action_donations_body),
                            onBack = { navController.popBackStack() }
                        )
                    }
                }

                composable(Routes.NGO_DISCOVERY) {
                    SessionRoute(
                        requiredRole = UserRole.NGO,
                        navController = navController
                    ) {
                        FeaturePlaceholderScreen(
                            title = stringResource(R.string.placeholder_discovery_title),
                            description = stringResource(R.string.placeholder_discovery_body),
                            onBack = { navController.popBackStack() }
                        )
                    }
                }

                composable(Routes.NGO_REQUESTS) {
                    SessionRoute(
                        requiredRole = UserRole.NGO,
                        navController = navController
                    ) {
                        FeaturePlaceholderScreen(
                            title = stringResource(R.string.placeholder_requests_title),
                            description = stringResource(R.string.placeholder_requests_body),
                            onBack = { navController.popBackStack() }
                        )
                    }
                }

                composable(Routes.NGO_PICKUPS) {
                    SessionRoute(
                        requiredRole = UserRole.NGO,
                        navController = navController
                    ) {
                        FeaturePlaceholderScreen(
                            title = stringResource(R.string.placeholder_pickups_title),
                            description = stringResource(R.string.placeholder_pickups_body),
                            onBack = { navController.popBackStack() }
                        )
                    }
                }

                composable(Routes.DONOR_HISTORY) {
                    SessionRoute(
                        requiredRole = UserRole.DONOR,
                        navController = navController
                    ) {
                        HistoryPlaceholderScreen(onBack = { navController.popBackStack() })
                    }
                }

                composable(Routes.NGO_HISTORY) {
                    SessionRoute(
                        requiredRole = UserRole.NGO,
                        navController = navController
                    ) {
                        HistoryPlaceholderScreen(onBack = { navController.popBackStack() })
                    }
                }

                composable(Routes.NOTIFICATIONS) {
                    SessionRoute(
                        requiredRole = null,
                        navController = navController
                    ) {
                        FeaturePlaceholderScreen(
                            title = stringResource(R.string.placeholder_notifications_title),
                            description = stringResource(R.string.placeholder_notifications_body),
                            onBack = { navController.popBackStack() }
                        )
                    }
                }

                composable(Routes.PROFILE) {
                    SessionRoute(
                        requiredRole = null,
                        navController = navController
                    ) {
                        ProfileScreen(
                            onBack = { navController.popBackStack() },
                            onLoggedOut = {
                                navController.navigateToWelcomeAndClear()
                            }
                        )
                    }
                }

                composable(Routes.SETTINGS) {
                    SessionRoute(
                        requiredRole = null,
                        navController = navController
                    ) {
                        FeaturePlaceholderScreen(
                            title = stringResource(R.string.placeholder_settings_title),
                            description = stringResource(R.string.placeholder_settings_body),
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }

            if (isMockMode) {
                MockModeBanner(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = Spacing.lg)
                )
            }
        }
    }
}

@Composable
private fun SessionRoute(
    requiredRole: UserRole?,
    navController: NavHostController,
    sessionViewModel: SessionViewModel = hiltViewModel(),
    content: @Composable () -> Unit
) {
    val session by sessionViewModel.session.collectAsStateWithLifecycle()
    val isRestored by sessionViewModel.isSessionRestored.collectAsStateWithLifecycle()
    val currentSession = session
    val isAuthorized = isRestored &&
        currentSession != null &&
        (requiredRole == null || currentSession.role == requiredRole)

    LaunchedEffect(isRestored, currentSession, requiredRole) {
        if (!isRestored) return@LaunchedEffect
        when {
            currentSession == null -> navController.navigateToWelcomeAndClear()
            requiredRole != null && currentSession.role != requiredRole -> {
                navController.navigateToDashboard(currentSession.role)
            }
        }
    }

    if (isAuthorized) {
        content()
    }
}

@Composable
private fun HistoryPlaceholderScreen(onBack: () -> Unit) {
    FeaturePlaceholderScreen(
        title = stringResource(R.string.placeholder_history_title),
        description = stringResource(R.string.placeholder_history_body),
        onBack = onBack
    )
}

private fun dashboardRoute(role: UserRole): String = when (role) {
    UserRole.DONOR -> Routes.DONOR_DASHBOARD
    UserRole.NGO -> Routes.NGO_DASHBOARD
}

private fun NavHostController.navigateToDashboard(role: UserRole) {
    navigate(dashboardRoute(role)) {
        popUpTo(Routes.WELCOME) { inclusive = true }
        launchSingleTop = true
    }
}

private fun NavHostController.navigateToLoginFromRegistration() {
    navigate(Routes.LOGIN) {
        popUpTo(Routes.WELCOME) { inclusive = false }
        launchSingleTop = true
    }
}

private fun NavHostController.navigateToWelcomeAndClear() {
    val graphId = graph.id
    navigate(Routes.WELCOME) {
        popUpTo(graphId) { inclusive = true }
        launchSingleTop = true
    }
}
