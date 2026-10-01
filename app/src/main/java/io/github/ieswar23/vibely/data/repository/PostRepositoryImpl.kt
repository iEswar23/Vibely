package io.github.ieswar23.vibely.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.filter
import androidx.paging.map
import androidx.room.withTransaction
import io.github.ieswar23.vibely.data.local.VibelyDatabase
import io.github.ieswar23.vibely.data.local.entity.FeedRemoteKeyEntity
import io.github.ieswar23.vibely.data.local.dao.PostDao
import io.github.ieswar23.vibely.data.local.dao.UserDao
import io.github.ieswar23.vibely.data.local.toDomain
import io.github.ieswar23.vibely.data.local.toDomainPosts
import io.github.ieswar23.vibely.data.paging.FeedCacheWriter
import io.github.ieswar23.vibely.data.paging.FeedRemoteMediator
import io.github.ieswar23.vibely.data.remote.VibelyApi
import io.github.ieswar23.vibely.data.remote.decodeHashtags
import io.github.ieswar23.vibely.data.remote.dto.CreatePostRequest
import io.github.ieswar23.vibely.data.remote.toEntity
import io.github.ieswar23.vibely.di.IoDispatcher
import io.github.ieswar23.vibely.domain.model.HashtagStat
import io.github.ieswar23.vibely.domain.model.Post
import io.github.ieswar23.vibely.domain.model.PostDraft
import io.github.ieswar23.vibely.util.Clock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostRepositoryImpl @Inject constructor(
    private val api: VibelyApi,
    private val database: VibelyDatabase,
    private val postDao: PostDao,
    private val userDao: UserDao,
    private val cacheWriter: FeedCacheWriter,
    private val clock: Clock,
    @IoDispatcher private val io: CoroutineDispatcher,
) : PostRepository {

    @OptIn(ExperimentalPagingApi::class)
    override fun feed(): Flow<PagingData<Post>> = Pager(
        config = PagingConfig(
            pageSize = PAGE_SIZE,
            prefetchDistance = PAGE_SIZE / 2,
            initialLoadSize = PAGE_SIZE * 2,
            enablePlaceholders = false,
        ),
        remoteMediator = FeedRemoteMediator(api, database, cacheWriter, clock),
        pagingSourceFactory = { postDao.feedPagingSource() },
    ).flow.map { paging ->
        paging.filter { it.author != null }.map { requireNotNull(it.toDomain()) }
    }

    override fun observePost(postId: String): Flow<Post?> =
        postDao.observePost(postId).map { it?.toDomain() }.flowOn(io)

    override fun postsByAuthor(userId: String): Flow<List<Post>> =
        postDao.observeByAuthor(userId).map { it.toDomainPosts() }.flowOn(io)

    override fun postCountByAuthor(userId: String): Flow<Int> = postDao.observeCountByAuthor(userId)

    override fun bookmarkedPosts(): Flow<List<Post>> =
        postDao.observeBookmarked().map { it.toDomainPosts() }.flowOn(io)

    override fun explorePosts(): Flow<List<Post>> =
        postDao.observeByPopularity().map { it.toDomainPosts() }.flowOn(io)

    override fun postsByHashtag(tag: String): Flow<List<Post>> =
        postDao.observeByHashtag(tag.removePrefix("#").lowercase()).map { it.toDomainPosts() }.flowOn(io)

    override fun trendingHashtags(limit: Int): Flow<List<HashtagStat>> =
        postDao.observeHashtagColumns().map { columns ->
            columns.asSequence()
                .flatMap { decodeHashtags(it).asSequence() }
                .groupingBy { it }
                .eachCount()
                .entries
                .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
                .take(limit)
                .map { HashtagStat(it.key, it.value) }
        }.flowOn(io)

    override suspend fun refreshExplore(): Result<Unit> = withContext(io) {
        runCatchingNonCancellation {
            ensureUsers()
            postDao.insertIgnore(api.getExplore().map { it.toEntity() })
        }
    }

    override suspend fun refreshUserPosts(userId: String): Result<Unit> = withContext(io) {
        runCatchingNonCancellation {
            ensureUsers()
            postDao.insertIgnore(api.getUserPosts(userId).map { it.toEntity() })
        }
    }

    override suspend fun setLiked(postId: String, liked: Boolean): Result<Unit> = withContext(io) {
        val current = postDao.getPost(postId) ?: return@withContext Result.failure(NoSuchElementException(postId))
        if (current.isLiked == liked) return@withContext Result.success(Unit)
        val delta = if (liked) 1 else -1
        postDao.updateLike(postId, liked, delta)
        runCatchingNonCancellation {
            val response = if (liked) api.likePost(postId) else api.unlikePost(postId)
            check(response.success) { response.message ?: "Couldn't update like" }
        }.onFailure { postDao.updateLike(postId, !liked, -delta) }
    }

    override suspend fun setBookmarked(postId: String, bookmarked: Boolean): Result<Unit> = withContext(io) {
        val current = postDao.getPost(postId) ?: return@withContext Result.failure(NoSuchElementException(postId))
        if (current.isBookmarked == bookmarked) return@withContext Result.success(Unit)
        postDao.updateBookmark(postId, bookmarked, if (bookmarked) clock.now() else null)
        runCatchingNonCancellation {
            val response = if (bookmarked) api.bookmarkPost(postId) else api.removeBookmark(postId)
            check(response.success) { response.message ?: "Couldn't update saved posts" }
        }.onFailure { postDao.updateBookmark(postId, current.isBookmarked, current.bookmarkedAt) }
    }

    override suspend fun createPost(draft: PostDraft): Result<Post> = withContext(io) {
        runCatchingNonCancellation {
            val created = api.createPost(
                CreatePostRequest(
                    type = draft.type.name,
                    gradient = draft.gradientKey,
                    emoji = draft.emoji,
                    overlayText = draft.overlayText?.takeIf { it.isNotBlank() },
                    caption = draft.caption,
                    location = draft.location?.takeIf { it.isNotBlank() },
                ),
            )
            database.withTransaction {
                postDao.upsert(created.toEntity())
                database.feedRemoteKeyDao().insertAll(
                    listOf(FeedRemoteKeyEntity(created.id, FeedRemoteKeyEntity.LOCAL_PAGE, nextPage = null, fetchedAt = clock.now())),
                )
            }
            checkNotNull(postDao.getPostWithAuthor(created.id)?.toDomain()) { "Your profile isn't loaded yet" }
        }
    }

    /** Post rows need their authors; fetch the user directory once if the cache is cold. */
    private suspend fun ensureUsers() {
        if (userDao.count() == 0) userDao.insertIgnore(api.getUsers().map { it.toEntity() })
    }

    companion object {
        const val PAGE_SIZE = 10
    }
}
