package io.github.ieswar23.vibely.ui.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ieswar23.vibely.data.repository.PostRepository
import io.github.ieswar23.vibely.data.repository.SettingsRepository
import io.github.ieswar23.vibely.data.repository.UserRepository
import io.github.ieswar23.vibely.domain.model.Post
import io.github.ieswar23.vibely.domain.model.User
import io.github.ieswar23.vibely.ui.navigation.Routes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ProfileTab { POSTS, SAVED }

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data object NotFound : ProfileUiState
    data class Content(
        val user: User,
        val posts: List<Post>,
        val saved: List<Post>,
        val isMe: Boolean,
        val isPrivate: Boolean,
        val isLoadingPosts: Boolean,
    ) : ProfileUiState
}

sealed interface ProfileEvent {
    data class Message(val text: String) : ProfileEvent
}

/**
 * Backs both the signed-in user's profile and other people's profiles. The nav argument may be a
 * user id, "@username" (from a mention) or one of the "me" keys.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val userRepository: UserRepository,
    private val postRepository: PostRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val userKey: String = savedStateHandle.get<String>(Routes.ARG_USER_KEY) ?: Routes.MY_PROFILE_KEY
    private val isMeKey = userKey == Routes.MY_PROFILE_KEY || userKey == Routes.MY_PROFILE_SAVED_KEY

    val initialTab: ProfileTab = if (userKey == Routes.MY_PROFILE_SAVED_KEY) ProfileTab.SAVED else ProfileTab.POSTS

    private val postsLoading = MutableStateFlow(true)

    /** Resolves the nav key to a user id; emits null when no such user exists. */
    private val userId: Flow<String?> = when {
        isMeKey -> userRepository.currentUser().filterNotNull().map { it.id }.distinctUntilChanged()
        userKey.startsWith("@") -> flow { emit(userRepository.findIdByUsername(userKey.removePrefix("@"))) }
        else -> flowOf(userKey)
    }

    val uiState: StateFlow<ProfileUiState> = userId.flatMapLatest { id ->
        if (id == null) {
            flowOf(ProfileUiState.NotFound)
        } else {
            combine(
                userRepository.observeUser(id),
                postRepository.postsByAuthor(id),
                postRepository.bookmarkedPosts(),
                settingsRepository.settings,
                postsLoading,
            ) { user, posts, saved, settings, loading ->
                if (user == null) {
                    ProfileUiState.Loading
                } else {
                    ProfileUiState.Content(
                        user = user,
                        posts = posts,
                        saved = if (user.isCurrentUser) saved else emptyList(),
                        isMe = user.isCurrentUser,
                        isPrivate = user.isCurrentUser && settings.privateAccount,
                        isLoadingPosts = loading && posts.isEmpty(),
                    )
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState.Loading)

    private val _events = Channel<ProfileEvent>(Channel.BUFFERED)
    val events: Flow<ProfileEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            val id = userId.first() ?: return@launch
            postsLoading.value = true
            postRepository.refreshUserPosts(id)
            postsLoading.value = false
        }
    }

    /** Follow/unfollow with an optimistic update; the repository rolls back if the request fails. */
    fun toggleFollow() {
        val content = uiState.value as? ProfileUiState.Content ?: return
        if (content.isMe) return
        val follow = !content.user.isFollowing
        viewModelScope.launch {
            userRepository.setFollowing(content.user.id, follow).onFailure {
                _events.send(
                    ProfileEvent.Message(
                        if (follow) "Couldn't follow ${content.user.username}. Try again." else "Couldn't unfollow ${content.user.username}. Try again.",
                    ),
                )
            }
        }
    }
}
