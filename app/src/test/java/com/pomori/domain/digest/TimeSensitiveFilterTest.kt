package com.pomori.domain.digest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Gelen arama, alarm gibi kaçırılması zarar veren bildirimlere dokunulmaması kuralı. */
class TimeSensitiveFilterTest {

    private fun incoming(
        category: String? = null,
        hasFullScreenIntent: Boolean = false,
        isInsistent: Boolean = false,
        isOngoing: Boolean = false,
        isGroupSummary: Boolean = false,
        isOwnApp: Boolean = false,
    ) = IncomingNotification(
        packageName = "com.example",
        appLabel = "Example",
        title = "t",
        text = "x",
        postedAt = 1_000,
        isOngoing = isOngoing,
        isGroupSummary = isGroupSummary,
        isOwnApp = isOwnApp,
        category = category,
        hasFullScreenIntent = hasFullScreenIntent,
        isInsistent = isInsistent,
    )

    private fun reason(n: IncomingNotification) = NotificationFilter.ignoreReason(n)

    // --- kategoriler ---

    @Test
    fun `every time sensitive category is left alone`() {
        val categories = listOf(
            "call", "alarm", "stopwatch", "reminder", "event", "navigation", "location_sharing",
            "workout", "transport", "progress", "service", "status", "system", "error",
        )

        categories.forEach { category ->
            assertEquals(
                "category '$category'",
                IgnoreReason.TIME_SENSITIVE,
                reason(incoming(category = category)),
            )
        }
    }

    @Test
    fun `message email social and promotional categories are still captured`() {
        listOf("msg", "email", "social", "promo", "recommendation").forEach { category ->
            assertNull("category '$category' must be captured", reason(incoming(category = category)))
        }
    }

    @Test
    fun `a missed call and a voicemail are captured because they are already missed`() {
        assertNull(reason(incoming(category = "missed_call")))
        assertNull(reason(incoming(category = "voicemail")))
    }

    @Test
    fun `no category and an unknown category are captured`() {
        assertNull(reason(incoming(category = null)))
        assertNull(reason(incoming(category = "some_future_category")))
    }

    // --- kategori olmasa da yakalayan işaretler ---

    @Test
    fun `a full screen intent is enough even without a category`() {
        assertEquals(IgnoreReason.TIME_SENSITIVE, reason(incoming(hasFullScreenIntent = true)))
    }

    @Test
    fun `an insistent notification is enough even without a category`() {
        // Çalan telefon ya da alarm, kullanıcı kapatana kadar sesi tekrarlar.
        assertEquals(IgnoreReason.TIME_SENSITIVE, reason(incoming(isInsistent = true)))
    }

    @Test
    fun `a call that apps forgot to categorize is caught by its full screen intent and insistent sound`() {
        val ringingCall = incoming(category = null, hasFullScreenIntent = true, isInsistent = true)

        assertEquals(IgnoreReason.TIME_SENSITIVE, reason(ringingCall))
    }

    // --- diğer kurallarla sıra ---

    @Test
    fun `our own notifications keep their own reason`() {
        assertEquals(IgnoreReason.OWN_APP, reason(incoming(category = "call", isOwnApp = true)))
    }

    @Test
    fun `ongoing keeps its own reason before time sensitive`() {
        assertEquals(IgnoreReason.ONGOING, reason(incoming(category = "call", isOngoing = true)))
    }

    @Test
    fun `group summary keeps its own reason before time sensitive`() {
        assertEquals(IgnoreReason.GROUP_SUMMARY, reason(incoming(category = "alarm", isGroupSummary = true)))
    }

    @Test
    fun `an ordinary notification is untouched by the new rule`() {
        assertNull(reason(incoming()))
    }
}
