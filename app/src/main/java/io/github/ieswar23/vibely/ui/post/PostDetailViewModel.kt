package io.github.ieswar23.vibely.ui.post

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ieswar23.vibely.data.repository.PostRepository
import io.github.ieswar23.vibely.domain.model.Post
import io.github.ieswar23.vibely.ui.navigation.Routes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface PostDetailUiState {
    data object Loading : PostDetailUiState
    data object NotFound : PostDetailUiState
    data class Content(val post: Post, val moreFromAuthor: List<Post>) : PostDetailUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PostDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val postRepository: PostRepository,
) : ViewModel() {

    private val postId: String = checkNotNull(savedStateHandle[Routes.ARG_POST_ID])

    val uiState: StateFlow<PostDetailUiState> = postRepository.observePost(postId)
        .flatMapLatest { post ->
            if (post == null) {
                flowOf(PostDetailUiState.NotFound)
            } else {
                postRepository.postsByAuthor(post.author.id)
                    .map { list -> list.filter { it.id != post.id }.take(MORE_COUNT) }
                    .distinctUntilChanged()
                    .map { more -> PostDetailUiState.Content(post, more) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PostDetailUiState.Loading)

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = _messages.receiveAsFlow()

    fun onLikeClick(post: Post) = setLiked(post, !post.isLiked)

    fun onDoubleTapLike(post: Post) {
        if (!post.isLiked) setLiked(post, true)
    }

    fun onBookmarkClick(post: Post) {
        val save = !post.isBookmarked
        viewModelScope.launch {
            postRepository.setBookmarked(post.id, save)
                .onSuccess { _messages.send(if (save) "Saved to your collection" else "Removed from saved") }
                .onFailure { _messages.send("Couldn't update saved posts") }
        }
    }

    private fun setLiked(post: Post, liked: Boolean) {
        viewModelScope.launch {
            postRepository.setLiked(post.id, liked).onFailure { _messages.send("Couldn't update like") }
        }
    }

    private companion object {
        const val MORE_COUNT = 6
    }
}
