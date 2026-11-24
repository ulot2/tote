package app.tote.ui.nav

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import app.tote.feature.editor.EditorRoute as EditorScreenRoute
import app.tote.feature.settings.SettingsRoute as SettingsScreenRoute
import app.tote.feature.profiles.ProfilesRoute as ProfilesScreenRoute
import app.tote.feature.learned.LearnedRoute as LearnedScreenRoute
import app.tote.feature.vault.VaultRoute as VaultScreenRoute

@Composable
fun ToteNavHost(modifier: Modifier = Modifier) {
    val nav = rememberNavController()
    SharedTransitionLayout(modifier) {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            NavHost(
                navController = nav,
                startDestination = VaultRoute,
                enterTransition = { fadeIn(tween(220)) },
                exitTransition = { fadeOut(tween(180)) },
                popEnterTransition = { fadeIn(tween(220)) },
                popExitTransition = { fadeOut(tween(180)) },
            ) {
                composable<VaultRoute> {
                    CompositionLocalProvider(LocalNavAnimatedScope provides this) {
                        VaultScreenRoute(
                            onOpenEditor = { nav.navigate(it) { launchSingleTop = true } },
                            onOpenSettings = { nav.navigate(SettingsRoute) { launchSingleTop = true } },
                            onManageProfiles = { nav.navigate(ProfilesRoute) { launchSingleTop = true } },
                        )
                    }
                }
                composable<EditorRoute> { backStackEntry ->
                    val route = backStackEntry.toRoute<EditorRoute>()
                    CompositionLocalProvider(LocalNavAnimatedScope provides this) {
                        EditorScreenRoute(route = route, onDone = { nav.popBackStack(VaultRoute, inclusive = false) })
                    }
                }
                composable<SettingsRoute>(
                    enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(280)) + fadeIn() },
                    popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(240)) + fadeOut() },
                ) {
                    SettingsScreenRoute(
                        onBack = { nav.popBackStack() },
                        onOpenProfiles = { nav.navigate(ProfilesRoute) { launchSingleTop = true } },
                        onOpenLearned = { nav.navigate(LearnedRoute) { launchSingleTop = true } },
                    )
                }
                composable<ProfilesRoute>(
                    enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(280)) + fadeIn() },
                    popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(240)) + fadeOut() },
                ) { ProfilesScreenRoute(onBack = { nav.popBackStack() }) }
                composable<LearnedRoute>(
                    enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(280)) + fadeIn() },
                    popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(240)) + fadeOut() },
                ) { LearnedScreenRoute(onBack = { nav.popBackStack() }) }
            }
        }
    }
}
