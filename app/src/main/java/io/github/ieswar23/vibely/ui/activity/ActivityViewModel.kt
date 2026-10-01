package io.github.ieswar23.vibely.ui.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ieswar23.vibely.data.repository.ActivityRepository
import io.github.ieswar23.vibely.data.repository.UserRepository
import io.github.ieswar23.vibely.domain.model.ActivityItem
import io.github.ieswar23.vibely.domain.model.User
import io.github.ieswar23.vibely.util.Clock
import io.github.ieswar23.vibely.util.TimeAgo
import io.github.ieswar23.vibely.util.TimeBucket
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ActivityUiState(
    val sections: List<Pair<TimeBucket, List<ActivityItem>>> = emptyList(),
    val suggestions: List<User> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
)

@HiltViewModel
class ActivityViewModel @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val userRepository: UserRepository,
    private val clock: Clock,
) : ViewModel() {

    private val refreshing = MutableStateFlow(true)

    val uiState: StateFlow<ActivityUiState> = combine(
        activityRepository.activity(),
        userRepository.suggestedUsers(limit = 6),
        userRepository.currentUser(),
        refreshing,
    ) { items, suggested, me, isRefreshing ->
        val now = clock.now()
        val sections = items
            .groupBy { TimeAgo.bucket(it.createdAt, now) }
            .toSortedMap(compareBy { it.ordinal })
            .map { (bucket, list) -> bucket to list }
        ActivityUiState(
            sections = sections,
            suggestions = suggested.filter { it.id != me?.id },
            isLoading = isRefreshing && items.isEmpty(),
            isRefreshing = isRefreshing && items.isNotEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ActivityUiState())

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = _messages.receiveAsFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            refreshing.value = true
            activityRepository.refresh().onFailure { _messages.send("Couldn't refresh activity") }
            refreshing.value = false
        }
    }

    fun markAllRead() {
        viewModelScope.launch { activityRepository.markAllRead() }
    }

    fun toggleFollow(user: User) {
        viewModelScope.launch {
            userRepository.setFollowing(user.id, !user.isFollowing)
                .onFailure { _messages.send("Couldn't update follow for ${user.username}") }
        }
    }
}
