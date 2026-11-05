package com.sharemeal.app.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sharemeal.app.R
import com.sharemeal.app.core.ui.Spacing
import com.sharemeal.app.core.ui.components.FeaturePlaceholderScreen
import com.sharemeal.app.core.ui.components.MockModeBanner
import com.sharemeal.app.feature.authentication.LoginPreviewScreen
import com.sharemeal.app.feature.dashboard.DonorDashboardScreen
import com.sharemeal.app.feature.dashboard.NgoDashboardScreen
import com.sharemeal.app.feature.onboarding.OnboardingScreen
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
                        onFinished = {
                            navController.navigate(Routes.ONBOARDING) {
                                popUpTo(Routes.SPLASH) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Routes.ONBOARDING) {
                    OnboardingScreen(
                        onGetStarted = {
                            navController.navigate(Routes.LOGIN) {
                                popUpTo(Routes.ONBOARDING) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Routes.LOGIN) {
                    LoginPreviewScreen(
                        onDonorPreview = {
                            navController.navigateToDashboard(Routes.DONOR_DASHBOARD)
                        },
                        onNgoPreview = {
                            navController.navigateToDashboard(Routes.NGO_DASHBOARD)
                        },
                        onRegister = {
                            navController.navigate(Routes.REGISTER)
                        }
                    )
                }

                composable(Routes.REGISTER) {
                    FeaturePlaceholderScreen(
                        title = stringResource(R.string.register_title),
                        description = stringResource(R.string.register_body),
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Routes.DONOR_DASHBOARD) {
                    DonorDashboardScreen(
                        onAddDonation = { navController.navigate(Routes.DONOR_ADD_DONATION) },
                        onMyDonations = { navController.navigate(Routes.DONOR_DONATIONS) },
                        onHistory = { navController.navigate(Routes.DONOR_HISTORY) },
                        onNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                        onProfile = { navController.navigate(Routes.PROFILE) },
                        onSettings = { navController.navigate(Routes.SETTINGS) }
                    )
                }

                composable(Routes.NGO_DASHBOARD) {
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

                composable(Routes.DONOR_ADD_DONATION) {
                    FeaturePlaceholderScreen(
                        title = stringResource(R.string.placeholder_add_donation_title),
                        description = stringResource(R.string.placeholder_add_donation_body),
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Routes.DONOR_DONATIONS) {
                    FeaturePlaceholderScreen(
                        title = stringResource(R.string.donor_action_donations_title),
                        description = stringResource(R.string.donor_action_donations_body),
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Routes.NGO_DISCOVERY) {
                    FeaturePlaceholderScreen(
                        title = stringResource(R.string.placeholder_discovery_title),
                        description = stringResource(R.string.placeholder_discovery_body),
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Routes.NGO_REQUESTS) {
                    FeaturePlaceholderScreen(
                        title = stringResource(R.string.placeholder_requests_title),
                        description = stringResource(R.string.placeholder_requests_body),
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Routes.NGO_PICKUPS) {
                    FeaturePlaceholderScreen(
                        title = stringResource(R.string.placeholder_pickups_title),
                        description = stringResource(R.string.placeholder_pickups_body),
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Routes.DONOR_HISTORY) {
                    HistoryPlaceholderScreen(onBack = { navController.popBackStack() })
                }

                composable(Routes.NGO_HISTORY) {
                    HistoryPlaceholderScreen(onBack = { navController.popBackStack() })
                }

                composable(Routes.NOTIFICATIONS) {
                    FeaturePlaceholderScreen(
                        title = stringResource(R.string.placeholder_notifications_title),
                        description = stringResource(R.string.placeholder_notifications_body),
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Routes.PROFILE) {
                    FeaturePlaceholderScreen(
                        title = stringResource(R.string.placeholder_profile_title),
                        description = stringResource(R.string.placeholder_profile_body),
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Routes.SETTINGS) {
                    FeaturePlaceholderScreen(
                        title = stringResource(R.string.placeholder_settings_title),
                        description = stringResource(R.string.placeholder_settings_body),
                        onBack = { navController.popBackStack() }
                    )
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
private fun HistoryPlaceholderScreen(onBack: () -> Unit) {
    FeaturePlaceholderScreen(
        title = stringResource(R.string.placeholder_history_title),
        description = stringResource(R.string.placeholder_history_body),
        onBack = onBack
    )
}

private fun NavHostController.navigateToDashboard(route: String) {
    navigate(route) {
        popUpTo(Routes.LOGIN) { inclusive = true }
        launchSingleTop = true
    }
}
