package io.github.ieswar23.vibely.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import io.github.ieswar23.vibely.data.local.entity.ActivityEntity
import io.github.ieswar23.vibely.data.local.entity.ActivityWithRelations
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(items: List<ActivityEntity>)

    @Transaction
    @Query("SELECT * FROM activity ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ActivityWithRelations>>

    @Query("SELECT COUNT(*) FROM activity WHERE isRead = 0")
    fun observeUnreadCount(): Flow<Int>

    @Query("UPDATE activity SET isRead = 1 WHERE isRead = 0")
    suspend fun markAllRead()
}
