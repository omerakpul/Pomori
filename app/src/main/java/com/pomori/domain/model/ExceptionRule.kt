package com.pomori.domain.model

/** Kullanıcının özete almak istemediği, yani seans sırasında bile olduğu gibi gelmesi gereken bildirimler. */
sealed interface ExceptionRule {
    val value: String

    /** Bu uygulamanın bütün bildirimleri (paket adı birebir). */
    data class Package(override val value: String) : ExceptionRule

    /** Başlığında bu gönderen adı geçen bildirimler (büyük/küçük harf fark etmez). */
    data class Sender(override val value: String) : ExceptionRule
}
