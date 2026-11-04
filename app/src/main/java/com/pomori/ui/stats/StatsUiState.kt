package com.pomori.ui.stats

import com.pomori.domain.model.Distractor

data class StatsUiState(
    /** İlk veri gelene kadar true; ekran bu sürede "veri yok" durumunu göstermemelidir. */
    val isLoading: Boolean = true,
    val totalFocusMs: Long = 0,
    val completedSessions: Int = 0,
    /** Bildirim sayısına göre azalan sıralı. */
    val topDistractors: List<Distractor> = emptyList(),
) {
    /** Yükleme bittiği halde hem tamamlanan seans hem bildirim yoksa henüz gösterilecek bir şey yoktur. */
    val isEmpty: Boolean get() = !isLoading && completedSessions == 0 && topDistractors.isEmpty()

    /** Toplam süre "X sa Y dk" yazılırken kullanılan parçalar. */
    val focusHours: Int get() = (totalFocusMs / MS_PER_HOUR).toInt()
    val focusMinutes: Int get() = ((totalFocusMs / MS_PER_MINUTE) % 60).toInt()

    /** Çubukların oranı için en kalabalık uygulamanın bildirim sayısı. */
    val maxDistractorCount: Int get() = topDistractors.maxOfOrNull { it.count } ?: 0

    private companion object {
        const val MS_PER_MINUTE = 60_000L
        const val MS_PER_HOUR = 60 * MS_PER_MINUTE
    }
}
