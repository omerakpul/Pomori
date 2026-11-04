package com.pomori.platform.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * POST_NOTIFICATIONS çalışma zamanı izni yalnızca Android 13 (API 33) ve üstünde vardır.
 * Daha eski sürümlerde bildirimler izinsiz gösterilir.
 */
object NotificationPermission {
    val isRuntimePermissionRequired: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    fun isGranted(context: Context): Boolean =
        !isRuntimePermissionRequired ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
}
