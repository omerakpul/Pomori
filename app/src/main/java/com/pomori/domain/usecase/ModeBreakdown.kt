package com.pomori.domain.usecase

import com.pomori.domain.model.FocusSession
import com.pomori.domain.model.ModeFocus

/**
 * Tamamlanan odak seanslarını çalıştıkları moda göre toplar; mod adı etiket görevi görür ("Ders" modunda
 * çalışılan süre = Ders'e harcanan süre). Süreye göre azalan sıralıdır, modu bilinmeyenler (null) en sonda.
 * Seanslar tek bir modda toplandıysa boş döner: tek satırlık kırılım bilgi vermez.
 */
fun modeBreakdown(sessions: List<FocusSession>): List<ModeFocus> {
    val groups = sessions.groupBy { it.presetId }
    if (groups.size < 2) return emptyList()
    return groups.map { (presetId, list) ->
        ModeFocus(presetId = presetId, focusMs = list.sumOf { it.plannedDurationMs }, sessions = list.size)
    }.sortedWith(compareBy<ModeFocus> { it.presetId == null }.thenByDescending { it.focusMs })
}
