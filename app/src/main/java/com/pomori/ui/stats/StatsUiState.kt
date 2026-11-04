package com.pomori.ui.stats

import com.pomori.domain.model.DayFocus
import com.pomori.domain.model.Distractor
import com.pomori.domain.model.GoalProgress
import com.pomori.domain.model.LabelFocus

data class StatsUiState(
    /** İlk veri gelene kadar true; ekran bu sürede "veri yok" durumunu göstermemelidir. */
    val isLoading: Boolean = true,
    val totalFocusMs: Long = 0,
    val completedSessions: Int = 0,
    /** Bildirim sayısına göre azalan sıralı. */
    val topDistractors: List<Distractor> = emptyList(),
    /** Bugün dahil son 7 gün, eskiden yeniye. */
    val dailyFocus: List<DayFocus> = emptyList(),
    /** Etikete göre kırılım; hiç etiket yoksa boş ve bölüm gizlenir. */
    val labelBreakdown: List<LabelFocus> = emptyList(),
    /** Günlük hedef ve seri; hedef kapalıysa kart gösterilmez. */
    val goal: GoalProgress = GoalProgress(),
) {
    /** Grafik, en az bir günde odak süresi varken anlamlıdır; hepsi sıfırsa boş bir şablon göstermeyiz. */
    val hasChart: Boolean get() = dailyFocus.any { it.focusMs > 0 }

    /** Çubuk yüksekliklerinin oranı için en uzun günün süresi. */
    val maxDailyFocusMs: Long get() = dailyFocus.maxOfOrNull { it.focusMs } ?: 0L

    /** Yükleme bittiği halde hem tamamlanan seans hem bildirim yoksa henüz gösterilecek bir şey yoktur. */
    val isEmpty: Boolean get() = !isLoading && completedSessions == 0 && topDistractors.isEmpty()

    /** Toplam süre "X sa Y dk" yazılırken kullanılan parçalar. */
    val focusHours: Int get() = (totalFocusMs / MS_PER_HOUR).toInt()
    val focusMinutes: Int get() = ((totalFocusMs / MS_PER_MINUTE) % 60).toInt()

    val maxLabelFocusMs: Long get() = labelBreakdown.maxOfOrNull { it.focusMs } ?: 0L

    /** Çubukların oranı için en kalabalık uygulamanın bildirim sayısı. */
    val maxDistractorCount: Int get() = topDistractors.maxOfOrNull { it.count } ?: 0

    private companion object {
        const val MS_PER_MINUTE = 60_000L
        const val MS_PER_HOUR = 60 * MS_PER_MINUTE
    }
}
