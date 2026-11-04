package com.pomori.domain.model

import java.time.LocalDate

/** Bir uygulamanın, seçilen dönemde yakalanan bildirim sayısı. */
data class Distractor(
    val packageName: String,
    val appLabel: String,
    val count: Int,
)

/** Bir takvim gününde tamamlanan odak seanslarının toplam süresi. */
data class DayFocus(
    val date: LocalDate,
    val focusMs: Long,
)

/** Bir etikete ait tamamlanan odak süresi. [label] null ise etiketsiz seanslardır. */
data class LabelFocus(
    val label: String?,
    val focusMs: Long,
    val sessions: Int,
)

/** Son 7 günün özeti. */
data class WeeklyStats(
    /** Tamamlanan odak seanslarının toplam süresi, ms. İptal edilenler sayılmaz. */
    val totalFocusMs: Long,
    val completedSessions: Int,
    /** Bildirim sayısına göre azalan sıralı, en fazla `GetWeeklyStats.TOP_DISTRACTORS` kadar. */
    val topDistractors: List<Distractor>,
    /** Bugün dahil son 7 takvim günü, eskiden yeniye; seans olmayan gün 0 ms ile yer alır. */
    val dailyFocus: List<DayFocus> = emptyList(),
    /** Etikete göre kırılım, süreye göre azalan; etiketsizler en sonda. Hiç etiket kullanılmadıysa boş. */
    val labelBreakdown: List<LabelFocus> = emptyList(),
)
