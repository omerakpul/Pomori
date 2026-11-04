package com.pomori.domain.usecase

import com.pomori.domain.digest.IgnoreReason
import com.pomori.domain.digest.IncomingNotification
import com.pomori.domain.model.ActiveSession
import com.pomori.domain.model.SessionType
import com.pomori.testutil.FakeActiveSessionStore
import com.pomori.testutil.FakeClock
import com.pomori.testutil.FakeNotificationRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureNotificationTest {

    private val now = 10_000L
    private val clock = FakeClock(nowMs = now)
    private val repo = FakeNotificationRepository()

    private fun focusSession(endAt: Long = now + 60_000, sessionId: Long = 7) = ActiveSession(
        sessionId = sessionId,
        type = SessionType.FOCUS,
        endAt = endAt,
        plannedDurationMs = 60_000,
    )

    private fun useCase(active: ActiveSession?) =
        CaptureNotification(FakeActiveSessionStore(active), repo, clock)

    private fun incoming(
        isOngoing: Boolean = false,
        isGroupSummary: Boolean = false,
        isOwnApp: Boolean = false,
    ) = IncomingNotification(
        packageName = "com.whatsapp",
        appLabel = "WhatsApp",
        title = "Ayşe",
        text = "Toplantı 15:00'te",
        postedAt = 9_500,
        isOngoing = isOngoing,
        isGroupSummary = isGroupSummary,
        isOwnApp = isOwnApp,
    )

    @Test
    fun `during focus the notification is stored under the active session`() = runTest {
        val result = useCase(focusSession(sessionId = 7))(incoming())

        assertEquals(CaptureResult.Captured(id = 1), result)
        val stored = repo.rows.single()
        assertEquals(7L, stored.sessionId)
        assertEquals("com.whatsapp", stored.packageName)
        assertEquals("WhatsApp", stored.appLabel)
        assertEquals("Ayşe", stored.title)
        assertEquals("Toplantı 15:00'te", stored.text)
        assertEquals(9_500L, stored.postedAt)
    }

    @Test
    fun `without an active session nothing is stored`() = runTest {
        val result = useCase(active = null)(incoming())

        assertEquals(CaptureResult.NoActiveFocus, result)
        assertTrue(repo.rows.isEmpty())
    }

    @Test
    fun `during a break nothing is stored`() = runTest {
        val breakSession = focusSession().copy(type = SessionType.BREAK)

        val result = useCase(breakSession)(incoming())

        assertEquals(CaptureResult.NoActiveFocus, result)
        assertTrue(repo.rows.isEmpty())
    }

    @Test
    fun `a focus session whose time is up no longer captures`() = runTest {
        // Süre dolmuş ama kayıt henüz kapatılmamış: sayaç BreakPending sayar.
        val expired = focusSession(endAt = now - 1)

        val result = useCase(expired)(incoming())

        assertEquals(CaptureResult.NoActiveFocus, result)
        assertTrue(repo.rows.isEmpty())
    }

    @Test
    fun `capturing stops exactly when the session ends`() = runTest {
        val useCase = useCase(focusSession(endAt = now + 1_000))

        assertTrue(useCase(incoming()) is CaptureResult.Captured)
        clock.advanceBy(1_000)
        assertEquals(CaptureResult.NoActiveFocus, useCase(incoming()))
        assertEquals(1, repo.rows.size)
    }

    @Test
    fun `filtered notifications are reported and not stored`() = runTest {
        val useCase = useCase(focusSession())

        assertEquals(CaptureResult.Ignored(IgnoreReason.ONGOING), useCase(incoming(isOngoing = true)))
        assertEquals(CaptureResult.Ignored(IgnoreReason.GROUP_SUMMARY), useCase(incoming(isGroupSummary = true)))
        assertEquals(CaptureResult.Ignored(IgnoreReason.OWN_APP), useCase(incoming(isOwnApp = true)))
        assertTrue(repo.rows.isEmpty())
    }

    @Test
    fun `when saving fails the exception reaches the caller so the notification stays`() = runTest {
        repo.failWith = IllegalStateException("disk full")

        val failure = runCatching { useCase(focusSession())(incoming()) }.exceptionOrNull()

        assertTrue(failure is IllegalStateException)
        assertTrue(repo.rows.isEmpty())
    }
}
