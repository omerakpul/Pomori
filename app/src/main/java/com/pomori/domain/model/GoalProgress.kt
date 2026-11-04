package com.pomori.domain.model

/** Günlük hedefin durumu. [goalMinutes] 0 ise hedef kapalıdır. */
data class GoalProgress(
    val goalMinutes: Int = 0,
    val todayMs: Long = 0,
    val streak: Int = 0,
) {
    val enabled: Boolean get() = goalMinutes > 0
    val goalMs: Long get() = goalMinutes * 60_000L
    val reached: Boolean get() = enabled && todayMs >= goalMs

    /** 0..1; hedef kapalıysa 0. */
    val fraction: Float get() = if (!enabled) 0f else (todayMs.toFloat() / goalMs).coerceIn(0f, 1f)
}
