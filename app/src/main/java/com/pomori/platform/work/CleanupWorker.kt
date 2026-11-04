package com.pomori.platform.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pomori.domain.usecase.CleanupOldNotifications
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

/**
 * Günlük temizlik işi. İş mantığı içermez: saklama süresini aşan bildirimleri silmeyi
 * [CleanupOldNotifications]'a bırakır.
 */
@HiltWorker
class CleanupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val cleanup: CleanupOldNotifications,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        cleanup()
        Result.success()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        // Geçici bir veritabanı hatası olabilir: birkaç kez yeniden dene, sonra vazgeç. Periyodik iş
        // bir sonraki günde zaten yeniden çalışır.
        if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
    }

    private companion object {
        const val MAX_ATTEMPTS = 3
    }
}
