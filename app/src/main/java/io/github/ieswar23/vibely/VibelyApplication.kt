package io.github.ieswar23.vibely

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import io.github.ieswar23.vibely.data.sync.FeedSyncWorker
import javax.inject.Inject

@HiltAndroidApp
class VibelyApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        FeedSyncWorker.syncNow(this)
        FeedSyncWorker.schedulePeriodic(this)
    }
}
