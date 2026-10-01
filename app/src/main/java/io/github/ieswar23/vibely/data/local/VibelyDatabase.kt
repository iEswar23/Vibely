package io.github.ieswar23.vibely.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.github.ieswar23.vibely.data.local.dao.ActivityDao
import io.github.ieswar23.vibely.data.local.dao.CommentDao
import io.github.ieswar23.vibely.data.local.dao.FeedRemoteKeyDao
import io.github.ieswar23.vibely.data.local.dao.PostDao
import io.github.ieswar23.vibely.data.local.dao.StoryDao
import io.github.ieswar23.vibely.data.local.dao.UserDao
import io.github.ieswar23.vibely.data.local.entity.ActivityEntity
import io.github.ieswar23.vibely.data.local.entity.CommentEntity
import io.github.ieswar23.vibely.data.local.entity.FeedRemoteKeyEntity
import io.github.ieswar23.vibely.data.local.entity.PostEntity
import io.github.ieswar23.vibely.data.local.entity.StoryEntity
import io.github.ieswar23.vibely.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        PostEntity::class,
        StoryEntity::class,
        CommentEntity::class,
        ActivityEntity::class,
        FeedRemoteKeyEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class VibelyDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun postDao(): PostDao
    abstract fun storyDao(): StoryDao
    abstract fun commentDao(): CommentDao
    abstract fun activityDao(): ActivityDao
    abstract fun feedRemoteKeyDao(): FeedRemoteKeyDao

    companion object {
        const val NAME = "vibely.db"
    }
}
