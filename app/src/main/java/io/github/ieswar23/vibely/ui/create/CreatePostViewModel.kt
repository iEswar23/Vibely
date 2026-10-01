package io.github.ieswar23.vibely.ui.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ieswar23.vibely.data.repository.PostRepository
import io.github.ieswar23.vibely.domain.DraftValidation
import io.github.ieswar23.vibely.domain.PostDraftValidator
import io.github.ieswar23.vibely.domain.model.PostDraft
import io.github.ieswar23.vibely.domain.model.PostType
import io.github.ieswar23.vibely.util.TextTokenParser
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreatePostUiState(
    val type: PostType = PostType.CANVAS,
    val gradientKey: String = DEFAULT_GRADIENT,
    val emoji: String = DEFAULT_EMOJI,
    val headline: String = "",
    val caption: String = "",
    val location: String = "",
    val isPublishing: Boolean = false,
    /** Errors are only surfaced after the first publish attempt to avoid shouting at a blank form. */
    val showErrors: Boolean = false,
    val validation: DraftValidation = DraftValidation(),
) {
    val captionLength: Int get() = caption.trim().length
    val hashtagCount: Int get() = TextTokenParser.hashtags(caption).size
    val canPublish: Boolean get() = !isPublishing && (type == PostType.CANVAS || caption.isNotBlank())

    fun toDraft() = PostDraft(
        type = type,
        gradientKey = if (type == PostType.CANVAS) gradientKey else null,
        emoji = if (type == PostType.CANVAS) emoji else null,
        overlayText = if (type == PostType.CANVAS) headline.trim().ifEmpty { null } else null,
        caption = caption.trim(),
        location = location.trim().ifEmpty { null },
    )

    companion object {
        const val DEFAULT_GRADIENT = "violet"
        const val DEFAULT_EMOJI = "✨"
    }
}

sealed interface CreatePostEvent {
    data object Published : CreatePostEvent
    data class Error(val message: String) : CreatePostEvent
}

@HiltViewModel
class CreatePostViewModel @Inject constructor(
    private val postRepository: PostRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CreatePostUiState())
    val state: StateFlow<CreatePostUiState> = _state.asStateFlow()

    private val _events = Channel<CreatePostEvent>(Channel.BUFFERED)
    val events: Flow<CreatePostEvent> = _events.receiveAsFlow()

    fun onTypeChange(type: PostType) = edit { copy(type = type) }
    fun onGradientSelected(key: String) = edit { copy(gradientKey = key) }
    fun onEmojiSelected(emoji: String) = edit { copy(emoji = emoji) }
    fun onHeadlineChange(text: String) = edit { copy(headline = text) }
    fun onCaptionChange(text: String) = edit { copy(caption = text) }
    fun onLocationChange(text: String) = edit { copy(location = text) }

    fun publish() {
        val current = _state.value
        if (current.isPublishing) return
        val draft = current.toDraft()
        val validation = PostDraftValidator.validate(draft)
        if (!validation.isValid) {
            _state.update { it.copy(showErrors = true, validation = validation) }
            return
        }
        _state.update { it.copy(isPublishing = true, validation = validation) }
        viewModelScope.launch {
            postRepository.createPost(draft)
                .onSuccess {
                    _state.value = CreatePostUiState()
                    _events.send(CreatePostEvent.Published)
                }
                .onFailure {
                    _state.update { it.copy(isPublishing = false) }
                    _events.send(CreatePostEvent.Error("Couldn't publish your post. Please try again."))
                }
        }
    }

    private inline fun edit(block: CreatePostUiState.() -> CreatePostUiState) {
        _state.update { old ->
            val next = old.block()
            next.copy(validation = PostDraftValidator.validate(next.toDraft()))
        }
    }
}
