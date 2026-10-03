package io.github.ieswar23.vibely.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import io.github.ieswar23.vibely.data.local.entity.PollOptionEntity
import io.github.ieswar23.vibely.data.local.entity.PostEntity
import io.github.ieswar23.vibely.data.local.entity.PostWithAuthor
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {

    /** Home feed: posts fetched through the feed API plus posts published on this device. */
    @Transaction
    @Query("SELECT * FROM posts WHERE id IN (SELECT postId FROM feed_remote_keys) ORDER BY createdAt DESC")
    fun feedPagingSource(): PagingSource<Int, PostWithAuthor>

    /**
     * The mock backend does not persist writes, so locally-applied interaction state (likes,
     * bookmarks, comment counts) is authoritative: already-cached posts are never overwritten.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(posts: List<PostEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(post: PostEntity)

    @Query("SELECT * FROM posts WHERE id = :id")
    suspend fun getPost(id: String): PostEntity?

    @Transaction
    @Query("SELECT * FROM posts WHERE id = :id")
    suspend fun getPostWithAuthor(id: String): PostWithAuthor?

    @Transaction
    @Query("SELECT * FROM posts WHERE id = :id")
    fun observePost(id: String): Flow<PostWithAuthor?>

    @Transaction
    @Query("SELECT * FROM posts WHERE authorId = :authorId ORDER BY createdAt DESC")
    fun observeByAuthor(authorId: String): Flow<List<PostWithAuthor>>

    @Query("SELECT COUNT(*) FROM posts WHERE authorId = :authorId")
    fun observeCountByAuthor(authorId: String): Flow<Int>

    @Transaction
    @Query("SELECT * FROM posts WHERE isBookmarked = 1 ORDER BY bookmarkedAt DESC")
    fun observeBookmarked(): Flow<List<PostWithAuthor>>

    @Transaction
    @Query("SELECT * FROM posts ORDER BY likeCount DESC, createdAt DESC")
    fun observeByPopularity(): Flow<List<PostWithAuthor>>

    @Transaction
    @Query("SELECT * FROM posts WHERE hashtags LIKE '%,' || :tag || ',%' ORDER BY likeCount DESC")
    fun observeByHashtag(tag: String): Flow<List<PostWithAuthor>>

    @Query("SELECT hashtags FROM posts WHERE hashtags != ','")
    fun observeHashtagColumns(): Flow<List<String>>

    @Query("UPDATE posts SET isLiked = :liked, likeCount = MAX(0, likeCount + :delta) WHERE id = :id")
    suspend fun updateLike(id: String, liked: Boolean, delta: Int)

    @Query("UPDATE posts SET isBookmarked = :bookmarked, bookmarkedAt = :at WHERE id = :id")
    suspend fun updateBookmark(id: String, bookmarked: Boolean, at: Long?)

    @Query("UPDATE posts SET commentCount = MAX(0, commentCount + :delta) WHERE id = :id")
    suspend fun adjustCommentCount(id: String, delta: Int)

    /** Writes a poll's tallies and the user's vote (or clears it again on rollback). */
    @Query("UPDATE posts SET poll_options = :options, poll_votedOption = :votedOption WHERE id = :id AND poll_question IS NOT NULL")
    suspend fun updatePollVote(id: String, options: List<PollOptionEntity>, votedOption: Int?)
}
