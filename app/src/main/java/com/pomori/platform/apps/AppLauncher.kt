package com.pomori.platform.apps

import android.content.Context
import android.content.Intent

/**
 * Özetteki bir bildirimin uygulamasını açar. Yakalanan bildirim sistemden kaldırıldığı için orijinal
 * sohbete/e-postaya doğrudan gidilemez (bkz. README, bilinen kısıtlar); uygulamanın ana ekranı açılır.
 * Android 11+ paket görünürlüğü için manifestteki launcher `<queries>` kaydı bu aramayı kapsar.
 */
object AppLauncher {

    /** Uygulama açıldıysa true. Silinmiş ya da başlatılamayan uygulamada false döner, hata fırlatmaz. */
    fun open(context: Context, packageName: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        return try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (_: Exception) {
            false
        }
    }
}
