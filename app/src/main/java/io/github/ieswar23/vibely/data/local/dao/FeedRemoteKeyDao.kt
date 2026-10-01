package io.github.ieswar23.vibely.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.ieswar23.vibely.data.local.entity.FeedRemoteKeyEntity

@Dao
interface FeedRemoteKeyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(keys: List<FeedRemoteKeyEntity>)

    /** When the remote feed was last fetched (locally published posts don't count). */
    @Query("SELECT MAX(fetchedAt) FROM feed_remote_keys WHERE page > 0")
    suspend fun lastFetchedAt(): Long?

    /** Highest API page cached so far, or null before the first successful load. */
    @Query("SELECT MAX(page) FROM feed_remote_keys WHERE page > 0")
    suspend fun lastPage(): Int?

    @Query("SELECT EXISTS(SELECT 1 FROM feed_remote_keys WHERE page > 0 AND nextPage IS NULL)")
    suspend fun isEndReached(): Boolean

    /** Drops remote paging state on refresh while keeping the user's own posts in the feed. */
    @Query("DELETE FROM feed_remote_keys WHERE page > 0")
    suspend fun clearRemotePages()
}
