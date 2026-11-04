package com.pomori.domain.model

/** Odak seansı sırasında yakalanıp özet için saklanan tek bir bildirim. */
data class CapturedNotification(
    val id: Long = 0,
    val sessionId: Long,
    val packageName: String,
    /** Yakalama anında çözülen uygulama adı; uygulama sonradan silinse de özet okunabilir kalır. */
    val appLabel: String,
    val title: String?,
    val text: String?,
    val postedAt: Long,
)
