package com.pomori.ui.permission

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.pomori.R
import com.pomori.platform.permission.NotificationListenerAccess
import com.pomori.ui.theme.PomoriTheme

const val TAG_ACCESS_WARNING = "notification_access_warning"

/**
 * Bildirim erişimi şu an açık mı? Kullanıcı ayarlardan döndüğünde (ON_RESUME) yeniden okunur,
 * böylece kart, uygulamayı yeniden başlatmadan kaybolur.
 */
@Composable
fun rememberNotificationAccessGranted(): Boolean {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(NotificationListenerAccess.isGranted(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        granted = NotificationListenerAccess.isGranted(context)
    }
    return granted
}

/**
 * Erişim kapalıyken gösterilen yumuşak uyarı. Uygulama çalışmaya devam eder; yalnızca bildirimlerin
 * özete eklenmediğini, normal şekilde gelmeye devam ettiğini söyler ve ayarlara götürür.
 */
@Composable
fun NotificationAccessCard(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag(TAG_ACCESS_WARNING),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.access_warning_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.access_warning_body),
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(
                onClick = onOpenSettings,
                // Varsayılan `primary` rengi kartın zemininde (tertiaryContainer) bazı dinamik renk
                // şemalarında neredeyse görünmez (koyu modda 1,12:1 ölçüldü). Kartın kendi içerik rengi
                // her şemada okunur.
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                ),
                modifier = Modifier.padding(top = 4.dp),
            ) {
                Text(
                    text = stringResource(R.string.access_warning_action),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NotificationAccessCardPreview() {
    PomoriTheme { NotificationAccessCard(onOpenSettings = {}, modifier = Modifier.padding(16.dp)) }
}
