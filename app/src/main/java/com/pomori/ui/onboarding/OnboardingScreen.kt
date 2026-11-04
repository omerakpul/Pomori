package com.pomori.ui.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pomori.R
import com.pomori.ui.components.IconBadge
import com.pomori.ui.components.PomoriButton
import com.pomori.ui.components.ambientGlow
import com.pomori.ui.theme.PomoriTheme

/** Bildirim erişimi istemeden önce neden istediğimizi ve neyi sakladığımızı açıklar. */
@Composable
fun OnboardingScreen(
    onGrantAccess: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .ambientGlow(scheme.primary, centerY = 0.12f)
            .safeDrawingPadding(),
    ) {
        // Sığarsa düğmeler altta durur, sığmazsa (büyük yazı tipi) her şey birlikte kayar.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = maxHeight)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).background(scheme.primary, CircleShape))
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(start = 10.dp),
                    )
                }
                Text(
                    text = stringResource(R.string.onboarding_title),
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold),
                    modifier = Modifier
                        .padding(top = 48.dp)
                        .semantics { heading() },
                )
                Text(
                    text = stringResource(R.string.onboarding_tagline),
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.primary,
                    modifier = Modifier.padding(top = 12.dp, bottom = 36.dp),
                )
                Feature(
                    icon = R.drawable.ic_notifications,
                    tint = scheme.primary,
                    title = stringResource(R.string.onboarding_why_title),
                    body = stringResource(R.string.onboarding_why_body),
                )
                Divider()
                Feature(
                    icon = R.drawable.ic_list,
                    tint = scheme.tertiary,
                    title = stringResource(R.string.onboarding_data_title),
                    body = stringResource(R.string.onboarding_data_body),
                )
                Divider()
                Feature(
                    icon = R.drawable.ic_lock,
                    tint = scheme.secondary,
                    title = stringResource(R.string.onboarding_privacy_title),
                    body = stringResource(R.string.onboarding_privacy_body),
                )
            }
            Column(
                modifier = Modifier.padding(top = 32.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                PomoriButton(
                    onClick = onGrantAccess,
                    modifier = Modifier.testTag("onboarding_grant_access"),
                ) {
                    Text(stringResource(R.string.onboarding_grant_access))
                }
                TextButton(
                    onClick = onSkip,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("onboarding_skip"),
                ) {
                    Text(stringResource(R.string.onboarding_skip), color = scheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun Divider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

/** Kartsız satır: soldaki renkli simge, sağda başlık ve açıklama; satırlar ince çizgiyle ayrılır. */
@Composable
private fun Feature(
    @DrawableRes icon: Int,
    tint: Color,
    title: String,
    body: String,
) {
    Row(
        modifier = Modifier.padding(vertical = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        IconBadge(icon = icon, tint = tint)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun OnboardingScreenPreview() {
    PomoriTheme { OnboardingScreen(onGrantAccess = {}, onSkip = {}) }
}
