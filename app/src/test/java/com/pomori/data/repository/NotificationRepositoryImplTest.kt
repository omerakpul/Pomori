package com.pomori.data.repository

import app.cash.turbine.test
import com.pomori.data.local.db.CapturedNotificationEntity
import com.pomori.data.local.db.DistractorRow
import com.pomori.data.local.db.NotificationDao
import com.pomori.domain.model.CapturedNotification
import com.pomori.domain.model.Distractor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationRepositoryImplTest {

    private val dao = FakeNotificationDao()
    private val repository = NotificationRepositoryImpl(dao)

    private fun notification(
        sessionId: Long = 1,
        title: String? = "Ayşe",
        text: String? = "Toplantı 15:00'te",
        postedAt: Long = 1_000,
    ) = CapturedNotification(
        sessionId = sessionId,
        packageName = "com.whatsapp",
        appLabel = "WhatsApp",
        title = title,
        text = text,
        postedAt = postedAt,
    )

    @Test
    fun `insert stores every field and returns the generated id`() = runTest {
        val id = repository.insert(notification())

        assertEquals(1L, id)
        assertEquals(
            CapturedNotificationEntity(
                id = 1,
                sessionId = 1,
                packageName = "com.whatsapp",
                appLabel = "WhatsApp",
                title = "Ayşe",
                text = "Toplantı 15:00'te",
                postedAt = 1_000,
            ),
            dao.rows.single(),
        )
    }

    @Test
    fun `null title and text survive the round trip`() = runTest {
        repository.insert(notification(title = null, text = null))

        repository.observeBySession(1).test {
            val item = awaitItem().single()
            assertEquals(null, item.title)
            assertEquals(null, item.text)
        }
    }

    @Test
    fun `observe maps entities back to domain models`() = runTest {
        repository.insert(notification(postedAt = 5_000))

        repository.observeBySession(1).test {
            assertEquals(
                listOf(notification(postedAt = 5_000).copy(id = 1)),
                awaitItem(),
            )
        }
    }

    @Test
    fun `observe only emits notifications of the requested session`() = runTest {
        repository.insert(notification(sessionId = 1))
        repository.insert(notification(sessionId = 2))

        repository.observeBySession(2).test {
            assertEquals(listOf(2L), awaitItem().map { it.sessionId })
        }
    }

    @Test
    fun `observe emits again when a new notification is inserted`() = runTest {
        repository.observeBySession(1).test {
            assertEquals(emptyList<CapturedNotification>(), awaitItem())

            repository.insert(notification())

            assertEquals(1, awaitItem().size)
        }
    }

    @Test
    fun `top distractors map the query rows to domain distractors`() = runTest {
        dao.distractorRows.value = listOf(
            DistractorRow(packageName = "com.whatsapp", appLabel = "WhatsApp", cnt = 6, lastAt = 10),
            DistractorRow(packageName = "com.google.android.gm", appLabel = "Gmail", cnt = 2, lastAt = 5),
        )

        repository.observeTopDistractors(from = 0, limit = 5).test {
            assertEquals(
                listOf(
                    Distractor("com.whatsapp", "WhatsApp", 6),
                    Distractor("com.google.android.gm", "Gmail", 2),
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `deleting old notifications passes the cutoff and returns the deleted count`() = runTest {
        repository.insert(notification(postedAt = 100))
        repository.insert(notification(postedAt = 200))
        repository.insert(notification(postedAt = 300))

        val deleted = repository.deleteOlderThan(250)

        assertEquals(2, deleted)
        assertEquals(250L, dao.lastDeleteCutoff)
    }

    @Test
    fun `top distractors pass the window and the limit to the query`() = runTest {
        repository.observeTopDistractors(from = 1234, limit = 3).test { awaitItem() }

        assertEquals(1234L to 3, dao.lastDistractorQuery)
    }

    private class FakeNotificationDao : NotificationDao {
        private val state = MutableStateFlow<List<CapturedNotificationEntity>>(emptyList())
        val distractorRows = MutableStateFlow<List<DistractorRow>>(emptyList())
        var lastDistractorQuery: Pair<Long, Int>? = null
        val rows: List<CapturedNotificationEntity> get() = state.value

        var lastDeleteCutoff: Long? = null

        override suspend fun deleteOlderThan(olderThan: Long): Int {
            lastDeleteCutoff = olderThan
            val before = state.value.size
            state.value = state.value.filter { it.postedAt >= olderThan }
            return before - state.value.size
        }

        override suspend fun insert(notification: CapturedNotificationEntity): Long {
            val id = rows.size + 1L
            state.value = rows + notification.copy(id = id)
            return id
        }

        override fun observeBySession(sessionId: Long): Flow<List<CapturedNotificationEntity>> =
            state.map { all -> all.filter { it.sessionId == sessionId } }

        override fun observeTopDistractors(from: Long, limit: Int): Flow<List<DistractorRow>> {
            lastDistractorQuery = from to limit
            return distractorRows
        }
    }
}
