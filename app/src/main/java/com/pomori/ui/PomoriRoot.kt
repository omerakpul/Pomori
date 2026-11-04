package com.pomori.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pomori.platform.permission.NotificationListenerAccess
import com.pomori.ui.onboarding.OnboardingScreen
import com.pomori.ui.onboarding.OnboardingViewModel
import com.pomori.ui.timer.TimerRoute

/**
 * Uygulamanın kök ekranı: onboarding tamamlanmadıysa onu, tamamlandıysa sayacı gösterir.
 * Birden çok ekran olunca (özet, ayarlar) bu yer NavHost'a dönüşecek.
 */
@Composable
fun PomoriRoot(
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val done by viewModel.onboardingDone.collectAsStateWithLifecycle()
    val context = LocalContext.current
    when (done) {
        null -> Unit
        false -> OnboardingScreen(
            // İzin verilmese de uygulama çalışır: ayarlar açılır ve onboarding tamamlanır,
            // kullanıcı ayarlardan dönünce sayaca düşer.
            onGrantAccess = {
                NotificationListenerAccess.openSettings(context)
                viewModel.complete()
            },
            onSkip = viewModel::complete,
            modifier = modifier,
        )
        true -> TimerRoute(modifier = modifier)
    }
}
