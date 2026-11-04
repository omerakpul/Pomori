package com.pomori.data.local.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pomori.domain.model.SessionStatus
import com.pomori.domain.model.SessionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** İstatistik sorguları gerçek SQLite üzerinde: gruplama, sıralama, pencere ve etiket seçimi. */
@RunWith(AndroidJUnit4::class)
class StatsQueriesTest {

    private lateinit var db: AppDatabase
    private lateinit var sessions: SessionDao
    private lateinit var notifications: NotificationDao
    private var sessionId = 0L

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).build()
        sessions = db.sessionDao()
        notifications = db.notificationDao()
        sessionId = runBlocking { newSession(SessionType.FOCUS, SessionStatus.RUNNING, startedAt = 0) }
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun newSession(type: SessionType, status: SessionStatus, startedAt: Long) = sessions.insert(
        FocusSessionEntity(
            type = type,
            startedAt = startedAt,
            plannedDurationMs = 1_500_000,
            endedAt = null,
            status = status,
        ),
    )

    private suspend fun captured(pkg: String, label: String, at: Long) {
        notifications.insert(
            CapturedNotificationEntity(
                sessionId = sessionId,
                packageName = pkg,
                appLabel = label,
                title = null,
                text = null,
                postedAt = at,
            ),
        )
    }

    private suspend fun top(from: Long = 0, limit: Int = 5) = notifications.observeTopDistractors(from, limit).first()

    // --- observeTopDistractors ---

    @Test
    fun distractors_areEmptyWithoutNotifications() = runBlocking {
        assertTrue(top().isEmpty())
    }

    @Test
    fun distractors_countPerPackageAndOrderByCountDescending() = runBlocking {
        repeat(2) { captured("com.gmail", "Gmail", 100L + it) }
        repeat(5) { captured("com.whatsapp", "WhatsApp", 100L + it) }
        repeat(3) { captured("com.insta", "Instagram", 100L + it) }

        val rows = top()

        assertEquals(listOf("com.whatsapp", "com.insta", "com.gmail"), rows.map { it.packageName })
        assertEquals(listOf(5, 3, 2), rows.map { it.cnt })
    }

    @Test
    fun distractors_tiesAreOrderedByLabelIgnoringCase() = runBlocking {
        captured("com.z", "zoom", 100)
        captured("com.b", "Bravo", 100)
        captured("com.a", "alfa", 100)

        assertEquals(listOf("alfa", "Bravo", "zoom"), top().map { it.appLabel })
    }

    @Test
    fun distractors_sameLabelFallsBackToPackageName() = runBlocking {
        captured("com.b", "Mail", 100)
        captured("com.a", "Mail", 100)

        assertEquals(listOf("com.a", "com.b"), top().map { it.packageName })
    }

    @Test
    fun distractors_areLimited() = runBlocking {
        (1..8).forEach { i -> repeat(i) { captured("com.app$i", "App $i", 100L + it) } }

        val rows = top(limit = 3)

        assertEquals(listOf(8, 7, 6), rows.map { it.cnt })
    }

    @Test
    fun distractors_onlyCountNotificationsFromTheWindow() = runBlocking {
        captured("com.recent", "Recent", at = 1_000)
        captured("com.old", "Old", at = 999)

        val rows = top(from = 1_000)

        assertEquals(listOf("com.recent"), rows.map { it.packageName })
    }

    @Test
    fun distractors_windowStartIsInclusive() = runBlocking {
        captured("com.edge", "Edge", at = 1_000)

        assertEquals(1, top(from = 1_000).size)
        assertEquals(0, top(from = 1_001).size)
    }

    @Test
    fun distractors_takeTheLabelOfTheNewestNotification() = runBlocking {
        captured("com.x", "com.x", at = 100) // etiket çözülememişti, paket adına düşmüştü
        captured("com.x", "Güzel Uygulama", at = 300)
        captured("com.x", "Eski Ad", at = 200)

        val row = top().single()

        assertEquals("Güzel Uygulama", row.appLabel)
        assertEquals(3, row.cnt)
        assertEquals(300L, row.lastAt)
    }

    // --- observeCompletedSince ---

    @Test
    fun completed_onlyReturnsCompletedFocusSessions() = runBlocking {
        val done = newSession(SessionType.FOCUS, SessionStatus.COMPLETED, startedAt = 100)
        newSession(SessionType.FOCUS, SessionStatus.CANCELLED, startedAt = 100)
        newSession(SessionType.FOCUS, SessionStatus.RUNNING, startedAt = 100)
        newSession(SessionType.BREAK, SessionStatus.COMPLETED, startedAt = 100)

        val rows = sessions.observeCompletedSince(0).first()

        assertEquals(listOf(done), rows.map { it.id })
    }

    @Test
    fun completed_respectsTheWindowStartInclusively() = runBlocking {
        newSession(SessionType.FOCUS, SessionStatus.COMPLETED, startedAt = 999)
        val atEdge = newSession(SessionType.FOCUS, SessionStatus.COMPLETED, startedAt = 1_000)
        val later = newSession(SessionType.FOCUS, SessionStatus.COMPLETED, startedAt = 2_000)

        val rows = sessions.observeCompletedSince(1_000).first()

        assertEquals(setOf(atEdge, later), rows.map { it.id }.toSet())
    }

    @Test
    fun completed_areNewestFirst() = runBlocking {
        val old = newSession(SessionType.FOCUS, SessionStatus.COMPLETED, startedAt = 100)
        val new = newSession(SessionType.FOCUS, SessionStatus.COMPLETED, startedAt = 300)
        val mid = newSession(SessionType.FOCUS, SessionStatus.COMPLETED, startedAt = 200)

        val rows = sessions.observeCompletedSince(0).first()

        assertEquals(listOf(new, mid, old), rows.map { it.id })
    }
}
