package com.pomori.testutil

import com.pomori.domain.model.ActiveSession
import com.pomori.domain.model.CapturedNotification
import com.pomori.domain.model.ExceptionRule
import com.pomori.domain.model.Distractor
import com.pomori.domain.model.FocusSession
import com.pomori.domain.model.InstalledApp
import com.pomori.domain.model.SessionStatus
import com.pomori.domain.model.SessionType
import com.pomori.domain.model.SettingsDefaults
import com.pomori.domain.repository.ActiveSessionStore
import com.pomori.domain.repository.ExceptionRepository
import com.pomori.domain.repository.InstalledApps
import com.pomori.domain.repository.NotificationRepository
import com.pomori.domain.repository.SessionRepository
import com.pomori.domain.repository.SettingsStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeSessionRepository : SessionRepository {
    val rows = linkedMapOf<Long, FocusSession>()
    private var nextId = 1L

    /** Her değişiklikte artar; gözlenen akışlar bununla yeniden hesaplanır. */
    private val version = MutableStateFlow(0)

    override suspend fun insert(session: FocusSession): Long {
        val id = nextId++
        rows[id] = session.copy(id = id)
        version.value++
        return id
    }

    override suspend fun update(session: FocusSession) {
        rows[session.id] = session
        version.value++
    }

    override suspend fun getById(id: Long): FocusSession? = rows[id]

    // Gerçek DAO sorgusuyla aynı koşullar: odak + tamamlandı + startedAt >= from, yeniden eskiye.
    override fun observeCompletedFocusSince(from: Long): Flow<List<FocusSession>> =
        version.map {
            rows.values
                .filter { it.type == SessionType.FOCUS && it.status == SessionStatus.COMPLETED && it.startedAt >= from }
                .sortedByDescending { it.startedAt }
        }
}

class FakeActiveSessionStore(initial: ActiveSession? = null) : ActiveSessionStore {
    private val state = MutableStateFlow(initial)

    override val active: Flow<ActiveSession?> = state
    override suspend fun current(): ActiveSession? = state.value
    override suspend fun set(session: ActiveSession) {
        state.value = session
    }

    override suspend fun clear() {
        state.value = null
    }
}

class FakeSettingsStore(
    initialOnboardingDone: Boolean = false,
    focusMinutes: Int = SettingsDefaults.FOCUS_MINUTES,
    breakMinutes: Int = SettingsDefaults.BREAK_MINUTES,
    retentionDays: Int = SettingsDefaults.RETENTION_DAYS,
) : SettingsStore {
    private val done = MutableStateFlow(initialOnboardingDone)
    private val focus = MutableStateFlow(focusMinutes)
    private val breakTime = MutableStateFlow(breakMinutes)
    private val retention = MutableStateFlow(retentionDays)

    override val onboardingDone: Flow<Boolean> = done
    override val focusMinutes: Flow<Int> = focus
    override val breakMinutes: Flow<Int> = breakTime
    override val retentionDays: Flow<Int> = retention

    override suspend fun setOnboardingDone() {
        done.value = true
    }

    // Gerçek uygulamayla aynı: aralık dışı değerler en yakın sınıra çekilir.
    override suspend fun setFocusMinutes(minutes: Int) {
        focus.value = minutes.coerceIn(SettingsDefaults.FOCUS_RANGE)
    }

    override suspend fun setBreakMinutes(minutes: Int) {
        breakTime.value = minutes.coerceIn(SettingsDefaults.BREAK_RANGE)
    }

    override suspend fun setRetentionDays(days: Int) {
        retention.value = days.coerceIn(SettingsDefaults.RETENTION_RANGE)
    }
}

class FakeExceptionRepository(initial: List<ExceptionRule> = emptyList()) : ExceptionRepository {
    private val state = MutableStateFlow(initial)
    val rules: List<ExceptionRule> get() = state.value

    /** Doluysa kurallar okunurken bu istisna fırlar (veritabanı hatası simülasyonu). */
    var readFailure: Exception? = null

    override fun observeAll(): Flow<List<ExceptionRule>> =
        state.map { rules -> readFailure?.let { throw it } ?: rules }

    override suspend fun add(rule: ExceptionRule): Boolean {
        val normalized = when (rule) {
            is ExceptionRule.Package -> ExceptionRule.Package(rule.value.trim())
            is ExceptionRule.Sender -> ExceptionRule.Sender(rule.value.trim())
        }
        if (normalized.value.isEmpty() || normalized in rules) return false
        state.value = rules + normalized
        return true
    }

    override suspend fun remove(rule: ExceptionRule) {
        state.value = rules - rule
    }
}

class FakeInstalledApps(var apps: List<InstalledApp> = emptyList()) : InstalledApps {
    override suspend fun launchableApps(): List<InstalledApp> = apps
}

class FakeNotificationRepository : NotificationRepository {
    private val state = MutableStateFlow<List<CapturedNotification>>(emptyList())
    val rows: List<CapturedNotification> get() = state.value

    /** Doluysa bir sonraki insert bu istisnayı fırlatır (disk dolu, FK ihlali vb. simülasyonu). */
    var failWith: Exception? = null

    override suspend fun insert(notification: CapturedNotification): Long {
        failWith?.let { throw it }
        val id = rows.size + 1L
        state.value = rows + notification.copy(id = id)
        return id
    }

    override fun observeBySession(sessionId: Long): Flow<List<CapturedNotification>> =
        state.map { all -> all.filter { it.sessionId == sessionId }.sortedByDescending { it.postedAt } }

    // Gerçek sorguyla aynı: sayı azalan, sonra etiket (harf duyarsız), sonra paket; etiket en yeni satırdan.
    override fun observeTopDistractors(from: Long, limit: Int): Flow<List<Distractor>> =
        state.map { all ->
            all.filter { it.postedAt >= from }
                .groupBy { it.packageName }
                .map { (pkg, items) ->
                    Distractor(pkg, items.maxBy { it.postedAt }.appLabel, items.size)
                }
                .sortedWith(
                    compareByDescending<Distractor> { it.count }
                        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.appLabel }
                        .thenBy { it.packageName },
                )
                .take(limit)
        }
}
