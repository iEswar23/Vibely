package io.github.ieswar23.vibely.data.paging

import androidx.room.withTransaction
import io.github.ieswar23.vibely.data.local.VibelyDatabase
import io.github.ieswar23.vibely.data.local.entity.FeedRemoteKeyEntity
import io.github.ieswar23.vibely.data.remote.dto.FeedPageDto
import io.github.ieswar23.vibely.data.remote.toEntity
import io.github.ieswar23.vibely.util.Clock
import javax.inject.Inject

/**
 * Persists one page of the remote feed (posts, side-loaded authors and paging keys) atomically.
 * Shared by [FeedRemoteMediator] and the background [io.github.ieswar23.vibely.data.sync.FeedSyncWorker].
 */
class FeedCacheWriter @Inject constructor(
    private val database: VibelyDatabase,
    private val clock: Clock,
) {
    /**
     * @param isRefresh when true, previously cached pages leave the feed so it restarts from page 1
     * (the posts themselves stay cached, keeping likes and saves intact).
     */
    suspend fun write(page: FeedPageDto, isRefresh: Boolean) {
        val now = clock.now()
        database.withTransaction {
            if (isRefresh) database.feedRemoteKeyDao().clearRemotePages()
            database.userDao().insertIgnore(page.authors.map { it.toEntity() })
            database.postDao().insertIgnore(page.posts.map { it.toEntity() })
            val next = if (page.hasMore) page.page + 1 else null
            database.feedRemoteKeyDao().insertAll(
                page.posts.map { FeedRemoteKeyEntity(postId = it.id, page = page.page, nextPage = next, fetchedAt = now) },
            )
        }
    }
}
