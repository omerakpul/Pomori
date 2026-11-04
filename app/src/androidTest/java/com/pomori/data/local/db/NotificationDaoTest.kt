package com.pomori.data.local.db

import android.database.sqlite.SQLiteConstraintException
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

/** Gerçek SQLite üzerinde (bellek içi): yabancı anahtar, CASCADE ve sıralama davranışı. */
@RunWith(AndroidJUnit4::class)
class NotificationDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var sessions: SessionDao
    private lateinit var notifications: NotificationDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).build()
        sessions = db.sessionDao()
        notifications = db.notificationDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun newSession(): Long = sessions.insert(
        FocusSessionEntity(
            type = SessionType.FOCUS,
            startedAt = 1_000,
            plannedDurationMs = 1_500_000,
            endedAt = null,
            status = SessionStatus.RUNNING,
        ),
    )

    private fun notification(sessionId: Long, postedAt: Long, title: String? = "t") =
        CapturedNotificationEntity(
            sessionId = sessionId,
            packageName = "com.whatsapp",
            appLabel = "WhatsApp",
            title = title,
            text = null,
            postedAt = postedAt,
        )

    @Test
    fun observeBySession_returnsNewestFirst() = runBlocking {
        val session = newSession()
        notifications.insert(notification(session, postedAt = 100, title = "eski"))
        notifications.insert(notification(session, postedAt = 300, title = "yeni"))
        notifications.insert(notification(session, postedAt = 200, title = "orta"))

        val titles = notifications.observeBySession(session).first().map { it.title }

        assertEquals(listOf("yeni", "orta", "eski"), titles)
    }

    @Test
    fun observeBySession_breaksTiesByNewestId() = runBlocking {
        val session = newSession()
        notifications.insert(notification(session, postedAt = 100, title = "ilk"))
        notifications.insert(notification(session, postedAt = 100, title = "ikinci"))

        val titles = notifications.observeBySession(session).first().map { it.title }

        assertEquals(listOf("ikinci", "ilk"), titles)
    }

    @Test
    fun observeBySession_ignoresOtherSessions() = runBlocking {
        val first = newSession()
        val second = newSession()
        notifications.insert(notification(first, postedAt = 100))
        notifications.insert(notification(second, postedAt = 200))

        val rows = notifications.observeBySession(second).first()

        assertEquals(listOf(second), rows.map { it.sessionId })
    }

    @Test
    fun insert_nullTitleAndTextArePersisted() = runBlocking {
        val session = newSession()
        notifications.insert(notification(session, postedAt = 100, title = null))

        val row = notifications.observeBySession(session).first().single()

        assertEquals(null, row.title)
        assertEquals(null, row.text)
    }

    @Test(expected = SQLiteConstraintException::class)
    fun insert_withoutExistingSession_isRejectedByForeignKey() {
        runBlocking { notifications.insert(notification(sessionId = 999, postedAt = 100)) }
    }

    @Test
    fun deletingSession_cascadesToItsNotifications() = runBlocking {
        val kept = newSession()
        val deleted = newSession()
        notifications.insert(notification(kept, postedAt = 100))
        notifications.insert(notification(deleted, postedAt = 200))
        notifications.insert(notification(deleted, postedAt = 300))

        db.openHelper.writableDatabase.execSQL("DELETE FROM focus_sessions WHERE id = $deleted")

        assertTrue(notifications.observeBySession(deleted).first().isEmpty())
        assertEquals(1, notifications.observeBySession(kept).first().size)
    }

    // --- deleteOlderThan ---

    @Test
    fun deleteOlderThan_removesOnlyStrictlyOlderRows() = runBlocking {
        val session = newSession()
        notifications.insert(notification(session, postedAt = 99, title = "eski"))
        notifications.insert(notification(session, postedAt = 100, title = "sınırda"))
        notifications.insert(notification(session, postedAt = 101, title = "yeni"))

        val deleted = notifications.deleteOlderThan(100)

        assertEquals(1, deleted)
        assertEquals(
            listOf("yeni", "sınırda"),
            notifications.observeBySession(session).first().map { it.title },
        )
    }

    @Test
    fun deleteOlderThan_worksAcrossSessions() = runBlocking {
        val first = newSession()
        val second = newSession()
        notifications.insert(notification(first, postedAt = 10))
        notifications.insert(notification(second, postedAt = 20))
        notifications.insert(notification(second, postedAt = 500))

        val deleted = notifications.deleteOlderThan(100)

        assertEquals(2, deleted)
        assertTrue(notifications.observeBySession(first).first().isEmpty())
        assertEquals(1, notifications.observeBySession(second).first().size)
    }

    @Test
    fun deleteOlderThan_returnsZeroWhenNothingMatches() = runBlocking {
        val session = newSession()
        notifications.insert(notification(session, postedAt = 500))

        assertEquals(0, notifications.deleteOlderThan(100))
        assertEquals(1, notifications.observeBySession(session).first().size)
    }

    @Test
    fun deleteOlderThan_doesNotTouchSessions() = runBlocking {
        val session = newSession()
        notifications.insert(notification(session, postedAt = 10))

        notifications.deleteOlderThan(100)

        // Seans kaydı kalır: istatistikler bildirimler silinse de tamamlanan seansları sayar.
        assertEquals(session, sessions.getById(session)?.id)
    }
}
