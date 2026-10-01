package io.github.ieswar23.vibely.ui.profile

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import io.github.ieswar23.vibely.MainDispatcherRule
import io.github.ieswar23.vibely.domain.model.UserSettings
import io.github.ieswar23.vibely.fakes.FakePostRepository
import io.github.ieswar23.vibely.fakes.FakeSettingsRepository
import io.github.ieswar23.vibely.fakes.FakeUserRepository
import io.github.ieswar23.vibely.fakes.TestData
import io.github.ieswar23.vibely.ui.navigation.Routes
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class ProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val users = FakeUserRepository(listOf(TestData.me, TestData.priya, TestData.rohan))
    private val posts = FakePostRepository(
        listOf(
            TestData.post(id = "p1", author = TestData.rohan),
            TestData.post(id = "p2", author = TestData.rohan),
            TestData.post(id = "p3", author = TestData.me, isBookmarked = false),
            TestData.post(id = "p4", author = TestData.priya, isBookmarked = true),
        ),
    )
    private val settings = FakeSettingsRepository(UserSettings(privateAccount = true))

    private fun viewModel(userKey: String) =
        ProfileViewModel(SavedStateHandle(mapOf(Routes.ARG_USER_KEY to userKey)), users, posts, settings)

    /** Keeps the WhileSubscribed state flow active for the duration of the test. */
    private fun TestScope.observe(vm: ProfileViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
    }

    private fun ProfileViewModel.content() = uiState.value as ProfileUiState.Content

    @Test
    fun `other user's profile shows their posts and follow state`() = runTest {
        val vm = viewModel(TestData.rohan.id)
        observe(vm)

        val content = vm.content()
        assertThat(content.isMe).isFalse()
        assertThat(content.user.isFollowing).isFalse()
        assertThat(content.posts.map { it.id }).containsExactly("p1", "p2")
        assertThat(content.saved).isEmpty()
        assertThat(posts.refreshedUserPosts).contains(TestData.rohan.id)
    }

    @Test
    fun `follow updates follower count and my following count`() = runTest {
        val vm = viewModel(TestData.rohan.id)
        observe(vm)

        vm.toggleFollow()

        val content = vm.content()
        assertThat(content.user.isFollowing).isTrue()
        assertThat(content.user.followerCount).isEqualTo(TestData.rohan.followerCount + 1)
        assertThat(users.users.value.getValue(TestData.me.id).followingCount).isEqualTo(TestData.me.followingCount + 1)
    }

    @Test
    fun `unfollow decrements follower count`() = runTest {
        val vm = viewModel(TestData.priya.id)
        observe(vm)

        vm.toggleFollow()

        val content = vm.content()
        assertThat(content.user.isFollowing).isFalse()
        assertThat(content.user.followerCount).isEqualTo(TestData.priya.followerCount - 1)
        assertThat(users.followCalls).containsExactly(TestData.priya.id to false)
    }

    @Test
    fun `failed follow rolls back and shows a message`() = runTest {
        val vm = viewModel(TestData.rohan.id)
        observe(vm)
        users.failNextFollow = true

        vm.toggleFollow()

        val content = vm.content()
        assertThat(content.user.isFollowing).isFalse()
        assertThat(content.user.followerCount).isEqualTo(TestData.rohan.followerCount)
        assertThat(vm.events.first()).isInstanceOf(ProfileEvent.Message::class.java)
    }

    @Test
    fun `me key resolves to the current user with saved posts and privacy`() = runTest {
        val vm = viewModel(Routes.MY_PROFILE_SAVED_KEY)
        observe(vm)

        val content = vm.content()
        assertThat(content.isMe).isTrue()
        assertThat(content.isPrivate).isTrue()
        assertThat(content.saved.map { it.id }).containsExactly("p4")
        assertThat(vm.initialTab).isEqualTo(ProfileTab.SAVED)

        vm.toggleFollow()
        assertThat(users.followCalls).isEmpty()
    }

    @Test
    fun `mention key resolves by username and unknown handles are not found`() = runTest {
        val vm = viewModel("@priya.wanders")
        observe(vm)
        assertThat(vm.content().user.id).isEqualTo(TestData.priya.id)

        val missing = viewModel("@nobody.here")
        observe(missing)
        assertThat(missing.uiState.value).isEqualTo(ProfileUiState.NotFound)
    }
}
