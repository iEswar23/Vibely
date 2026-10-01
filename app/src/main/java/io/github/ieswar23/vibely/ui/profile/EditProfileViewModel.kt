package io.github.ieswar23.vibely.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ieswar23.vibely.data.repository.UserRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditProfileUiState(
    val userId: String = "",
    val name: String = "",
    val username: String = "",
    val bio: String = "",
    val website: String = "",
    val isLoaded: Boolean = false,
    val isSaving: Boolean = false,
    val nameError: String? = null,
    val usernameError: String? = null,
    val bioError: String? = null,
) {
    companion object {
        const val MAX_NAME = 30
        const val MAX_BIO = 150
    }
}

sealed interface EditProfileEvent {
    data object Saved : EditProfileEvent
    data class Message(val text: String) : EditProfileEvent
}

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(EditProfileUiState())
    val state: StateFlow<EditProfileUiState> = _state.asStateFlow()

    private val _events = Channel<EditProfileEvent>(Channel.BUFFERED)
    val events: Flow<EditProfileEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            val me = userRepository.currentUser().filterNotNull().first()
            _state.value = EditProfileUiState(
                userId = me.id,
                name = me.name,
                username = me.username,
                bio = me.bio,
                website = me.website.orEmpty(),
                isLoaded = true,
            )
        }
    }

    fun onNameChange(value: String) = _state.update { it.copy(name = value.take(EditProfileUiState.MAX_NAME), nameError = null) }
    fun onUsernameChange(value: String) = _state.update { it.copy(username = value.lowercase().replace(" ", ""), usernameError = null) }
    fun onBioChange(value: String) = _state.update { it.copy(bio = value.take(EditProfileUiState.MAX_BIO), bioError = null) }
    fun onWebsiteChange(value: String) = _state.update { it.copy(website = value.trim()) }

    fun save() {
        val current = _state.value
        if (!current.isLoaded || current.isSaving) return
        viewModelScope.launch {
            val nameError = if (current.name.isBlank()) "Name can't be empty" else null
            val usernameError = when {
                !USERNAME_REGEX.matches(current.username) -> "3–30 characters: letters, numbers, dots and underscores"
                userRepository.isUsernameTaken(current.username, current.userId) -> "That username is taken"
                else -> null
            }
            if (nameError != null || usernameError != null) {
                _state.update { it.copy(nameError = nameError, usernameError = usernameError) }
                return@launch
            }
            _state.update { it.copy(isSaving = true) }
            userRepository.updateProfile(current.name, current.username, current.bio, current.website)
                .onSuccess { _events.send(EditProfileEvent.Saved) }
                .onFailure {
                    _state.update { it.copy(isSaving = false) }
                    _events.send(EditProfileEvent.Message("Couldn't save your profile"))
                }
        }
    }

    companion object {
        val USERNAME_REGEX = Regex("^[a-z0-9._]{3,30}$")
    }
}
