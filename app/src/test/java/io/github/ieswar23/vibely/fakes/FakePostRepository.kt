package io.github.ieswar23.vibely.fakes

import androidx.paging.PagingData
import io.github.ieswar23.vibely.data.repository.PostRepository
import io.github.ieswar23.vibely.domain.model.HashtagStat
import io.github.ieswar23.vibely.domain.model.Post
import io.github.ieswar23.vibely.domain.model.PostDraft
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** In-memory [PostRepository] that mimics the optimistic update + rollback semantics of the real one. */
class FakePostRepository(initial: List<Post> = emptyList()) : PostRepository {

    val posts = MutableStateFlow(initial.associateBy { it.id })
    var failNextWrite = false
    val likeCalls = mutableListOf<Pair<String, Boolean>>()
    val bookmarkCalls = mutableListOf<Pair<String, Boolean>>()
    val createdDrafts = mutableListOf<PostDraft>()
    val refreshedUserPosts = mutableListOf<String>()

    override fun feed(): Flow<PagingData<Post>> = flowOf(PagingData.empty())

    override fun observePost(postId: String): Flow<Post?> = posts.map { it[postId] }

    override fun postsByAuthor(userId: String): Flow<List<Post>> =
        posts.map { all -> all.values.filter { it.author.id == userId }.sortedByDescending { it.createdAt } }

    override fun postCountByAuthor(userId: String): Flow<Int> = postsByAuthor(userId).map { it.size }

    override fun bookmarkedPosts(): Flow<List<Post>> = posts.map { all -> all.values.filter { it.isBookmarked } }

    override fun explorePosts(): Flow<List<Post>> = posts.map { all -> all.values.sortedByDescending { it.likeCount } }

    override fun postsByHashtag(tag: String): Flow<List<Post>> = posts.map { all -> all.values.filter { tag in it.hashtags } }

    override fun trendingHashtags(limit: Int): Flow<List<HashtagStat>> = flowOf(emptyList())

    override suspend fun refreshExplore(): Result<Unit> = Result.success(Unit)

    override suspend fun refreshUserPosts(userId: String): Result<Unit> {
        refreshedUserPosts += userId
        return Result.success(Unit)
    }

    override suspend fun setLiked(postId: String, liked: Boolean): Result<Unit> {
        likeCalls += postId to liked
        val before = posts.value[postId] ?: return Result.failure(NoSuchElementException(postId))
        posts.update { it + (postId to before.copy(isLiked = liked, likeCount = before.likeCount + if (liked) 1 else -1)) }
        return if (consumeFailure()) {
            posts.update { it + (postId to before) }
            Result.failure(IllegalStateException("network"))
        } else {
            Result.success(Unit)
        }
    }

    override suspend fun setBookmarked(postId: String, bookmarked: Boolean): Result<Unit> {
        bookmarkCalls += postId to bookmarked
        val before = posts.value[postId] ?: return Result.failure(NoSuchElementException(postId))
        posts.update { it + (postId to before.copy(isBookmarked = bookmarked)) }
        return if (consumeFailure()) {
            posts.update { it + (postId to before) }
            Result.failure(IllegalStateException("network"))
        } else {
            Result.success(Unit)
        }
    }

    override suspend fun createPost(draft: PostDraft): Result<Post> {
        createdDrafts += draft
        if (consumeFailure()) return Result.failure(IllegalStateException("network"))
        val post = TestData.post(id = "new-${createdDrafts.size}", author = TestData.me, caption = draft.caption)
        posts.update { it + (post.id to post) }
        return Result.success(post)
    }

    private fun consumeFailure(): Boolean = failNextWrite.also { failNextWrite = false }
}
