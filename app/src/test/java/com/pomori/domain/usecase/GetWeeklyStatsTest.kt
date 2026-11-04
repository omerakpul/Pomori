package com.pomori.domain.usecase

import app.cash.turbine.test
import com.pomori.domain.model.CapturedNotification
import com.pomori.domain.model.FocusSession
import com.pomori.domain.model.SessionStatus
import com.pomori.domain.model.SessionType
import com.pomori.testutil.FakeClock
import com.pomori.testutil.FakeNotificationRepository
import com.pomori.testutil.FakeSessionRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetWeeklyStatsTest {

    private val day = 24 * 60 * 60 * 1000L
    private val minute = 60 * 1000L
    private val now = 100 * day
    private val clock = FakeClock(nowMs = now)
    private val sessions = FakeSessionRepository()
    private val notifications = FakeNotificationRepository()
    private val getWeeklyStats = GetWeeklyStats(sessions, notifications, clock)

    private suspend fun session(
        startedAgo: Long,
        planned: Long = 25 * minute,
        type: SessionType = SessionType.FOCUS,
        status: SessionStatus = SessionStatus.COMPLETED,
    ) = sessions.insert(
        FocusSession(
            type = type,
            startedAt = now - startedAgo,
            plannedDurationMs = planned,
            endedAt = now - startedAgo + planned,
            status = status,
        ),
    )

    private suspend fun notification(pkg: String, label: String = pkg, ago: Long = day) {
        notifications.insert(
            CapturedNotification(
                sessionId = 1,
                packageName = pkg,
                appLabel = label,
                title = null,
                text = null,
                postedAt = now - ago,
            ),
        )
    }

    @Test
    fun `without any data everything is zero`() = runTest {
        getWeeklyStats().test {
            val stats = awaitItem()
            assertEquals(0L, stats.totalFocusMs)
            assertEquals(0, stats.completedSessions)
            assertEquals(emptyList<String>(), stats.topDistractors.map { it.packageName })
        }
    }

    @Test
    fun `total focus time and count come from completed focus sessions`() = runTest {
        session(startedAgo = 1 * day, planned = 25 * minute)
        session(startedAgo = 2 * day, planned = 50 * minute)
        session(startedAgo = 3 * day, planned = 25 * minute)

        getWeeklyStats().test {
            val stats = awaitItem()
            assertEquals(100 * minute, stats.totalFocusMs)
            assertEquals(3, stats.completedSessions)
        }
    }

    @Test
    fun `cancelled and still running sessions are not counted`() = runTest {
        session(startedAgo = day, status = SessionStatus.COMPLETED)
        session(startedAgo = day, status = SessionStatus.CANCELLED)
        session(startedAgo = day, status = SessionStatus.RUNNING)

        getWeeklyStats().test {
            assertEquals(1, awaitItem().completedSessions)
        }
    }

    @Test
    fun `breaks are not counted as focus time`() = runTest {
        session(startedAgo = day, type = SessionType.FOCUS)
        session(startedAgo = day, type = SessionType.BREAK, planned = 5 * minute)

        getWeeklyStats().test {
            val stats = awaitItem()
            assertEquals(1, stats.completedSessions)
            assertEquals(25 * minute, stats.totalFocusMs)
        }
    }

    @Test
    fun `sessions older than seven days are left out`() = runTest {
        session(startedAgo = 6 * day + 23 * 60 * minute) // 7 günden az
        session(startedAgo = 8 * day)
        session(startedAgo = 30 * day)

        getWeeklyStats().test {
            assertEquals(1, awaitItem().completedSessions)
        }
    }

    @Test
    fun `a session exactly seven days old is still included`() = runTest {
        session(startedAgo = 7 * day)

        getWeeklyStats().test {
            assertEquals(1, awaitItem().completedSessions)
        }
    }

    @Test
    fun `distractors are ordered by notification count`() = runTest {
        repeat(2) { notification("com.gmail", "Gmail") }
        repeat(5) { notification("com.whatsapp", "WhatsApp") }
        repeat(3) { notification("com.insta", "Instagram") }

        getWeeklyStats().test {
            val distractors = awaitItem().topDistractors
            assertEquals(listOf("WhatsApp", "Instagram", "Gmail"), distractors.map { it.appLabel })
            assertEquals(listOf(5, 3, 2), distractors.map { it.count })
        }
    }

    @Test
    fun `distractors with equal counts are ordered by app label ignoring case`() = runTest {
        notification("com.z", "zoom")
        notification("com.b", "Bravo")
        notification("com.a", "Alfa")

        getWeeklyStats().test {
            assertEquals(listOf("Alfa", "Bravo", "zoom"), awaitItem().topDistractors.map { it.appLabel })
        }
    }

    @Test
    fun `only the five busiest apps are listed`() = runTest {
        (1..8).forEach { i -> repeat(i) { notification("com.app$i", "App $i") } }

        getWeeklyStats().test {
            val distractors = awaitItem().topDistractors
            assertEquals(5, distractors.size)
            assertEquals(listOf(8, 7, 6, 5, 4), distractors.map { it.count })
        }
    }

    @Test
    fun `old notifications are not counted as distractors`() = runTest {
        notification("com.recent", ago = 2 * day)
        notification("com.old", ago = 9 * day)

        getWeeklyStats().test {
            assertEquals(listOf("com.recent"), awaitItem().topDistractors.map { it.packageName })
        }
    }

    @Test
    fun `the label comes from the newest notification of an app`() = runTest {
        notification("com.x", label = "com.x", ago = 3 * day)
        notification("com.x", label = "Güzel Uygulama", ago = day)

        getWeeklyStats().test {
            assertEquals("Güzel Uygulama", awaitItem().topDistractors.single().appLabel)
        }
    }

    @Test
    fun `the stats update when a session completes or a notification is captured`() = runTest {
        getWeeklyStats().test {
            assertEquals(0, awaitItem().completedSessions)

            session(startedAgo = day)
            assertEquals(1, awaitItem().completedSessions)

            notification("com.whatsapp", "WhatsApp")
            assertEquals(1, awaitItem().topDistractors.size)
        }
    }
}
