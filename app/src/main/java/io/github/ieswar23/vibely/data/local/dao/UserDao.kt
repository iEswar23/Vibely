package io.github.ieswar23.vibely.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.ieswar23.vibely.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(users: List<UserEntity>)

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUser(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id")
    fun observeUser(id: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE username = :username COLLATE NOCASE LIMIT 1")
    suspend fun getByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE isCurrentUser = 1 LIMIT 1")
    fun observeCurrentUser(): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE isCurrentUser = 1 LIMIT 1")
    suspend fun getCurrentUser(): UserEntity?

    @Query(
        """
        SELECT * FROM users
        WHERE isCurrentUser = 0 AND (name LIKE '%' || :query || '%' OR username LIKE '%' || :query || '%')
        ORDER BY CASE WHEN username LIKE :query || '%' THEN 0 ELSE 1 END, followerCount DESC
        LIMIT 30
        """,
    )
    fun search(query: String): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE isCurrentUser = 0 AND isFollowing = 0 ORDER BY followerCount DESC LIMIT :limit")
    fun observeSuggested(limit: Int): Flow<List<UserEntity>>

    @Query("SELECT COUNT(*) FROM users")
    suspend fun count(): Int

    @Query("UPDATE users SET isFollowing = :following, followerCount = MAX(0, followerCount + :delta) WHERE id = :id")
    suspend fun updateFollowing(id: String, following: Boolean, delta: Int)

    @Query("UPDATE users SET followingCount = MAX(0, followingCount + :delta) WHERE isCurrentUser = 1")
    suspend fun adjustCurrentUserFollowing(delta: Int)

    @Query("UPDATE users SET name = :name, username = :username, bio = :bio, website = :website WHERE id = :id")
    suspend fun updateProfile(id: String, name: String, username: String, bio: String, website: String?)
}
