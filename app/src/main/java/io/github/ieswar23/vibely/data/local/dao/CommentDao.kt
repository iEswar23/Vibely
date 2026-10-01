package io.github.ieswar23.vibely.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import io.github.ieswar23.vibely.data.local.entity.CommentEntity
import io.github.ieswar23.vibely.data.local.entity.CommentWithAuthor
import kotlinx.coroutines.flow.Flow

@Dao
interface CommentDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(comments: List<CommentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(comment: CommentEntity)

    @Transaction
    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY createdAt ASC")
    fun observeForPost(postId: String): Flow<List<CommentWithAuthor>>

    @Query("SELECT * FROM comments WHERE id = :id")
    suspend fun getComment(id: String): CommentEntity?

    @Query("UPDATE comments SET isLiked = :liked, likeCount = MAX(0, likeCount + :delta) WHERE id = :id")
    suspend fun updateLike(id: String, liked: Boolean, delta: Int)

    @Query("DELETE FROM comments WHERE id = :id")
    suspend fun delete(id: String)
}
