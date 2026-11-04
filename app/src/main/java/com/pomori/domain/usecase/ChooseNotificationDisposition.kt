package com.pomori.domain.usecase

import com.pomori.domain.repository.SettingsStore
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** Yakalanan bildirime sistemde ne yapılacağı. */
sealed interface NotificationDisposition {
    /** Eski davranış: bildirim sistemden silinir, yalnızca özette yaşar. */
    data object Remove : NotificationDisposition

    /**
     * Bildirim, seans bitene kadar ertelenir; süre dolunca sistem orijinal bildirimi (kendi simgesi, eylemleri ve
     * dokunma hedefiyle) paneline geri koyar.
     *
     * @property durationMs ertelemenin süresi.
     * @property returnsAt bildirimin geri döneceği an (epoch ms); geri dönen bildirimi tanımak için kullanılır.
     */
    data class Snooze(val durationMs: Long, val returnsAt: Long) : NotificationDisposition
}

/**
 * Yakalanan bildirim silinmez, seans bitişine kadar ertelenir: böylece bildirim kaybolmaz ve geri geldiğinde dokununca
 * orijinal sohbete gidilir. Kullanıcı Ayarlar'dan kapatırsa eski davranışa (silme) dönülür.
 */
class ChooseNotificationDisposition @Inject constructor(
    private val settings: SettingsStore,
) {
    /**
     * @param sessionEndsAt çalışan odak seansının planlanan bitişi (epoch ms)
     * @param now şu an (epoch ms)
     */
    suspend operator fun invoke(sessionEndsAt: Long, now: Long): NotificationDisposition {
        if (!settings.restoreNotifications.first()) return NotificationDisposition.Remove
        // Tampon: bildirim, seans durumu "bitti"ye geçtikten sonra dönsün. Aksi halde saat kayması yüzünden seans
        // hâlâ sürüyor görünür ve dönen bildirim yeniden yakalanıp yine ertelenirdi.
        val duration = (sessionEndsAt - now + RETURN_BUFFER_MS).coerceAtLeast(MIN_SNOOZE_MS)
        return NotificationDisposition.Snooze(durationMs = duration, returnsAt = now + duration)
    }

    companion object {
        const val RETURN_BUFFER_MS = 3_000L

        /** Çok kısa erteleme sistemde güvenilir değil; seans bitmek üzereyken bile en az bu kadar. */
        const val MIN_SNOOZE_MS = 5_000L
    }
}
