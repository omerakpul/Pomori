package com.pomori.domain.usecase

import com.pomori.domain.model.FocusSession
import com.pomori.domain.model.LabelFocus

/**
 * Tamamlanan odak seanslarını etikete göre toplar. Etiketler büyük/küçük harf ayırmadan birleşir
 * ("Ders" ve "ders" aynı etiket; ilk görülen yazım korunur). Süreye göre azalan sıralıdır, etiketsizler en sonda.
 * Hiçbir seansta etiket yoksa boş döner: tek satırlık "Etiketsiz" kırılımı bilgi vermez.
 */
fun labelBreakdown(sessions: List<FocusSession>): List<LabelFocus> {
    if (sessions.none { it.label != null }) return emptyList()
    val groups = sessions.groupBy { it.label?.lowercase() }
    return groups.values
        .map { list ->
            LabelFocus(
                label = list.first().label,
                focusMs = list.sumOf { it.plannedDurationMs },
                sessions = list.size,
            )
        }
        .sortedWith(compareBy<LabelFocus> { it.label == null }.thenByDescending { it.focusMs })
}
