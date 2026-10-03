package io.github.ieswar23.vibely.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.ieswar23.vibely.data.local.ALL_MIGRATIONS
import io.github.ieswar23.vibely.data.local.VibelyDatabase
import io.github.ieswar23.vibely.data.local.dao.ActivityDao
import io.github.ieswar23.vibely.data.local.dao.CommentDao
import io.github.ieswar23.vibely.data.local.dao.FeedRemoteKeyDao
import io.github.ieswar23.vibely.data.local.dao.PostDao
import io.github.ieswar23.vibely.data.local.dao.StoryDao
import io.github.ieswar23.vibely.data.local.dao.UserDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): VibelyDatabase =
        Room.databaseBuilder(context, VibelyDatabase::class.java, VibelyDatabase.NAME)
            .addMigrations(*ALL_MIGRATIONS)
            // Everything is re-downloadable from the API, so a missing migration path just resets the cache.
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideUserDao(db: VibelyDatabase): UserDao = db.userDao()
    @Provides fun providePostDao(db: VibelyDatabase): PostDao = db.postDao()
    @Provides fun provideStoryDao(db: VibelyDatabase): StoryDao = db.storyDao()
    @Provides fun provideCommentDao(db: VibelyDatabase): CommentDao = db.commentDao()
    @Provides fun provideActivityDao(db: VibelyDatabase): ActivityDao = db.activityDao()
    @Provides fun provideFeedRemoteKeyDao(db: VibelyDatabase): FeedRemoteKeyDao = db.feedRemoteKeyDao()
}
