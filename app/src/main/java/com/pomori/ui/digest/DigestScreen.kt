package com.pomori.ui.digest

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pomori.R
import com.pomori.domain.model.AppDigest
import com.pomori.domain.model.CapturedNotification
import com.pomori.platform.permission.NotificationListenerAccess
import com.pomori.ui.permission.NotificationAccessCard
import com.pomori.ui.permission.rememberNotificationAccessGranted
import com.pomori.ui.theme.PomoriTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** ViewModel'e bağlanan giriş noktası. Seans kimliği navigasyon argümanından gelir. */
@Composable
fun DigestRoute(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DigestViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    DigestScreen(
        state = state,
        onToggleApp = viewModel::toggleExpanded,
        onClose = onClose,
        notificationAccessGranted = rememberNotificationAccessGranted(),
        onOpenNotificationAccess = { NotificationListenerAccess.openSettings(context) },
        modifier = modifier,
    )
}

@Composable
fun DigestScreen(
    state: DigestUiState,
    onToggleApp: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    notificationAccessGranted: Boolean = true,
    onOpenNotificationAccess: () -> Unit = {},
) {
    Column(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            when {
                // İlk veri gelene kadar hiçbir şey çizme: boş durum yanıp sönmesin.
                state.isLoading -> Unit
                state.isEmpty -> EmptyDigest(notificationAccessGranted, onOpenNotificationAccess)
                else -> DigestList(state, onToggleApp)
            }
        }
        if (!state.isLoading) {
            Button(
                onClick = onClose,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .testTag("digest_continue"),
            ) {
                Text(stringResource(R.string.digest_continue))
            }
        }
    }
}

@Composable
private fun EmptyDigest(
    notificationAccessGranted: Boolean,
    onOpenNotificationAccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .testTag("digest_empty"),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Erişim kapalıyken "hiç bölünmedin" demek yanlış olurdu: bildirimler gelmiş ama yakalanmamış olabilir.
        Text(
            text = stringResource(
                if (notificationAccessGranted) R.string.digest_empty_title else R.string.digest_empty_title_no_access,
            ),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(
                if (notificationAccessGranted) R.string.digest_empty_body else R.string.digest_empty_body_no_access,
            ),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (!notificationAccessGranted) {
            NotificationAccessCard(
                onOpenSettings = onOpenNotificationAccess,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}

@Composable
private fun DigestList(
    state: DigestUiState,
    onToggleApp: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("digest_list"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(bottom = 4.dp)) {
                Text(
                    text = stringResource(R.string.digest_title),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = stringResource(R.string.digest_summary, state.total, state.apps.size),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(state.apps, key = { it.packageName }) { app ->
            AppCard(
                app = app,
                expanded = state.isExpanded(app.packageName),
                onToggle = { onToggleApp(app.packageName) },
            )
        }
    }
}

@Composable
private fun AppCard(app: AppDigest, expanded: Boolean, onToggle: () -> Unit) {
    val stateText = stringResource(if (expanded) R.string.digest_state_expanded else R.string.digest_state_collapsed)
    Card(modifier = Modifier.fillMaxWidth().testTag("digest_app_${app.packageName}")) {
        Column(modifier = Modifier.animateContentSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button, onClick = onToggle)
                    .semantics { stateDescription = stateText }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = app.appLabel,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                CountBadge(app.count)
                Icon(
                    painter = painterResource(R.drawable.ic_expand_more),
                    contentDescription = null, // Durum satırın stateDescription'ında bildirilir.
                    modifier = Modifier.rotate(if (expanded) 180f else 0f),
                )
            }
            if (expanded) {
                HorizontalDivider()
                app.items.forEachIndexed { index, item ->
                    NotificationRow(item)
                    if (index != app.items.lastIndex) HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}

@Composable
private fun CountBadge(count: Int) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun NotificationRow(item: CapturedNotification) {
    val time = rememberTimeText(item.postedAt)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = item.title ?: item.text ?: stringResource(R.string.digest_item_untitled),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = time,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // Başlık yoksa metin zaten başlık yerine gösterildi; tekrar etme.
        if (item.title != null && item.text != null) {
            Text(
                text = item.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun rememberTimeText(epochMs: Long): String {
    val formatter = remember { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT) }
    return remember(epochMs) {
        Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).format(formatter)
    }
}

private fun sample(id: Long, pkg: String, label: String, title: String?, text: String?, at: Long) =
    CapturedNotification(id, 1, pkg, label, title, text, at)

@Preview(showBackground = true)
@Composable
private fun DigestScreenPreview() {
    val wa = AppDigest(
        "com.whatsapp",
        "WhatsApp",
        listOf(
            sample(2, "com.whatsapp", "WhatsApp", "Mehmet", "Kod review hazır", 1_700_000_100_000),
            sample(1, "com.whatsapp", "WhatsApp", "Ayşe", "Toplantı 15:00'te", 1_700_000_000_000),
        ),
    )
    val gmail = AppDigest(
        "com.google.android.gm",
        "Gmail",
        listOf(sample(3, "com.google.android.gm", "Gmail", "Fatura", null, 1_700_000_050_000)),
    )
    PomoriTheme {
        DigestScreen(
            state = DigestUiState(
                isLoading = false,
                total = 3,
                apps = listOf(wa, gmail),
                expanded = setOf("com.whatsapp"),
            ),
            onToggleApp = {},
            onClose = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyDigestNoAccessPreview() {
    PomoriTheme {
        DigestScreen(
            state = DigestUiState(isLoading = false),
            onToggleApp = {},
            onClose = {},
            notificationAccessGranted = false,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyDigestPreview() {
    PomoriTheme { DigestScreen(state = DigestUiState(isLoading = false), onToggleApp = {}, onClose = {}) }
}
