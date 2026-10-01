package io.github.ieswar23.vibely.fakes

import io.github.ieswar23.vibely.data.repository.SettingsRepository
import io.github.ieswar23.vibely.data.repository.StoryRepository
import io.github.ieswar23.vibely.domain.model.Story
import io.github.ieswar23.vibely.domain.model.ThemeMode
import io.github.ieswar23.vibely.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeStoryRepository(initial: List<Story> = emptyList()) : StoryRepository {
    val stories = MutableStateFlow(initial)
    var refreshCount = 0

    override fun stories(): Flow<List<Story>> = stories

    override suspend fun refresh(): Result<Unit> {
        refreshCount++
        return Result.success(Unit)
    }

    override suspend fun markSeen(storyId: String) {
        stories.update { list -> list.map { if (it.id == storyId) it.copy(isSeen = true) else it } }
    }
}

class FakeSettingsRepository(initial: UserSettings = UserSettings()) : SettingsRepository {
    val state = MutableStateFlow(initial)
    override val settings: Flow<UserSettings> = state
    override suspend fun setThemeMode(mode: ThemeMode) = state.update { it.copy(themeMode = mode) }
    override suspend fun setPrivateAccount(enabled: Boolean) = state.update { it.copy(privateAccount = enabled) }
    override suspend fun clear() {
        state.value = UserSettings()
    }
}
