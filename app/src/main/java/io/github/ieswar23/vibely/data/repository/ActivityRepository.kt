package io.github.ieswar23.vibely.data.repository

import io.github.ieswar23.vibely.data.local.dao.ActivityDao
import io.github.ieswar23.vibely.data.local.dao.PostDao
import io.github.ieswar23.vibely.data.local.dao.UserDao
import io.github.ieswar23.vibely.data.local.toDomain
import io.github.ieswar23.vibely.data.remote.VibelyApi
import io.github.ieswar23.vibely.data.remote.toEntity
import io.github.ieswar23.vibely.di.IoDispatcher
import io.github.ieswar23.vibely.domain.model.ActivityItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface ActivityRepository {
    fun activity(): Flow<List<ActivityItem>>
    fun unreadCount(): Flow<Int>
    suspend fun refresh(): Result<Unit>
    suspend fun markAllRead()
}

@Singleton
class ActivityRepositoryImpl @Inject constructor(
    private val api: VibelyApi,
    private val activityDao: ActivityDao,
    private val userDao: UserDao,
    private val postDao: PostDao,
    @IoDispatcher private val io: CoroutineDispatcher,
) : ActivityRepository {

    override fun activity(): Flow<List<ActivityItem>> =
        activityDao.observeAll().map { list -> list.mapNotNull { it.toDomain() } }

    override fun unreadCount(): Flow<Int> = activityDao.observeUnreadCount()

    override suspend fun refresh(): Result<Unit> = withContext(io) {
        runCatchingNonCancellation {
            if (userDao.count() == 0) userDao.insertIgnore(api.getUsers().map { it.toEntity() })
            // Activity rows reference the current user's posts for thumbnails.
            userDao.getCurrentUser()?.let { me -> postDao.insertIgnore(api.getUserPosts(me.id).map { it.toEntity() }) }
            activityDao.insertIgnore(api.getActivity().map { it.toEntity() })
        }
    }

    override suspend fun markAllRead() = withContext(io) { activityDao.markAllRead() }
}
