package com.pomori.platform.notification

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pomori'nin ertelediği bildirimlerin defteri (bildirim anahtarı → hangi seans, ne zaman dönecek).
 *
 * İki işe yarar:
 * 1. Ertelenen bildirim süre dolunca sistem tarafından aynı anahtarla yeniden gönderilir ve dinleyiciye yeni bir
 *    bildirim gibi gelir. Defter olmadan, seans erken iptal edilip hemen yenisi başlatılmışsa, dönen bildirim yeni
 *    seansa yeniden yakalanır ve yine ertelenirdi. Deftere bakılıp "bu bizim ertelediğimiz" denince dokunulmaz.
 * 2. Seans iptal edilince o seansta ertelenen bildirimlerin hangileri olduğu bilinir ve hemen geri getirilir.
 *
 * Bellekte tutulur: süreç ölürse defter boşalır ve en kötü ihtimalle bir bildirim bir kez daha yakalanır
 * (kayıp yok, yalnızca çift kayıt). Veritabanı şeması için bu risk değmez.
 */
@Singleton
class SnoozeLedger @Inject constructor() {

    private data class Entry(val sessionId: Long, val returnsAt: Long)

    private val entries = ConcurrentHashMap<String, Entry>()

    /** Bildirimin, `sessionId` seansında `returnsAt` anına kadar ertelendiğini kaydeder. Eski kayıtları temizler. */
    fun remember(key: String, sessionId: Long, returnsAt: Long, now: Long) {
        entries.entries.removeIf { it.value.returnsAt < now - STALE_AFTER_MS }
        entries[key] = Entry(sessionId, returnsAt)
    }

    /**
     * Bildirim bu defterdeyse true döner ve kaydı siler (dönen bildirim). Dönüş saatinden önce de true döner:
     * kullanıcı ertelenenler listesinden bildirimi erken geri getirmiş olabilir; her iki durumda da dokunulmaz.
     */
    fun consume(key: String): Boolean = entries.remove(key) != null

    /** Bu seansta ertelenmiş ve henüz dönmemiş bildirimlerin anahtarları. */
    fun keysOf(sessionId: Long): List<String> =
        entries.entries.filter { it.value.sessionId == sessionId }.map { it.key }

    /** Bildirimin dönüş anını günceller (erken geri getirmede kısaltılır). Kayıtlı değilse hiçbir şey yapmaz. */
    fun reschedule(key: String, returnsAt: Long) {
        entries.computeIfPresent(key) { _, old -> old.copy(returnsAt = returnsAt) }
    }

    private companion object {
        /** Dönüş zamanından bu kadar sonra hâlâ dönmemiş kayıt (uygulama bildirimi iptal etmiştir) artık geçersiz. */
        const val STALE_AFTER_MS = 60 * 60 * 1000L
    }
}
