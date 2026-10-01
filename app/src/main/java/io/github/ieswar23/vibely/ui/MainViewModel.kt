package io.github.ieswar23.vibely.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ieswar23.vibely.data.repository.ActivityRepository
import io.github.ieswar23.vibely.data.repository.SettingsRepository
import io.github.ieswar23.vibely.data.repository.UserRepository
import io.github.ieswar23.vibely.domain.model.ThemeMode
import io.github.ieswar23.vibely.domain.model.User
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class MainUiState(
    val isReady: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val unreadActivity: Int = 0,
    val currentUser: User? = null,
)

/** App-shell state: theme for the splash/theme switch, the activity badge and the profile tab avatar. */
@HiltViewModel
class MainViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    activityRepository: ActivityRepository,
    userRepository: UserRepository,
) : ViewModel() {

    val uiState: StateFlow<MainUiState> = combine(
        settingsRepository.settings,
        activityRepository.unreadCount(),
        userRepository.currentUser(),
    ) { settings, unread, me ->
        MainUiState(isReady = true, themeMode = settings.themeMode, unreadActivity = unread, currentUser = me)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainUiState())
}
