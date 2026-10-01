package io.github.ieswar23.vibely.data.repository

import io.github.ieswar23.vibely.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun currentUser(): Flow<User?>
    fun observeUser(userId: String): Flow<User?>
    fun search(query: String): Flow<List<User>>
    fun suggestedUsers(limit: Int = 8): Flow<List<User>>

    /** Resolves "@handle" style mentions to a user id. */
    suspend fun findIdByUsername(username: String): String?
    suspend fun isUsernameTaken(username: String, excludingUserId: String): Boolean

    suspend fun refreshUsers(): Result<Unit>

    /** Optimistic follow/unfollow; follower counts are rolled back if the API call fails. */
    suspend fun setFollowing(userId: String, follow: Boolean): Result<Unit>

    suspend fun updateProfile(name: String, username: String, bio: String, website: String?): Result<Unit>
}
