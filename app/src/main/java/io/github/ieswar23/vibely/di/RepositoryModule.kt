package io.github.ieswar23.vibely.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.ieswar23.vibely.data.repository.ActivityRepository
import io.github.ieswar23.vibely.data.repository.ActivityRepositoryImpl
import io.github.ieswar23.vibely.data.repository.CommentRepository
import io.github.ieswar23.vibely.data.repository.CommentRepositoryImpl
import io.github.ieswar23.vibely.data.repository.DataStoreSettingsRepository
import io.github.ieswar23.vibely.data.repository.DefaultSyncRepository
import io.github.ieswar23.vibely.data.repository.PostRepository
import io.github.ieswar23.vibely.data.repository.PostRepositoryImpl
import io.github.ieswar23.vibely.data.repository.SettingsRepository
import io.github.ieswar23.vibely.data.repository.StoryRepository
import io.github.ieswar23.vibely.data.repository.StoryRepositoryImpl
import io.github.ieswar23.vibely.data.repository.SyncRepository
import io.github.ieswar23.vibely.data.repository.UserRepository
import io.github.ieswar23.vibely.data.repository.UserRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton abstract fun bindPostRepository(impl: PostRepositoryImpl): PostRepository
    @Binds @Singleton abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository
    @Binds @Singleton abstract fun bindStoryRepository(impl: StoryRepositoryImpl): StoryRepository
    @Binds @Singleton abstract fun bindCommentRepository(impl: CommentRepositoryImpl): CommentRepository
    @Binds @Singleton abstract fun bindActivityRepository(impl: ActivityRepositoryImpl): ActivityRepository
    @Binds @Singleton abstract fun bindSettingsRepository(impl: DataStoreSettingsRepository): SettingsRepository
    @Binds @Singleton abstract fun bindSyncRepository(impl: DefaultSyncRepository): SyncRepository
}
