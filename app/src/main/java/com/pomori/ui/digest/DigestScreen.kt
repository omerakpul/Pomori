package com.pomori.ui.digest

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pomori.R
import com.pomori.domain.model.AppDigest
import com.pomori.domain.model.CapturedNotification
import com.pomori.ui.components.EmptyState
import com.pomori.ui.components.IconBadge
import com.pomori.ui.components.IconLabel
import com.pomori.ui.components.PomoriButton
import com.pomori.ui.components.InitialAvatar
import com.pomori.ui.components.SoftCard
import com.pomori.platform.apps.AppLauncher
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
        onOpenApp = { packageName ->
            // Silinmiş ya da başlatılamayan uygulamada sessizce hiçbir şey olmasın: kısa bir bilgi göster.
            if (!AppLauncher.open(context, packageName)) {
                Toast.makeText(context, R.string.digest_open_failed, Toast.LENGTH_SHORT).show()
            }
        },
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
    onOpenApp: (packageName: String) -> Unit = {},
) {
    Column(modifier = modifier.fillMaxSize().safeDrawingPadding()) {
        Box(modifier = Modifier.weight(1f)) {
            when {
                // İlk veri gelene kadar hiçbir şey çizme: boş durum yanıp sönmesin.
                state.isLoading -> Unit
                state.isEmpty -> EmptyDigest(notificationAccessGranted, onOpenNotificationAccess)
                else -> DigestList(state, onToggleApp, onOpenApp)
            }
        }
        if (!state.isLoading) {
            PomoriButton(
                onClick = onClose,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .testTag("digest_continue"),
            ) {
                IconLabel(R.drawable.ic_check, stringResource(R.string.digest_continue))
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
    val scheme = MaterialTheme.colorScheme
    // Erişim kapalıyken "hiç bölünmedin" demek yanlış olurdu: bildirimler gelmiş ama yakalanmamış olabilir.
    EmptyState(
        icon = if (notificationAccessGranted) R.drawable.ic_check else R.drawable.ic_notifications,
        title = stringResource(
            if (notificationAccessGranted) R.string.digest_empty_title else R.string.digest_empty_title_no_access,
        ),
        body = stringResource(
            if (notificationAccessGranted) R.string.digest_empty_body else R.string.digest_empty_body_no_access,
        ),
        tint = if (notificationAccessGranted) scheme.secondary else scheme.tertiary,
        modifier = modifier
            .fillMaxSize()
            .testTag("digest_empty"),
    ) {
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
    onOpenApp: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("digest_list"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { DigestHeader(total = state.total, appCount = state.apps.size) }
        items(state.apps, key = { it.packageName }) { app ->
            AppCard(
                app = app,
                expanded = state.isExpanded(app.packageName),
                onToggle = { onToggleApp(app.packageName) },
                onOpen = { onOpenApp(app.packageName) },
            )
        }
    }
}

/** Özetin başı: başlık, kaç bildirimin beklediği ve kaç uygulamadan geldiği. */
@Composable
private fun DigestHeader(total: Int, appCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        IconBadge(
            icon = R.drawable.ic_inbox,
            tint = MaterialTheme.colorScheme.primary,
            size = 56.dp,
            iconSize = 28.dp,
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.digest_title),
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.digest_summary, total, appCount),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AppCard(app: AppDigest, expanded: Boolean, onToggle: () -> Unit, onOpen: () -> Unit) {
    val stateText = stringResource(if (expanded) R.string.digest_state_expanded else R.string.digest_state_collapsed)
    val chevronRotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")
    SoftCard(modifier = Modifier.fillMaxWidth().testTag("digest_app_${app.packageName}")) {
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
                // Baş harf süs: uygulama adı zaten yanında okunuyor.
                InitialAvatar(name = app.appLabel, modifier = Modifier.clearAndSetSemantics { })
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
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.rotate(chevronRotation),
                )
            }
            if (expanded) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                app.items.forEachIndexed { index, item ->
                    NotificationRow(item, onOpen)
                    if (index != app.items.lastIndex) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.padding(start = 68.dp, end = 16.dp),
                        )
                    }
                }
                // Satırlara dokunmak da uygulamayı açar; ama bunu keşfettirmek için görünür bir düğme de var.
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                TextButton(
                    onClick = onOpen,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(end = 8.dp)
                        .testTag("digest_open_${app.packageName}"),
                ) {
                    IconLabel(R.drawable.ic_open_in_new, stringResource(R.string.digest_open_app))
                }
            }
        }
    }
}

@Composable
private fun CountBadge(count: Int) {
    Text(
        text = count.toString(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
private fun NotificationRow(item: CapturedNotification, onOpen: () -> Unit) {
    val time = rememberTimeText(item.postedAt)
    val openLabel = stringResource(R.string.digest_open_app)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = openLabel, onClick = onOpen)
            .padding(start = 68.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
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
