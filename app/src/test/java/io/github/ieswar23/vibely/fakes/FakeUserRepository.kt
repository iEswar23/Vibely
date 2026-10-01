package io.github.ieswar23.vibely.fakes

import io.github.ieswar23.vibely.data.repository.UserRepository
import io.github.ieswar23.vibely.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** In-memory [UserRepository]; follow updates are applied optimistically and rolled back on failure. */
class FakeUserRepository(initial: List<User>) : UserRepository {

    val users = MutableStateFlow(initial.associateBy { it.id })
    var failNextFollow = false
    val followCalls = mutableListOf<Pair<String, Boolean>>()

    override fun currentUser(): Flow<User?> = users.map { all -> all.values.firstOrNull { it.isCurrentUser } }

    override fun observeUser(userId: String): Flow<User?> = users.map { it[userId] }

    override fun search(query: String): Flow<List<User>> =
        users.map { all -> all.values.filter { !it.isCurrentUser && (it.username.contains(query) || it.name.contains(query, true)) } }

    override fun suggestedUsers(limit: Int): Flow<List<User>> =
        users.map { all -> all.values.filter { !it.isCurrentUser && !it.isFollowing }.take(limit) }

    override suspend fun findIdByUsername(username: String): String? =
        users.value.values.firstOrNull { it.username.equals(username, ignoreCase = true) }?.id

    override suspend fun isUsernameTaken(username: String, excludingUserId: String): Boolean =
        users.value.values.any { it.username == username && it.id != excludingUserId }

    override suspend fun refreshUsers(): Result<Unit> = Result.success(Unit)

    override suspend fun setFollowing(userId: String, follow: Boolean): Result<Unit> {
        followCalls += userId to follow
        val snapshot = users.value
        val target = snapshot[userId] ?: return Result.failure(NoSuchElementException(userId))
        if (target.isCurrentUser) return Result.failure(IllegalArgumentException("self"))
        val delta = if (follow) 1 else -1
        users.update { all ->
            all.mapValues { (id, user) ->
                when {
                    id == userId -> user.copy(isFollowing = follow, followerCount = user.followerCount + delta)
                    user.isCurrentUser -> user.copy(followingCount = user.followingCount + delta)
                    else -> user
                }
            }
        }
        if (failNextFollow) {
            failNextFollow = false
            users.value = snapshot
            return Result.failure(IllegalStateException("network"))
        }
        return Result.success(Unit)
    }

    override suspend fun updateProfile(name: String, username: String, bio: String, website: String?): Result<Unit> {
        users.update { all -> all.mapValues { (_, u) -> if (u.isCurrentUser) u.copy(name = name, username = username, bio = bio, website = website) else u } }
        return Result.success(Unit)
    }
}
