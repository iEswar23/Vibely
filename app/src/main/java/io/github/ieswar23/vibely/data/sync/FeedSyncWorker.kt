package io.github.ieswar23.vibely.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.github.ieswar23.vibely.data.repository.SyncRepository
import java.util.concurrent.TimeUnit

/** Keeps the offline cache warm: users, stories, activity and the first page of the feed. */
@HiltWorker
class FeedSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncRepository: SyncRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result =
        syncRepository.syncAll().fold(
            onSuccess = { Result.success() },
            onFailure = { if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure() },
        )

    companion object {
        private const val PERIODIC_WORK = "vibely-feed-sync"
        private const val ONE_TIME_WORK = "vibely-feed-sync-now"
        private const val MAX_ATTEMPTS = 3

        private val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        /** Schedules a refresh every 6 hours; existing schedules are kept across app starts. */
        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<FeedSyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(PERIODIC_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        /** One-off sync, used on app start so stories and activity are ready quickly. */
        fun syncNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<FeedSyncWorker>()
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(ONE_TIME_WORK, ExistingWorkPolicy.KEEP, request)
        }
    }
}
