package com.pomori.ui.timer

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pomori.R
import com.pomori.platform.permission.NotificationPermission
import com.pomori.platform.service.FocusForegroundService
import com.pomori.ui.theme.PomoriTheme

/** ViewModel'e, izin isteme akışına ve servis başlatmaya bağlanan giriş noktası. */
@Composable
fun TimerRoute(
    modifier: Modifier = Modifier,
    viewModel: TimerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // İzin verilse de verilmese de seans başlar: reddedilirse uygulama bozulmaz,
    // yalnızca sistem bildirimi görünmez (sayaç uygulama içinde çalışmaya devam eder).
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { viewModel.startFocus() }

    // Seans sürerken (ekran açıldığında da) servisin çalıştığından emin ol.
    LaunchedEffect(state.phase) {
        if (state.phase == TimerPhase.FOCUS || state.phase == TimerPhase.BREAK) {
            FocusForegroundService.start(context)
        }
    }

    TimerScreen(
        state = state,
        onStartFocus = {
            if (NotificationPermission.isGranted(context)) {
                viewModel.startFocus()
            } else {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        onCancelFocus = viewModel::cancelFocus,
        onStartBreak = viewModel::startBreak,
        onSkipBreak = viewModel::skipBreak,
        modifier = modifier,
    )
}

@Composable
fun TimerScreen(
    state: TimerUiState,
    onStartFocus: () -> Unit,
    onCancelFocus: () -> Unit,
    onStartBreak: () -> Unit,
    onSkipBreak: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val phaseLabel = when (state.phase) {
        TimerPhase.IDLE -> stringResource(R.string.phase_idle)
        TimerPhase.FOCUS -> stringResource(R.string.phase_focus)
        TimerPhase.BREAK_PENDING -> stringResource(R.string.phase_break_pending)
        TimerPhase.BREAK -> stringResource(R.string.phase_break)
    }
    val timeText = if (state.phase == TimerPhase.BREAK_PENDING) {
        stringResource(R.string.time_break_pending)
    } else {
        formatRemaining(state.remainingMs)
    }
    val ringDescription = stringResource(R.string.timer_content_description, phaseLabel, timeText)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = phaseLabel,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.testTag(TAG_PHASE),
        )
        Spacer(Modifier.height(24.dp))

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(280.dp)
                .semantics(mergeDescendants = true) { contentDescription = ringDescription },
        ) {
            CircularProgressIndicator(
                progress = { state.progress },
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 12.dp,
                strokeCap = StrokeCap.Round,
            )
            Text(
                text = timeText,
                style = MaterialTheme.typography.displayLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag(TAG_TIME),
            )
        }
        Spacer(Modifier.height(32.dp))

        when (state.phase) {
            TimerPhase.IDLE -> Button(
                onClick = onStartFocus,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TAG_START_FOCUS),
            ) { Text(stringResource(R.string.action_start_focus)) }

            TimerPhase.FOCUS -> OutlinedButton(
                onClick = onCancelFocus,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TAG_CANCEL),
            ) { Text(stringResource(R.string.action_cancel)) }

            TimerPhase.BREAK_PENDING -> {
                Button(
                    onClick = onStartBreak,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TAG_START_BREAK),
                ) { Text(stringResource(R.string.action_start_break)) }
                TextButton(
                    onClick = onSkipBreak,
                    modifier = Modifier.testTag(TAG_SKIP),
                ) { Text(stringResource(R.string.action_skip_break)) }
            }

            TimerPhase.BREAK -> TextButton(
                onClick = onSkipBreak,
                modifier = Modifier.testTag(TAG_SKIP),
            ) { Text(stringResource(R.string.action_skip_break)) }
        }
    }
}

const val TAG_PHASE = "timer_phase"
const val TAG_TIME = "timer_time"
const val TAG_START_FOCUS = "timer_start_focus"
const val TAG_CANCEL = "timer_cancel"
const val TAG_START_BREAK = "timer_start_break"
const val TAG_SKIP = "timer_skip"

@Preview(showBackground = true)
@Composable
private fun TimerScreenFocusPreview() {
    PomoriTheme {
        TimerScreen(
            state = TimerUiState(TimerPhase.FOCUS, remainingMs = 18 * 60_000L + 5_000L, totalMs = 25 * 60_000L),
            onStartFocus = {},
            onCancelFocus = {},
            onStartBreak = {},
            onSkipBreak = {},
        )
    }
}
