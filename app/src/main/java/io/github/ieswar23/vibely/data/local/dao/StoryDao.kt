package io.github.ieswar23.vibely.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import io.github.ieswar23.vibely.data.local.entity.StoryEntity
import io.github.ieswar23.vibely.data.local.entity.StoryWithUser
import kotlinx.coroutines.flow.Flow

@Dao
interface StoryDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(stories: List<StoryEntity>)

    /** Unseen stories first (newest first), followed by already watched ones. */
    @Transaction
    @Query("SELECT * FROM stories ORDER BY isSeen ASC, createdAt DESC")
    fun observeStories(): Flow<List<StoryWithUser>>

    @Query("UPDATE stories SET isSeen = 1 WHERE id = :id")
    suspend fun markSeen(id: String)
}
