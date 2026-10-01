package io.github.ieswar23.vibely.ui.comments

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ieswar23.vibely.data.repository.CommentRepository
import io.github.ieswar23.vibely.data.repository.PostRepository
import io.github.ieswar23.vibely.data.repository.UserRepository
import io.github.ieswar23.vibely.domain.model.Comment
import io.github.ieswar23.vibely.domain.model.Post
import io.github.ieswar23.vibely.domain.model.User
import io.github.ieswar23.vibely.ui.navigation.Routes
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CommentsUiState(
    val post: Post? = null,
    val comments: List<Comment> = emptyList(),
    val currentUser: User? = null,
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val input: String = "",
    val isSending: Boolean = false,
) {
    val canSend: Boolean get() = input.isNotBlank() && input.length <= MAX_COMMENT_LENGTH && !isSending

    companion object {
        const val MAX_COMMENT_LENGTH = 500
    }
}

sealed interface CommentsEvent {
    data object CommentPosted : CommentsEvent
    data class Message(val text: String) : CommentsEvent
}

@HiltViewModel
class CommentsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    postRepository: PostRepository,
    userRepository: UserRepository,
    private val commentRepository: CommentRepository,
) : ViewModel() {

    private val postId: String = checkNotNull(savedStateHandle[Routes.ARG_POST_ID])

    private val local = MutableStateFlow(LocalState())

    val uiState: StateFlow<CommentsUiState> = combine(
        postRepository.observePost(postId),
        commentRepository.comments(postId),
        userRepository.currentUser(),
        local,
    ) { post, comments, me, localState ->
        CommentsUiState(
            post = post,
            comments = comments,
            currentUser = me,
            isLoading = localState.loading && comments.isEmpty(),
            loadFailed = localState.failed && comments.isEmpty(),
            input = localState.input,
            isSending = localState.sending,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CommentsUiState())

    private val _events = Channel<CommentsEvent>(Channel.BUFFERED)
    val events: Flow<CommentsEvent> = _events.receiveAsFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            local.update { it.copy(loading = true, failed = false) }
            val result = commentRepository.refresh(postId)
            local.update { it.copy(loading = false, failed = result.isFailure) }
        }
    }

    fun onInputChange(text: String) {
        local.update { it.copy(input = text.take(CommentsUiState.MAX_COMMENT_LENGTH)) }
    }

    fun appendToInput(text: String) {
        local.update { it.copy(input = (it.input + text).take(CommentsUiState.MAX_COMMENT_LENGTH)) }
    }

    fun replyTo(comment: Comment) {
        local.update { it.copy(input = "@${comment.author.username} ") }
    }

    fun send() {
        val text = local.value.input.trim()
        if (text.isEmpty() || local.value.sending) return
        viewModelScope.launch {
            local.update { it.copy(sending = true) }
            commentRepository.addComment(postId, text)
                .onSuccess {
                    local.update { it.copy(input = "", sending = false) }
                    _events.send(CommentsEvent.CommentPosted)
                }
                .onFailure {
                    local.update { it.copy(sending = false) }
                    _events.send(CommentsEvent.Message("Couldn't post your comment"))
                }
        }
    }

    fun toggleCommentLike(comment: Comment) {
        viewModelScope.launch {
            commentRepository.setCommentLiked(comment.id, !comment.isLiked)
                .onFailure { _events.send(CommentsEvent.Message("Couldn't update like")) }
        }
    }

    private data class LocalState(
        val input: String = "",
        val loading: Boolean = true,
        val failed: Boolean = false,
        val sending: Boolean = false,
    )
}
