package io.github.ieswar23.vibely.data.repository

import androidx.room.withTransaction
import io.github.ieswar23.vibely.data.local.VibelyDatabase
import io.github.ieswar23.vibely.data.local.dao.CommentDao
import io.github.ieswar23.vibely.data.local.dao.PostDao
import io.github.ieswar23.vibely.data.local.toDomain
import io.github.ieswar23.vibely.data.remote.VibelyApi
import io.github.ieswar23.vibely.data.remote.dto.CreateCommentRequest
import io.github.ieswar23.vibely.data.remote.toEntity
import io.github.ieswar23.vibely.di.IoDispatcher
import io.github.ieswar23.vibely.domain.model.Comment
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface CommentRepository {
    fun comments(postId: String): Flow<List<Comment>>
    suspend fun refresh(postId: String): Result<Unit>
    suspend fun addComment(postId: String, text: String): Result<Unit>
    suspend fun setCommentLiked(commentId: String, liked: Boolean): Result<Unit>
}

@Singleton
class CommentRepositoryImpl @Inject constructor(
    private val api: VibelyApi,
    private val database: VibelyDatabase,
    private val commentDao: CommentDao,
    private val postDao: PostDao,
    @IoDispatcher private val io: CoroutineDispatcher,
) : CommentRepository {

    override fun comments(postId: String): Flow<List<Comment>> =
        commentDao.observeForPost(postId).map { list -> list.mapNotNull { it.toDomain() } }

    override suspend fun refresh(postId: String): Result<Unit> = withContext(io) {
        runCatchingNonCancellation { commentDao.insertIgnore(api.getComments(postId).map { it.toEntity() }) }
    }

    override suspend fun addComment(postId: String, text: String): Result<Unit> = withContext(io) {
        runCatchingNonCancellation {
            val created = api.addComment(postId, CreateCommentRequest(text.trim()))
            database.withTransaction {
                commentDao.insert(created.toEntity())
                postDao.adjustCommentCount(postId, 1)
            }
        }
    }

    override suspend fun setCommentLiked(commentId: String, liked: Boolean): Result<Unit> = withContext(io) {
        val current = commentDao.getComment(commentId)
            ?: return@withContext Result.failure(NoSuchElementException(commentId))
        if (current.isLiked == liked) return@withContext Result.success(Unit)
        val delta = if (liked) 1 else -1
        commentDao.updateLike(commentId, liked, delta)
        runCatchingNonCancellation {
            val response = if (liked) api.likeComment(commentId) else api.unlikeComment(commentId)
            check(response.success) { response.message ?: "Couldn't update like" }
        }.onFailure { commentDao.updateLike(commentId, !liked, -delta) }
    }
}
