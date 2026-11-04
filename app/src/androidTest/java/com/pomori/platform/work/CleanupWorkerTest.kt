package com.pomori.platform.work

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.pomori.data.local.db.AppDatabase
import com.pomori.data.local.db.CapturedNotificationEntity
import com.pomori.data.local.db.FocusSessionEntity
import com.pomori.data.repository.NotificationRepositoryImpl
import com.pomori.domain.Clock
import com.pomori.domain.model.CapturedNotification
import com.pomori.domain.model.Distractor
import com.pomori.domain.model.SessionStatus
import com.pomori.domain.model.SessionType
import com.pomori.domain.repository.NotificationRepository
import com.pomori.domain.repository.SettingsStore
import com.pomori.domain.usecase.CleanupOldNotifications
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * CleanupWorker.doWork(), gerçek Room (bellek içi) ve gerçek CleanupOldNotifications üzerinde.
 * Hilt fabrikasını taklit eden basit bir WorkerFactory ile oluşturulur; çalışma zamanlaması
 * WorkManager'ın işidir, burada işin kendisi sınanır.
 */
@RunWith(AndroidJUnit4::class)
class CleanupWorkerTest {

    private val day = 24 * 60 * 60 * 1000L
    private val now = 100 * day
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private class FixedRetention(private val days: Int) : SettingsStore {
        override val onboardingDone: Flow<Boolean> = flowOf(true)
        override val focusMinutes: Flow<Int> = flowOf(25)
        override val breakMinutes: Flow<Int> = flowOf(5)
        override val longBreakMinutes: Flow<Int> = flowOf(15)
        override val selectedPresetId: Flow<Long?> = flowOf(null)
        override val restoreNotifications: Flow<Boolean> = flowOf(true)
        override val retentionDays: Flow<Int> = flowOf(days)
        override suspend fun setOnboardingDone() = Unit
        override suspend fun setFocusMinutes(minutes: Int) = Unit
        override suspend fun setBreakMinutes(minutes: Int) = Unit
        override suspend fun setLongBreakMinutes(minutes: Int) = Unit
        override suspend fun setSelectedPresetId(id: Long) = Unit
        override suspend fun setRestoreNotifications(enabled: Boolean) = Unit
        override suspend fun setRetentionDays(days: Int) = Unit
    }

    private class FailingRepository : NotificationRepository {
        override suspend fun insert(notification: CapturedNotification): Long = error("kullanılmaz")
        override suspend fun deleteOlderThan(olderThan: Long): Int = throw IllegalStateException("db unavailable")
        override fun observeBySession(sessionId: Long): Flow<List<CapturedNotification>> = error("kullanılmaz")
        override fun observeTopDistractors(from: Long, limit: Int): Flow<List<Distractor>> = error("kullanılmaz")
    }

    private fun worker(cleanup: CleanupOldNotifications, attempt: Int = 0): CleanupWorker =
        TestListenableWorkerBuilder<CleanupWorker>(context)
            .setRunAttemptCount(attempt)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters,
                ): ListenableWorker = CleanupWorker(appContext, workerParameters, cleanup)
            })
            .build()

    private fun realCleanup(retentionDays: Int) = CleanupOldNotifications(
        FixedRetention(retentionDays),
        NotificationRepositoryImpl(db.notificationDao()),
        Clock { now },
    )

    private suspend fun seed(agesInDays: List<Long>) {
        val session = db.sessionDao().insert(
            FocusSessionEntity(
                type = SessionType.FOCUS,
                startedAt = 0,
                plannedDurationMs = 1_500_000,
                endedAt = null,
                status = SessionStatus.COMPLETED,
            ),
        )
        agesInDays.forEach { age ->
            db.notificationDao().insert(
                CapturedNotificationEntity(
                    sessionId = session,
                    packageName = "com.test",
                    appLabel = "Test",
                    title = "$age gün",
                    text = null,
                    postedAt = now - age * day,
                ),
            )
        }
    }

    private suspend fun remainingTitles(): List<String?> {
        val session = db.sessionDao().observeCompletedSince(0).first().single().id
        return db.notificationDao().observeBySession(session).first().map { it.title }
    }

    @Test
    fun doWork_deletesNotificationsOlderThanTheRetentionPeriodAndSucceeds() = runBlocking {
        seed(listOf(1, 5, 8, 20))

        val result = worker(realCleanup(retentionDays = 7)).doWork()

        assertEquals(Result.success(), result)
        assertEquals(listOf<String?>("1 gün", "5 gün"), remainingTitles().sortedBy { it })
    }

    @Test
    fun doWork_withNothingToDelete_succeedsAndKeepsEverything() = runBlocking {
        seed(listOf(1, 2))

        val result = worker(realCleanup(retentionDays = 7)).doWork()

        assertEquals(Result.success(), result)
        assertEquals(2, remainingTitles().size)
    }

    @Test
    fun doWork_whenTheDatabaseFails_retriesAtFirst() = runBlocking {
        val cleanup = CleanupOldNotifications(FixedRetention(7), FailingRepository(), Clock { now })

        assertEquals(Result.retry(), worker(cleanup, attempt = 0).doWork())
        assertEquals(Result.retry(), worker(cleanup, attempt = 2).doWork())
    }

    @Test
    fun doWork_givesUpAfterThreeAttempts() = runBlocking {
        val cleanup = CleanupOldNotifications(FixedRetention(7), FailingRepository(), Clock { now })

        assertEquals(Result.failure(), worker(cleanup, attempt = 3).doWork())
    }
}
