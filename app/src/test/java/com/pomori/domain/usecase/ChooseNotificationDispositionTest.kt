package com.pomori.domain.usecase

import com.pomori.testutil.FakeSettingsStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ChooseNotificationDispositionTest {

    private val minute = 60_000L
    private val now = 1_000_000L

    private fun choose(restore: Boolean = true) =
        ChooseNotificationDisposition(FakeSettingsStore(restoreNotifications = restore))

    @Test
    fun `with the setting on the notification is snoozed until the session ends plus a small buffer`() = runTest {
        val choice = choose()(sessionEndsAt = now + 20 * minute, now = now)

        val expected = 20 * minute + ChooseNotificationDisposition.RETURN_BUFFER_MS
        assertEquals(NotificationDisposition.Snooze(durationMs = expected, returnsAt = now + expected), choice)
    }

    @Test
    fun `the buffer makes the notification return after the session has already ended`() = runTest {
        val choice = choose()(sessionEndsAt = now + minute, now = now) as NotificationDisposition.Snooze

        // Dönüş anı seans bitişinden sonra: o anda seans "bitti" görünür, dönen bildirim yeniden yakalanmaz.
        assertEquals(true, choice.returnsAt > now + minute)
    }

    @Test
    fun `with the setting off the notification is removed as before`() = runTest {
        assertEquals(NotificationDisposition.Remove, choose(restore = false)(now + 20 * minute, now))
    }

    @Test
    fun `a session about to end still snoozes for a minimum time`() = runTest {
        val choice = choose()(sessionEndsAt = now + 100, now = now) as NotificationDisposition.Snooze

        assertEquals(ChooseNotificationDisposition.MIN_SNOOZE_MS, choice.durationMs)
    }

    @Test
    fun `an already finished session still snoozes for the minimum time instead of a negative one`() = runTest {
        val choice = choose()(sessionEndsAt = now - 10 * minute, now = now) as NotificationDisposition.Snooze

        assertEquals(ChooseNotificationDisposition.MIN_SNOOZE_MS, choice.durationMs)
    }

    @Test
    fun `the setting is read each time so a change applies to the next notification`() = runTest {
        val settings = FakeSettingsStore(restoreNotifications = true)
        val use = ChooseNotificationDisposition(settings)

        assertEquals(true, use(now + minute, now) is NotificationDisposition.Snooze)
        settings.setRestoreNotifications(false)
        assertEquals(NotificationDisposition.Remove, use(now + minute, now))
    }
}
