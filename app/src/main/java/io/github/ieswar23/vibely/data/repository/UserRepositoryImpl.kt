package io.github.ieswar23.vibely.data.repository

import io.github.ieswar23.vibely.data.local.dao.UserDao
import io.github.ieswar23.vibely.data.local.toDomain
import io.github.ieswar23.vibely.data.remote.VibelyApi
import io.github.ieswar23.vibely.data.remote.toEntity
import io.github.ieswar23.vibely.di.IoDispatcher
import io.github.ieswar23.vibely.domain.model.User
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val api: VibelyApi,
    private val userDao: UserDao,
    @IoDispatcher private val io: CoroutineDispatcher,
) : UserRepository {

    override fun currentUser(): Flow<User?> = userDao.observeCurrentUser().map { it?.toDomain() }

    override fun observeUser(userId: String): Flow<User?> = userDao.observeUser(userId).map { it?.toDomain() }

    override fun search(query: String): Flow<List<User>> {
        val trimmed = query.trim().removePrefix("@")
        if (trimmed.isEmpty()) return flowOf(emptyList())
        return userDao.search(trimmed).map { list -> list.map { it.toDomain() } }
    }

    override fun suggestedUsers(limit: Int): Flow<List<User>> =
        userDao.observeSuggested(limit).map { list -> list.map { it.toDomain() } }

    override suspend fun findIdByUsername(username: String): String? = withContext(io) {
        userDao.getByUsername(username.removePrefix("@"))?.id
    }

    override suspend fun isUsernameTaken(username: String, excludingUserId: String): Boolean = withContext(io) {
        val existing = userDao.getByUsername(username)
        existing != null && existing.id != excludingUserId
    }

    override suspend fun refreshUsers(): Result<Unit> = withContext(io) {
        runCatchingNonCancellation { userDao.insertIgnore(api.getUsers().map { it.toEntity() }) }
    }

    override suspend fun setFollowing(userId: String, follow: Boolean): Result<Unit> = withContext(io) {
        val user = userDao.getUser(userId) ?: return@withContext Result.failure(NoSuchElementException(userId))
        if (user.isCurrentUser) return@withContext Result.failure(IllegalArgumentException("You can't follow yourself"))
        if (user.isFollowing == follow) return@withContext Result.success(Unit)
        val delta = if (follow) 1 else -1
        userDao.updateFollowing(userId, follow, delta)
        userDao.adjustCurrentUserFollowing(delta)
        runCatchingNonCancellation {
            val response = if (follow) api.follow(userId) else api.unfollow(userId)
            check(response.success) { response.message ?: "Couldn't update follow" }
        }.onFailure {
            userDao.updateFollowing(userId, !follow, -delta)
            userDao.adjustCurrentUserFollowing(-delta)
        }
    }

    override suspend fun updateProfile(name: String, username: String, bio: String, website: String?): Result<Unit> =
        withContext(io) {
            runCatchingNonCancellation {
                val me = checkNotNull(userDao.getCurrentUser()) { "No signed-in user" }
                userDao.updateProfile(me.id, name.trim(), username.trim(), bio.trim(), website?.trim()?.takeIf { it.isNotEmpty() })
            }
        }
}
