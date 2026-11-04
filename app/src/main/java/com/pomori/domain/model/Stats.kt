package com.pomori.domain.model

/** Bir uygulamanın, seçilen dönemde yakalanan bildirim sayısı. */
data class Distractor(
    val packageName: String,
    val appLabel: String,
    val count: Int,
)

/** Son 7 günün özeti. */
data class WeeklyStats(
    /** Tamamlanan odak seanslarının toplam süresi, ms. İptal edilenler sayılmaz. */
    val totalFocusMs: Long,
    val completedSessions: Int,
    /** Bildirim sayısına göre azalan sıralı, en fazla `GetWeeklyStats.TOP_DISTRACTORS` kadar. */
    val topDistractors: List<Distractor>,
)
