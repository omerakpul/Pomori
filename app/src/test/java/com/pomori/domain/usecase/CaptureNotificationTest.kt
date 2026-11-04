package com.pomori.domain.usecase

import com.pomori.domain.digest.IgnoreReason
import com.pomori.domain.digest.IncomingNotification
import com.pomori.domain.model.ActiveSession
import com.pomori.domain.model.ExceptionRule
import com.pomori.domain.model.SessionType
import com.pomori.testutil.FakeActiveSessionStore
import com.pomori.testutil.FakeClock
import com.pomori.testutil.FakeExceptionRepository
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

    private val exceptions = FakeExceptionRepository()

    private fun useCase(active: ActiveSession?) =
        CaptureNotification(FakeActiveSessionStore(active), repo, exceptions, clock)

    private fun incoming(
        isOngoing: Boolean = false,
        isGroupSummary: Boolean = false,
        isOwnApp: Boolean = false,
        category: String? = null,
        hasFullScreenIntent: Boolean = false,
    ) = IncomingNotification(
        packageName = "com.whatsapp",
        appLabel = "WhatsApp",
        title = "Ayşe",
        text = "Toplantı 15:00'te",
        postedAt = 9_500,
        isOngoing = isOngoing,
        isGroupSummary = isGroupSummary,
        isOwnApp = isOwnApp,
        category = category,
        hasFullScreenIntent = hasFullScreenIntent,
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

    // --- istisna kuralları ---

    @Test
    fun `a notification from an excepted package is left alone`() = runTest {
        exceptions.add(ExceptionRule.Package("com.whatsapp"))

        val result = useCase(focusSession())(incoming())

        assertEquals(CaptureResult.Ignored(IgnoreReason.EXCEPTED), result)
        assertTrue(repo.rows.isEmpty())
    }

    @Test
    fun `a notification from an excepted sender is left alone`() = runTest {
        exceptions.add(ExceptionRule.Sender("Ayşe"))

        val result = useCase(focusSession())(incoming())

        assertEquals(CaptureResult.Ignored(IgnoreReason.EXCEPTED), result)
        assertTrue(repo.rows.isEmpty())
    }

    @Test
    fun `rules that do not match do not stop the capture`() = runTest {
        exceptions.add(ExceptionRule.Package("com.bank"))
        exceptions.add(ExceptionRule.Sender("Patron"))

        val result = useCase(focusSession())(incoming())

        assertTrue(result is CaptureResult.Captured)
        assertEquals(1, repo.rows.size)
    }

    @Test
    fun `a rule added during the session applies to the next notification`() = runTest {
        val useCase = useCase(focusSession())
        assertTrue(useCase(incoming()) is CaptureResult.Captured)

        exceptions.add(ExceptionRule.Package("com.whatsapp"))

        assertEquals(CaptureResult.Ignored(IgnoreReason.EXCEPTED), useCase(incoming()))
        assertEquals(1, repo.rows.size)
    }

    @Test
    fun `a removed rule stops excepting the notification`() = runTest {
        exceptions.add(ExceptionRule.Package("com.whatsapp"))
        val useCase = useCase(focusSession())
        assertEquals(CaptureResult.Ignored(IgnoreReason.EXCEPTED), useCase(incoming()))

        exceptions.remove(ExceptionRule.Package("com.whatsapp"))

        assertTrue(useCase(incoming()) is CaptureResult.Captured)
    }

    @Test
    fun `outside a focus session the rules are not even consulted`() = runTest {
        exceptions.readFailure = IllegalStateException("would crash if read")

        val result = useCase(active = null)(incoming())

        assertEquals(CaptureResult.NoActiveFocus, result)
    }

    @Test
    fun `our own and ongoing notifications are ignored for their own reason first`() = runTest {
        exceptions.add(ExceptionRule.Package("com.whatsapp"))

        assertEquals(
            CaptureResult.Ignored(IgnoreReason.OWN_APP),
            useCase(focusSession())(incoming(isOwnApp = true)),
        )
        assertEquals(
            CaptureResult.Ignored(IgnoreReason.ONGOING),
            useCase(focusSession())(incoming(isOngoing = true)),
        )
    }

    @Test
    fun `when the rules cannot be read nothing is stored and the exception reaches the caller`() = runTest {
        exceptions.readFailure = IllegalStateException("db unavailable")

        val failure = runCatching { useCase(focusSession())(incoming()) }.exceptionOrNull()

        assertTrue(failure is IllegalStateException)
        assertTrue(repo.rows.isEmpty())
    }

    // --- zamana duyarlı bildirimler ---

    @Test
    fun `an incoming call during focus is neither stored nor hidden`() = runTest {
        val result = useCase(focusSession())(incoming(category = "call", hasFullScreenIntent = true))

        assertEquals(CaptureResult.Ignored(IgnoreReason.TIME_SENSITIVE), result)
        assertTrue(repo.rows.isEmpty())
    }

    @Test
    fun `an alarm during focus is neither stored nor hidden`() = runTest {
        val result = useCase(focusSession())(incoming(category = "alarm"))

        assertEquals(CaptureResult.Ignored(IgnoreReason.TIME_SENSITIVE), result)
        assertTrue(repo.rows.isEmpty())
    }

    @Test
    fun `a chat message during focus is still captured`() = runTest {
        val result = useCase(focusSession())(incoming(category = "msg"))

        assertTrue(result is CaptureResult.Captured)
        assertEquals(1, repo.rows.size)
    }

    @Test
    fun `a call is left alone even when the exception rules cannot be read`() = runTest {
        exceptions.readFailure = IllegalStateException("db unavailable")

        // Zamana duyarlı olan, kurallar okunmadan karara bağlanır: veritabanı hatası aramayı gizletmez.
        val result = useCase(focusSession())(incoming(category = "call"))

        assertEquals(CaptureResult.Ignored(IgnoreReason.TIME_SENSITIVE), result)
    }
}
