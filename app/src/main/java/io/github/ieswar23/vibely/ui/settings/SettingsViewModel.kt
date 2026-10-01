package io.github.ieswar23.vibely.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ieswar23.vibely.data.repository.SettingsRepository
import io.github.ieswar23.vibely.data.repository.SyncRepository
import io.github.ieswar23.vibely.domain.model.ThemeMode
import io.github.ieswar23.vibely.domain.model.UserSettings
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SettingsEvent {
    data object ResetComplete : SettingsEvent
    data class Message(val text: String) : SettingsEvent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val syncRepository: SyncRepository,
) : ViewModel() {

    val settings: StateFlow<UserSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettings())

    private val _isResetting = MutableStateFlow(false)
    val isResetting: StateFlow<Boolean> = _isResetting.asStateFlow()

    private val _events = Channel<SettingsEvent>(Channel.BUFFERED)
    val events: Flow<SettingsEvent> = _events.receiveAsFlow()

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setPrivateAccount(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setPrivateAccount(enabled) }
    }

    /** Logout-style reset: wipe local data and preferences, then reload the starter content. */
    fun resetAppData() {
        if (_isResetting.value) return
        viewModelScope.launch {
            _isResetting.value = true
            syncRepository.resetAll()
                .onSuccess { _events.send(SettingsEvent.ResetComplete) }
                .onFailure { _events.send(SettingsEvent.Message("Reset finished, but some content couldn't be reloaded")) }
            _isResetting.value = false
        }
    }
}
