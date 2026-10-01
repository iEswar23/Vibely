package io.github.ieswar23.vibely.ui.story

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ieswar23.vibely.data.repository.StoryRepository
import io.github.ieswar23.vibely.domain.model.Story
import io.github.ieswar23.vibely.domain.model.StoryFrame
import io.github.ieswar23.vibely.ui.navigation.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StoryViewerState(
    val stories: List<Story> = emptyList(),
    val storyIndex: Int = 0,
    val frameIndex: Int = 0,
    val isLoading: Boolean = true,
    val isFinished: Boolean = false,
    /** Bumped when the same frame must restart its progress (e.g. "previous" on the first frame). */
    val restartToken: Int = 0,
) {
    val currentStory: Story? get() = stories.getOrNull(storyIndex)
    val currentFrame: StoryFrame? get() = currentStory?.frames?.getOrNull(frameIndex)
}

/**
 * Drives the full-screen story viewer. The ordered list is snapshotted on open so marking stories
 * as seen (which reorders the stories row) does not shuffle the viewer mid-session.
 */
@HiltViewModel
class StoryViewerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val storyRepository: StoryRepository,
) : ViewModel() {

    private val startUserId: String = checkNotNull(savedStateHandle[Routes.ARG_USER_ID])

    private val _state = MutableStateFlow(StoryViewerState())
    val state: StateFlow<StoryViewerState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val stories = storyRepository.stories().first().filter { it.frames.isNotEmpty() }
            val start = stories.indexOfFirst { it.user.id == startUserId }.coerceAtLeast(0)
            _state.value = StoryViewerState(
                stories = stories,
                storyIndex = start,
                isLoading = false,
                isFinished = stories.isEmpty(),
            )
            markCurrentSeen()
        }
    }

    fun next() {
        val current = _state.value
        val story = current.currentStory ?: return
        when {
            current.frameIndex < story.frames.lastIndex -> _state.update { it.copy(frameIndex = it.frameIndex + 1) }
            current.storyIndex < current.stories.lastIndex -> {
                _state.update { it.copy(storyIndex = it.storyIndex + 1, frameIndex = 0) }
                markCurrentSeen()
            }
            else -> _state.update { it.copy(isFinished = true) }
        }
    }

    fun previous() {
        val current = _state.value
        when {
            current.frameIndex > 0 -> _state.update { it.copy(frameIndex = it.frameIndex - 1) }
            current.storyIndex > 0 -> _state.update { it.copy(storyIndex = it.storyIndex - 1, frameIndex = 0) }
            else -> _state.update { it.copy(restartToken = it.restartToken + 1) }
        }
    }

    private fun markCurrentSeen() {
        val story = _state.value.currentStory ?: return
        if (!story.isSeen) viewModelScope.launch { storyRepository.markSeen(story.id) }
    }
}
