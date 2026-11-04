package com.pomori.domain.digest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationFilterTest {

    private fun incoming(
        isOngoing: Boolean = false,
        isGroupSummary: Boolean = false,
        isOwnApp: Boolean = false,
    ) = IncomingNotification(
        packageName = "com.whatsapp",
        appLabel = "WhatsApp",
        title = "Ayşe",
        text = "Selam",
        postedAt = 1_000,
        isOngoing = isOngoing,
        isGroupSummary = isGroupSummary,
        isOwnApp = isOwnApp,
    )

    @Test
    fun `an ordinary notification is not ignored`() {
        assertNull(NotificationFilter.ignoreReason(incoming()))
    }

    @Test
    fun `ongoing notifications are ignored`() {
        assertEquals(IgnoreReason.ONGOING, NotificationFilter.ignoreReason(incoming(isOngoing = true)))
    }

    @Test
    fun `group summaries are ignored`() {
        assertEquals(
            IgnoreReason.GROUP_SUMMARY,
            NotificationFilter.ignoreReason(incoming(isGroupSummary = true)),
        )
    }

    @Test
    fun `our own notifications are ignored`() {
        assertEquals(IgnoreReason.OWN_APP, NotificationFilter.ignoreReason(incoming(isOwnApp = true)))
    }

    @Test
    fun `own app wins over every other reason`() {
        val all = incoming(isOngoing = true, isGroupSummary = true, isOwnApp = true)
        assertEquals(IgnoreReason.OWN_APP, NotificationFilter.ignoreReason(all))
    }

    @Test
    fun `ongoing wins over group summary`() {
        val both = incoming(isOngoing = true, isGroupSummary = true)
        assertEquals(IgnoreReason.ONGOING, NotificationFilter.ignoreReason(both))
    }
}
