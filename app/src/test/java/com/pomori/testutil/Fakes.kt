package com.pomori.testutil

import com.pomori.domain.model.ActiveSession
import com.pomori.domain.model.FocusSession
import com.pomori.domain.repository.ActiveSessionStore
import com.pomori.domain.repository.SessionRepository
import com.pomori.domain.repository.SettingsStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSessionRepository : SessionRepository {
    val rows = linkedMapOf<Long, FocusSession>()
    private var nextId = 1L

    override suspend fun insert(session: FocusSession): Long {
        val id = nextId++
        rows[id] = session.copy(id = id)
        return id
    }

    override suspend fun update(session: FocusSession) {
        rows[session.id] = session
    }

    override suspend fun getById(id: Long): FocusSession? = rows[id]
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

class FakeSettingsStore(initialOnboardingDone: Boolean = false) : SettingsStore {
    private val done = MutableStateFlow(initialOnboardingDone)

    override val onboardingDone: Flow<Boolean> = done

    override suspend fun setOnboardingDone() {
        done.value = true
    }
}
