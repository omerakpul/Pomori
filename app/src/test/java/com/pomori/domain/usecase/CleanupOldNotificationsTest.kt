package com.pomori.domain.usecase

import com.pomori.domain.model.CapturedNotification
import com.pomori.testutil.FakeClock
import com.pomori.testutil.FakeNotificationRepository
import com.pomori.testutil.FakeSettingsStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class CleanupOldNotificationsTest {

    private val day = 24 * 60 * 60 * 1000L
    private val now = 100 * day
    private val clock = FakeClock(nowMs = now)
    private val repo = FakeNotificationRepository()

    private fun useCase(retentionDays: Int = 7) =
        CleanupOldNotifications(FakeSettingsStore(retentionDays = retentionDays), repo, clock)

    private suspend fun notification(ago: Long) {
        repo.insert(
            CapturedNotification(
                sessionId = 1,
                packageName = "com.whatsapp",
                appLabel = "WhatsApp",
                title = "t",
                text = null,
                postedAt = now - ago,
            ),
        )
    }

    private fun remainingAges() = repo.rows.map { now - it.postedAt }.sorted()

    @Test
    fun `nothing is deleted when everything is within the retention period`() = runTest {
        notification(ago = 1 * day)
        notification(ago = 6 * day)

        assertEquals(0, useCase(retentionDays = 7)())
        assertEquals(2, repo.rows.size)
    }

    @Test
    fun `notifications older than the retention period are deleted`() = runTest {
        notification(ago = 1 * day)
        notification(ago = 8 * day)
        notification(ago = 30 * day)

        val deleted = useCase(retentionDays = 7)()

        assertEquals(2, deleted)
        assertEquals(listOf(1 * day), remainingAges())
    }

    @Test
    fun `a notification exactly at the limit is kept`() = runTest {
        notification(ago = 7 * day)

        assertEquals(0, useCase(retentionDays = 7)())
        assertEquals(1, repo.rows.size)
    }

    @Test
    fun `one millisecond past the limit is deleted`() = runTest {
        notification(ago = 7 * day + 1)

        assertEquals(1, useCase(retentionDays = 7)())
        assertEquals(0, repo.rows.size)
    }

    @Test
    fun `the configured retention is used`() = runTest {
        notification(ago = 2 * day)
        notification(ago = 10 * day)
        notification(ago = 20 * day)

        useCase(retentionDays = 14)()
        assertEquals(listOf(2 * day, 10 * day), remainingAges())

        useCase(retentionDays = 1)()
        assertEquals(emptyList<Long>(), remainingAges())
    }

    @Test
    fun `an empty store deletes nothing`() = runTest {
        assertEquals(0, useCase()())
    }

    @Test
    fun `running it twice is harmless`() = runTest {
        notification(ago = 1 * day)
        notification(ago = 9 * day)
        val useCase = useCase(retentionDays = 7)

        assertEquals(1, useCase())
        assertEquals(0, useCase())
        assertEquals(1, repo.rows.size)
    }

    @Test
    fun `the cutoff follows the clock`() = runTest {
        notification(ago = 5 * day)
        val useCase = useCase(retentionDays = 7)
        assertEquals(0, useCase())

        clock.advanceBy(3 * day) // bildirim artık 8 günlük

        assertEquals(1, useCase())
    }
}
