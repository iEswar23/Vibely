package io.github.ieswar23.vibely.data.repository

import androidx.paging.PagingData
import io.github.ieswar23.vibely.domain.model.HashtagStat
import io.github.ieswar23.vibely.domain.model.Post
import io.github.ieswar23.vibely.domain.model.PostDraft
import kotlinx.coroutines.flow.Flow

interface PostRepository {
    /** Infinite home feed backed by Room + [androidx.paging.RemoteMediator]. */
    fun feed(): Flow<PagingData<Post>>

    fun observePost(postId: String): Flow<Post?>
    fun postsByAuthor(userId: String): Flow<List<Post>>
    fun postCountByAuthor(userId: String): Flow<Int>
    fun bookmarkedPosts(): Flow<List<Post>>
    fun explorePosts(): Flow<List<Post>>
    fun postsByHashtag(tag: String): Flow<List<Post>>
    fun trendingHashtags(limit: Int = 12): Flow<List<HashtagStat>>

    suspend fun refreshExplore(): Result<Unit>
    suspend fun refreshUserPosts(userId: String): Result<Unit>

    /** Optimistically toggles a like: Room is updated first and rolled back if the API call fails. */
    suspend fun setLiked(postId: String, liked: Boolean): Result<Unit>

    /** Optimistically toggles a bookmark, rolled back on failure. */
    suspend fun setBookmarked(postId: String, bookmarked: Boolean): Result<Unit>

    /**
     * Optimistically records the user's vote: the tally in Room changes immediately and is rolled back
     * if the API call fails. Fails with [io.github.ieswar23.vibely.domain.PollVoteException] when the
     * user has already voted or the poll has closed (one vote per user).
     */
    suspend fun vote(postId: String, optionIndex: Int): Result<Unit>

    suspend fun createPost(draft: PostDraft): Result<Post>
}
