package com.pomori.data.repository

import app.cash.turbine.test
import com.pomori.data.local.db.CapturedNotificationEntity
import com.pomori.data.local.db.NotificationDao
import com.pomori.domain.model.CapturedNotification
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

    private class FakeNotificationDao : NotificationDao {
        private val state = MutableStateFlow<List<CapturedNotificationEntity>>(emptyList())
        val rows: List<CapturedNotificationEntity> get() = state.value

        override suspend fun insert(notification: CapturedNotificationEntity): Long {
            val id = rows.size + 1L
            state.value = rows + notification.copy(id = id)
            return id
        }

        override fun observeBySession(sessionId: Long): Flow<List<CapturedNotificationEntity>> =
            state.map { all -> all.filter { it.sessionId == sessionId } }
    }
}
