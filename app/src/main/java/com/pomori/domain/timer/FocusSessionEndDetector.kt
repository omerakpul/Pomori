package com.pomori.domain.timer

/**
 * Çalışan odak seansının bittiği (iptal edildi, başka seansa geçildi) anı yakalar.
 *
 * Aktif seans her değiştiğinde [onFocusSessionChanged] çağrılır; o an çalışan odak seansının id'si (yoksa null) verilir.
 * Önceki seans artık çalışmıyorsa onun id'sini döner: o seansta ertelenen bildirimler panele hemen geri getirilmeli.
 *
 * Saf bir sınıftır (Android'e bağlı değil), böylece her geçiş JVM testinde sınanır.
 */
class FocusSessionEndDetector {

    private var running: Long? = null

    /**
     * @param focusSessionId şu an çalışan odak seansı; boşta, molada ya da süresi dolmuşsa null
     * @return az önce sona eren odak seansının id'si, ya da sona eren yoksa null
     */
    fun onFocusSessionChanged(focusSessionId: Long?): Long? {
        val previous = running
        running = focusSessionId
        return previous?.takeIf { it != focusSessionId }
    }
}
