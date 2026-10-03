package io.github.ieswar23.vibely.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ieswar23.vibely.data.repository.PostRepository
import io.github.ieswar23.vibely.data.repository.StoryRepository
import io.github.ieswar23.vibely.data.repository.UserRepository
import io.github.ieswar23.vibely.domain.PollVoteException
import io.github.ieswar23.vibely.domain.model.Post
import io.github.ieswar23.vibely.domain.model.Story
import io.github.ieswar23.vibely.domain.model.User
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

data class StoriesUiState(
    val currentUser: User? = null,
    val stories: List<Story> = emptyList(),
    val isLoading: Boolean = true,
)

sealed interface FeedEvent {
    data class Message(val text: String) : FeedEvent

    /** Shown as a snackbar with an "Undo" action. */
    data class BookmarkChanged(val postId: String, val saved: Boolean) : FeedEvent
}

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val storyRepository: StoryRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    val feed: Flow<PagingData<Post>> = postRepository.feed().cachedIn(viewModelScope)

    private val storiesLoading = MutableStateFlow(true)

    val storiesState: StateFlow<StoriesUiState> = combine(
        userRepository.currentUser(),
        storyRepository.stories(),
        storiesLoading,
    ) { me, stories, loading ->
        StoriesUiState(currentUser = me, stories = stories, isLoading = loading && stories.isEmpty())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StoriesUiState())

    private val _events = Channel<FeedEvent>(Channel.BUFFERED)
    val events: Flow<FeedEvent> = _events.receiveAsFlow()

    init {
        refreshStories()
    }

    /** Called on first load and on pull-to-refresh (the feed itself refreshes via Paging). */
    fun refreshStories() {
        viewModelScope.launch {
            storiesLoading.value = true
            userRepository.refreshUsers()
            storyRepository.refresh().onFailure { _events.send(FeedEvent.Message("Couldn't load stories")) }
            storiesLoading.value = false
        }
    }

    fun onLikeClick(post: Post) = setLiked(post, !post.isLiked)

    /** Double tap only ever likes – it never removes an existing like. */
    fun onDoubleTapLike(post: Post) {
        if (!post.isLiked) setLiked(post, true)
    }

    private fun setLiked(post: Post, liked: Boolean) {
        viewModelScope.launch {
            postRepository.setLiked(post.id, liked)
                .onFailure { _events.send(FeedEvent.Message("Couldn't update like. Please try again.")) }
        }
    }

    /** One vote per user: taps on a poll you've already voted in are ignored. */
    fun onVote(post: Post, optionIndex: Int) {
        if (post.poll == null || post.poll.votedOptionIndex != null) return
        viewModelScope.launch {
            postRepository.vote(post.id, optionIndex)
                .onFailure { error ->
                    val message = (error as? PollVoteException)?.message ?: "Couldn't save your vote. Please try again."
                    _events.send(FeedEvent.Message(message))
                }
        }
    }

    fun onBookmarkClick(post: Post) {
        val save = !post.isBookmarked
        viewModelScope.launch {
            postRepository.setBookmarked(post.id, save)
                .onSuccess { _events.send(FeedEvent.BookmarkChanged(post.id, save)) }
                .onFailure { _events.send(FeedEvent.Message("Couldn't update saved posts")) }
        }
    }

    fun undoBookmark(postId: String, wasSaved: Boolean) {
        viewModelScope.launch { postRepository.setBookmarked(postId, !wasSaved) }
    }
}
