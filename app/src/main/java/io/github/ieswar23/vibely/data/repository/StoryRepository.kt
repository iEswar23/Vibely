package io.github.ieswar23.vibely.data.repository

import io.github.ieswar23.vibely.data.local.dao.StoryDao
import io.github.ieswar23.vibely.data.local.dao.UserDao
import io.github.ieswar23.vibely.data.local.toDomain
import io.github.ieswar23.vibely.data.remote.VibelyApi
import io.github.ieswar23.vibely.data.remote.toEntity
import io.github.ieswar23.vibely.di.IoDispatcher
import io.github.ieswar23.vibely.domain.model.Story
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface StoryRepository {
    fun stories(): Flow<List<Story>>
    suspend fun refresh(): Result<Unit>
    suspend fun markSeen(storyId: String)
}

@Singleton
class StoryRepositoryImpl @Inject constructor(
    private val api: VibelyApi,
    private val storyDao: StoryDao,
    private val userDao: UserDao,
    @IoDispatcher private val io: CoroutineDispatcher,
) : StoryRepository {

    override fun stories(): Flow<List<Story>> =
        storyDao.observeStories().map { list -> list.mapNotNull { it.toDomain() } }

    override suspend fun refresh(): Result<Unit> = withContext(io) {
        runCatchingNonCancellation {
            if (userDao.count() == 0) userDao.insertIgnore(api.getUsers().map { it.toEntity() })
            storyDao.insertIgnore(api.getStories().map { it.toEntity() })
        }
    }

    override suspend fun markSeen(storyId: String) = withContext(io) { storyDao.markSeen(storyId) }
}
