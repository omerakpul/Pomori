package com.pomori.ui.digest

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pomori.R
import com.pomori.domain.model.AppDigest
import com.pomori.domain.model.CapturedNotification
import com.pomori.ui.permission.TAG_ACCESS_WARNING
import com.pomori.ui.pomoriComposeRule
import com.pomori.ui.str
import com.pomori.ui.theme.PomoriTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DigestScreenTest {

    @get:Rule
    val rule = pomoriComposeRule()

    private fun n(id: Long, pkg: String, title: String?, text: String?) = CapturedNotification(
        id = id,
        sessionId = 1,
        packageName = pkg,
        appLabel = pkg,
        title = title,
        text = text,
        postedAt = 1_700_000_000_000 + id * 60_000,
    )

    private val whatsapp = AppDigest(
        packageName = "com.whatsapp",
        appLabel = "WhatsApp",
        items = listOf(
            n(3, "com.whatsapp", "Mehmet", "Kod review hazır"),
            n(2, "com.whatsapp", "Ayşe", "Toplantı 15:00'te"),
            n(1, "com.whatsapp", "Aile grubu", "Akşam yemeği?"),
        ),
    )
    private val gmail = AppDigest(
        packageName = "com.google.android.gm",
        appLabel = "Gmail",
        items = listOf(n(4, "com.google.android.gm", "Fatura hazır", "Kasım ayı faturanız ekte")),
    )
    private val populated = DigestUiState(isLoading = false, total = 4, apps = listOf(whatsapp, gmail))

    private class Calls {
        val log = mutableListOf<String>()
    }

    private fun show(
        state: DigestUiState,
        calls: Calls = Calls(),
        accessGranted: Boolean = true,
    ): Calls {
        rule.setContent {
            PomoriTheme {
                DigestScreen(
                    state = state,
                    onToggleApp = { calls.log += "toggle:$it" },
                    onClose = { calls.log += "close" },
                    notificationAccessGranted = accessGranted,
                    onOpenNotificationAccess = { calls.log += "openAccess" },
                )
            }
        }
        return calls
    }

    // --- yükleme ---

    @Test
    fun loading_drawsNothingNotEvenTheEmptyStateOrTheCloseButton() {
        show(DigestUiState(isLoading = true))

        rule.onNodeWithTag("digest_empty").assertDoesNotExist()
        rule.onNodeWithTag("digest_list").assertDoesNotExist()
        rule.onNodeWithTag("digest_continue").assertDoesNotExist()
    }

    // --- boş durum ---

    @Test
    fun empty_withAccess_celebratesAnUndisturbedSession() {
        show(DigestUiState(isLoading = false), accessGranted = true)

        rule.onNodeWithTag("digest_empty").assertIsDisplayed()
        rule.onNodeWithText(rule.str(R.string.digest_empty_title)).assertIsDisplayed()
        rule.onNodeWithText(rule.str(R.string.digest_empty_body)).assertIsDisplayed()
        rule.onNodeWithTag(TAG_ACCESS_WARNING).assertDoesNotExist()
    }

    @Test
    fun empty_withoutAccess_doesNotClaimTheFocusWasUndisturbed() {
        show(DigestUiState(isLoading = false), accessGranted = false)

        // Erişim kapalıyken bildirimler gelmiş ama yakalanmamış olabilir: "harika iş" demek yanlış olurdu.
        rule.onNodeWithText(rule.str(R.string.digest_empty_title_no_access)).assertIsDisplayed()
        rule.onNodeWithText(rule.str(R.string.digest_empty_body_no_access)).assertIsDisplayed()
        rule.onNodeWithText(rule.str(R.string.digest_empty_title)).assertDoesNotExist()
        rule.onNodeWithTag(TAG_ACCESS_WARNING).assertIsDisplayed()
    }

    @Test
    fun empty_withoutAccess_theCardOpensTheSettings() {
        val calls = show(DigestUiState(isLoading = false), accessGranted = false)

        rule.onNode(hasText(rule.str(R.string.access_warning_action))).performClick()

        assertEquals(listOf("openAccess"), calls.log)
    }

    @Test
    fun empty_stillOffersTheCloseButton() {
        val calls = show(DigestUiState(isLoading = false))

        rule.onNodeWithTag("digest_continue").performClick()

        assertEquals(listOf("close"), calls.log)
    }

    // --- dolu liste ---

    @Test
    fun populated_showsTheSummaryAndOneCardPerApp() {
        show(populated)

        rule.onNodeWithText(rule.str(R.string.digest_title)).assertIsDisplayed()
        rule.onNodeWithText(rule.str(R.string.digest_summary, 4, 2)).assertIsDisplayed()
        rule.onNodeWithTag("digest_app_com.whatsapp").assertIsDisplayed()
        rule.onNodeWithTag("digest_app_com.google.android.gm").assertIsDisplayed()
        rule.onNodeWithText("WhatsApp").assertIsDisplayed()
        rule.onNodeWithText("Gmail").assertIsDisplayed()
        rule.onNodeWithTag("digest_empty").assertDoesNotExist()
    }

    @Test
    fun populated_showsTheNotificationCountOfEachApp() {
        show(populated)

        // Sayı rozeti, kartın tıklanabilir (birleştirilmiş) satırının içindedir.
        rule.onNode(hasText("3") and hasAnyAncestor(hasTestTag("digest_app_com.whatsapp"))).assertIsDisplayed()
        rule.onNode(hasText("1") and hasAnyAncestor(hasTestTag("digest_app_com.google.android.gm"))).assertIsDisplayed()
    }

    @Test
    fun populated_startsCollapsed() {
        show(populated)

        rule.onNodeWithText("Mehmet").assertDoesNotExist()
        rule.onNodeWithText("Fatura hazır").assertDoesNotExist()
    }

    @Test
    fun tappingACard_reportsWhichAppWasToggled() {
        val calls = show(populated)

        rule.onNodeWithText("WhatsApp").performClick()
        rule.onNodeWithText("Gmail").performClick()

        assertEquals(listOf("toggle:com.whatsapp", "toggle:com.google.android.gm"), calls.log)
    }

    @Test
    fun anExpandedApp_listsItsNotificationsWithTitleAndText() {
        show(populated.copy(expanded = setOf("com.whatsapp")))

        rule.onNodeWithText("Mehmet").assertIsDisplayed()
        rule.onNodeWithText("Kod review hazır").assertIsDisplayed()
        rule.onNodeWithText("Ayşe").assertIsDisplayed()
        rule.onNodeWithText("Toplantı 15:00'te").assertIsDisplayed()
        // Diğer uygulama kapalı kalır.
        rule.onNodeWithText("Fatura hazır").assertDoesNotExist()
    }

    @Test
    fun expandingAndCollapsing_revealsAndHidesTheNotifications() {
        // Durumu tutan bir sarmalayıcı: gerçek ViewModel'in yaptığı gibi dokunuşa göre açıp kapatır.
        rule.setContent {
            var state by mutableStateOf(populated)
            PomoriTheme {
                DigestScreen(
                    state = state,
                    onToggleApp = { pkg ->
                        val expanded = state.expanded
                        state = state.copy(expanded = if (pkg in expanded) expanded - pkg else expanded + pkg)
                    },
                    onClose = {},
                )
            }
        }

        rule.onNodeWithText("Mehmet").assertDoesNotExist()
        rule.onNodeWithText("WhatsApp").performClick()
        rule.onNodeWithText("Mehmet").assertIsDisplayed()
        rule.onNodeWithText("WhatsApp").performClick()
        rule.onNodeWithText("Mehmet").assertDoesNotExist()
    }

    // --- başlık/metin yedekleri ---

    @Test
    fun aNotificationWithOnlyTextUsesTheTextAsItsTitle() {
        val app = AppDigest("org.telegram", "Telegram", listOf(n(1, "org.telegram", null, "Sadece metin")))
        show(DigestUiState(isLoading = false, total = 1, apps = listOf(app), expanded = setOf("org.telegram")))

        rule.onNodeWithText("Sadece metin").assertIsDisplayed()
    }

    @Test
    fun aNotificationWithNeitherTitleNorTextShowsAPlaceholder() {
        val app = AppDigest("com.silent", "Sessiz", listOf(n(1, "com.silent", null, null)))
        show(DigestUiState(isLoading = false, total = 1, apps = listOf(app), expanded = setOf("com.silent")))

        rule.onNodeWithText(rule.str(R.string.digest_item_untitled)).assertIsDisplayed()
    }

    // --- kapat ---

    @Test
    fun theCloseButton_isShownWithTheListAndCloses() {
        val calls = show(populated)

        rule.onNodeWithTag("digest_continue").assertIsDisplayed()
        rule.onNodeWithTag("digest_continue").performClick()

        assertEquals(listOf("close"), calls.log)
    }
}
