package com.pomori.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    when (done) {
        null -> Unit
        false -> OnboardingScreen(onContinue = viewModel::complete, modifier = modifier)
        true -> TimerRoute(modifier = modifier)
    }
}
