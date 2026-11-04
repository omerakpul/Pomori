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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Gerçek SQLite üzerinde (bellek içi): mod tablosu ve seansların mod bağlantısı. */
@RunWith(AndroidJUnit4::class)
class PresetDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: PresetDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).build()
        dao = db.presetDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun preset(name: String, focus: Int = 25) =
        PresetEntity(name = name, focusMinutes = focus, breakMinutes = 5, longBreakMinutes = 15, longBreakEvery = 4)

    @Test
    fun insert_returnsGeneratedIdsAndKeepsCreationOrder() = runBlocking {
        val a = dao.insert(preset("B"))
        val b = dao.insert(preset("A"))

        assertEquals(1L, a)
        assertEquals(2L, b)
        assertEquals(listOf("B", "A"), dao.observeAll().first().map { it.name })
    }

    @Test
    fun update_changesTheRow() = runBlocking {
        val id = dao.insert(preset("Ders", focus = 40))

        dao.update(preset("Kodlama", focus = 25).copy(id = id))

        val saved = dao.observeAll().first().single()
        assertEquals("Kodlama", saved.name)
        assertEquals(25, saved.focusMinutes)
    }

    @Test
    fun delete_removesOnlyThatRow() = runBlocking {
        val a = dao.insert(preset("A"))
        dao.insert(preset("B"))

        dao.delete(a)

        assertEquals(listOf("B"), dao.observeAll().first().map { it.name })
        assertEquals(1, dao.count())
    }

    @Test
    fun count_isZeroOnAnEmptyTable() = runBlocking {
        assertEquals(0, dao.count())
    }

    @Test
    fun aSession_remembersWhichPresetItRanWith() = runBlocking {
        val presetId = dao.insert(preset("Ders"))
        val sessionId = db.sessionDao().insert(
            FocusSessionEntity(
                type = SessionType.FOCUS,
                startedAt = 1_000,
                plannedDurationMs = 60_000,
                endedAt = null,
                status = SessionStatus.RUNNING,
                presetId = presetId,
            ),
        )

        assertEquals(presetId, db.sessionDao().getById(sessionId)?.presetId)
    }

    @Test
    fun aSessionWithoutAPreset_keepsNull() = runBlocking {
        val sessionId = db.sessionDao().insert(
            FocusSessionEntity(
                type = SessionType.FOCUS,
                startedAt = 1_000,
                plannedDurationMs = 60_000,
                endedAt = null,
                status = SessionStatus.RUNNING,
            ),
        )

        assertNull(db.sessionDao().getById(sessionId)?.presetId)
    }

    @Test
    fun deletingAPreset_doesNotDeleteItsSessions() = runBlocking {
        val presetId = dao.insert(preset("Ders"))
        dao.insert(preset("Yedek")) // son mod değil
        val sessionId = db.sessionDao().insert(
            FocusSessionEntity(
                type = SessionType.FOCUS,
                startedAt = 1_000,
                plannedDurationMs = 60_000,
                endedAt = 2_000,
                status = SessionStatus.COMPLETED,
                presetId = presetId,
            ),
        )

        dao.delete(presetId)

        // Geçmiş istatistik silinen modla birlikte kaybolmamalı.
        assertEquals(sessionId, db.sessionDao().getById(sessionId)?.id)
    }
}
