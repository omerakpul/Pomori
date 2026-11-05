package com.pomori.platform.locale

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import com.pomori.domain.model.AppLanguage
import java.util.Locale

/**
 * Uygulama içi dil seçimi. Dil, DataStore yerine SharedPreferences'ta tutulur: Activity/Servis'in
 * `attachBaseContext`'i, Hilt ve DataStore hazır olmadan, eşzamanlı olarak dili bilmek zorundadır.
 *
 * Her giriş noktası (Application, Activity, Servis, Receiver) kendi bağlamını [wrap] ile sarar; böylece ekranlar ve
 * bildirimler aynı dilde olur. Android 13+'ın sistem içi uygulama dili ayarından bağımsız, her sürümde çalışır.
 */
object AppLocale {
    private const val PREFS = "app_locale"
    private const val KEY_LANGUAGE = "language"

    fun read(context: Context): AppLanguage {
        val stored = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_LANGUAGE, null)
        return AppLanguage.entries.firstOrNull { it.name == stored } ?: AppLanguage.SYSTEM
    }

    fun write(context: Context, language: AppLanguage) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_LANGUAGE, language.name).apply()
    }

    /** Seçili dile göre ayarlanmış bağlamı döner. Sistem dili seçiliyse bağlama dokunmaz. */
    fun wrap(base: Context): Context {
        val language = read(base)
        val systemLocale = Resources.getSystem().configuration.locales[0]
        val locale = language.tag?.let { Locale.forLanguageTag(it) } ?: systemLocale
        // Tarih/gün adı gibi Locale.getDefault() kullanan yerler de seçili dili izlesin.
        Locale.setDefault(locale)
        if (language == AppLanguage.SYSTEM) return base
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return base.createConfigurationContext(config)
    }
}
