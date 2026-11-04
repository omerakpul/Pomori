package com.pomori.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pomori.ui.components.ContentWidthLimit
import com.pomori.ui.digest.DigestRoute
import com.pomori.ui.digest.DigestViewModel
import com.pomori.ui.presets.PresetsRoute
import com.pomori.ui.settings.SettingsRoute
import com.pomori.ui.stats.StatsRoute
import com.pomori.ui.timer.TimerRoute

object Routes {
    const val TIMER = "timer"
    const val SETTINGS = "settings"
    const val STATS = "stats"
    const val PRESETS = "presets"

    /** `{sessionId}` argümanı DigestViewModel'in SavedStateHandle'ına otomatik gelir. */
    const val DIGEST = "digest/{${DigestViewModel.ARG_SESSION_ID}}"

    fun digest(sessionId: Long) = "digest/$sessionId"
}

/** Uygulamanın ana ekranları: sayaç, istatistikler, ayarlar ve seans bitince açılan bildirim özeti. */
@Composable
fun PomoriNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.TIMER, modifier = modifier) {
        composable(Routes.TIMER) {
            TimerRoute(
                onOpenDigest = { sessionId -> navController.navigate(Routes.digest(sessionId)) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenStats = { navController.navigate(Routes.STATS) },
                onOpenPresets = { navController.navigate(Routes.PRESETS) },
            )
        }
        composable(Routes.SETTINGS) {
            ContentWidthLimit {
                SettingsRoute(
                    onBack = { navController.popBackStack() },
                    onOpenPresets = { navController.navigate(Routes.PRESETS) },
                )
            }
        }
        composable(Routes.PRESETS) {
            ContentWidthLimit { PresetsRoute(onBack = { navController.popBackStack() }) }
        }
        composable(Routes.STATS) {
            ContentWidthLimit { StatsRoute(onBack = { navController.popBackStack() }) }
        }
        composable(
            route = Routes.DIGEST,
            arguments = listOf(navArgument(DigestViewModel.ARG_SESSION_ID) { type = NavType.LongType }),
        ) {
            // Kapatınca (ya da geri tuşuyla) sayaca döner; mola başlatma/atlama düğmeleri orada.
            ContentWidthLimit { DigestRoute(onClose = { navController.popBackStack() }) }
        }
    }
}
