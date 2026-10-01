package io.github.ieswar23.vibely.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import io.github.ieswar23.vibely.data.local.VibelyDatabase
import io.github.ieswar23.vibely.data.local.entity.PostWithAuthor
import io.github.ieswar23.vibely.data.remote.VibelyApi
import io.github.ieswar23.vibely.util.Clock
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit

/**
 * Network + database paging for the home feed. Room is the single source of truth: the mediator
 * only fetches pages from [VibelyApi] and writes them to the cache, while the UI pages through
 * Room's [androidx.paging.PagingSource].
 */
@OptIn(ExperimentalPagingApi::class)
class FeedRemoteMediator(
    private val api: VibelyApi,
    private val database: VibelyDatabase,
    private val writer: FeedCacheWriter,
    private val clock: Clock,
) : RemoteMediator<Int, PostWithAuthor>() {

    override suspend fun initialize(): InitializeAction {
        val lastFetch = database.feedRemoteKeyDao().lastFetchedAt() ?: return InitializeAction.LAUNCH_INITIAL_REFRESH
        return if (clock.now() - lastFetch < CACHE_TIMEOUT_MS) {
            InitializeAction.SKIP_INITIAL_REFRESH
        } else {
            InitializeAction.LAUNCH_INITIAL_REFRESH
        }
    }

    override suspend fun load(loadType: LoadType, state: PagingState<Int, PostWithAuthor>): MediatorResult {
        val page = when (loadType) {
            LoadType.REFRESH -> FIRST_PAGE
            LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
            LoadType.APPEND -> {
                val keys = database.feedRemoteKeyDao()
                if (keys.isEndReached()) return MediatorResult.Success(endOfPaginationReached = true)
                // Nothing fetched yet: the pending REFRESH will load the first page.
                val lastPage = keys.lastPage() ?: return MediatorResult.Success(endOfPaginationReached = false)
                lastPage + 1
            }
        }
        return try {
            val response = api.getFeed(page = page, limit = state.config.pageSize)
            writer.write(response, isRefresh = loadType == LoadType.REFRESH)
            MediatorResult.Success(endOfPaginationReached = !response.hasMore)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            MediatorResult.Error(e)
        }
    }

    companion object {
        const val FIRST_PAGE = 1
        val CACHE_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(30)
    }
}
