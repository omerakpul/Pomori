package com.pomori.platform.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import com.pomori.domain.model.InstalledApp
import com.pomori.domain.repository.InstalledApps
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Launcher'da görünen uygulamaları listeler. Android 11+ paket görünürlüğü için manifestteki
 * `<queries>` launcher intent'i bu sorguyu kapsar (QUERY_ALL_PACKAGES gerekmez).
 */
class PackageManagerInstalledApps @Inject constructor(
    @ApplicationContext private val context: Context,
) : InstalledApps {

    override suspend fun launchableApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        packageManager.queryLauncherActivities(launcher)
            .map { InstalledApp(it.activityInfo.packageName, it.loadLabel(packageManager).toString()) }
            .filter { it.packageName != context.packageName }
            .distinctBy { it.packageName }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
    }
}

private fun PackageManager.queryLauncherActivities(intent: Intent): List<ResolveInfo> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        queryIntentActivities(intent, 0)
    }
