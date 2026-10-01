package io.github.ieswar23.vibely.data.repository

import io.github.ieswar23.vibely.data.local.VibelyDatabase
import io.github.ieswar23.vibely.data.paging.FeedCacheWriter
import io.github.ieswar23.vibely.data.paging.FeedRemoteMediator
import io.github.ieswar23.vibely.data.remote.VibelyApi
import io.github.ieswar23.vibely.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** App-wide cache orchestration used by the background worker and the "reset" setting. */
interface SyncRepository {
    /** Refreshes users, stories, activity and the first feed page. */
    suspend fun syncAll(): Result<Unit>

    /** Logout-style reset: wipes the cache and preferences, then re-downloads the seed data. */
    suspend fun resetAll(): Result<Unit>
}

@Singleton
class DefaultSyncRepository @Inject constructor(
    private val api: VibelyApi,
    private val database: VibelyDatabase,
    private val feedCacheWriter: FeedCacheWriter,
    private val userRepository: UserRepository,
    private val storyRepository: StoryRepository,
    private val activityRepository: ActivityRepository,
    private val settingsRepository: SettingsRepository,
    @IoDispatcher private val io: CoroutineDispatcher,
) : SyncRepository {

    override suspend fun syncAll(): Result<Unit> = withContext(io) {
        runCatchingNonCancellation {
            userRepository.refreshUsers().getOrThrow()
            storyRepository.refresh().getOrThrow()
            activityRepository.refresh().getOrThrow()
            val firstPage = api.getFeed(FeedRemoteMediator.FIRST_PAGE, PostRepositoryImpl.PAGE_SIZE)
            // Not a refresh: never drop pages the user may be scrolling through right now.
            feedCacheWriter.write(firstPage, isRefresh = false)
        }
    }

    override suspend fun resetAll(): Result<Unit> = withContext(io) {
        database.clearAllTables()
        settingsRepository.clear()
        syncAll()
    }
}
