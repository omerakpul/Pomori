package com.pomori.domain

/**
 * Zaman kaynağı. Domain katmanı sistem saatine doğrudan bağlanmaz;
 * testlerde sahte saat verilebilsin diye arayüz üzerinden okunur.
 */
fun interface Clock {
    /** Şu anki zaman, epoch milisaniye. */
    fun now(): Long
}
