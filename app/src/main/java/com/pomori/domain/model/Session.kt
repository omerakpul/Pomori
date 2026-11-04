package com.pomori.domain.model

enum class SessionType { FOCUS, BREAK }

enum class SessionStatus { RUNNING, COMPLETED, CANCELLED }

data class FocusSession(
    val id: Long = 0,
    val type: SessionType,
    val startedAt: Long,
    val plannedDurationMs: Long,
    val endedAt: Long? = null,
    val status: SessionStatus,
)

/**
 * Süreç ölse bile kaybolmaması gereken aktif seans kaydı (DataStore'da tutulur).
 * Sayaç "kalan süreyi azalt" yerine bu bitiş zamanından hesaplanır.
 */
data class ActiveSession(
    val sessionId: Long,
    val type: SessionType,
    val endAt: Long,
    val plannedDurationMs: Long,
    /** Yalnızca mola seanslarında anlamlı: bu mola "uzun mola" mı? Eski kayıtlarda yoktur, false sayılır. */
    val isLongBreak: Boolean = false,
)
