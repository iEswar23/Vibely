package io.github.ieswar23.vibely.ui.feed

import com.google.common.truth.Truth.assertThat
import io.github.ieswar23.vibely.MainDispatcherRule
import io.github.ieswar23.vibely.fakes.FakePostRepository
import io.github.ieswar23.vibely.fakes.FakeStoryRepository
import io.github.ieswar23.vibely.fakes.FakeUserRepository
import io.github.ieswar23.vibely.fakes.TestData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class FeedViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val unliked = TestData.post(id = "p1", likes = 10, isLiked = false)
    private val liked = TestData.post(id = "p2", likes = 25, isLiked = true)

    private lateinit var posts: FakePostRepository
    private lateinit var stories: FakeStoryRepository
    private lateinit var viewModel: FeedViewModel

    @Before
    fun setUp() {
        posts = FakePostRepository(listOf(unliked, liked))
        stories = FakeStoryRepository()
        viewModel = FeedViewModel(posts, stories, FakeUserRepository(listOf(TestData.me, TestData.priya)))
    }

    @Test
    fun `stories are refreshed when the feed opens`() {
        assertThat(stories.refreshCount).isEqualTo(1)
    }

    @Test
    fun `like click toggles an unliked post to liked`() = runTest {
        viewModel.onLikeClick(unliked)

        assertThat(posts.likeCalls).containsExactly("p1" to true)
        val updated = posts.posts.value.getValue("p1")
        assertThat(updated.isLiked).isTrue()
        assertThat(updated.likeCount).isEqualTo(11)
    }

    @Test
    fun `like click on a liked post removes the like`() = runTest {
        viewModel.onLikeClick(liked)

        assertThat(posts.likeCalls).containsExactly("p2" to false)
        assertThat(posts.posts.value.getValue("p2").likeCount).isEqualTo(24)
    }

    @Test
    fun `double tap never unlikes`() = runTest {
        viewModel.onDoubleTapLike(liked)
        viewModel.onDoubleTapLike(unliked)

        assertThat(posts.likeCalls).containsExactly("p1" to true)
    }

    @Test
    fun `failed like is rolled back and reported`() = runTest {
        posts.failNextWrite = true

        viewModel.onLikeClick(unliked)

        assertThat(posts.posts.value.getValue("p1")).isEqualTo(unliked)
        val event = viewModel.events.first()
        assertThat(event).isInstanceOf(FeedEvent.Message::class.java)
    }

    @Test
    fun `bookmark emits an undoable event and undo restores state`() = runTest {
        viewModel.onBookmarkClick(unliked)

        val event = viewModel.events.first()
        assertThat(event).isEqualTo(FeedEvent.BookmarkChanged(postId = "p1", saved = true))
        assertThat(posts.posts.value.getValue("p1").isBookmarked).isTrue()

        viewModel.undoBookmark("p1", wasSaved = true)

        assertThat(posts.bookmarkCalls).containsExactly("p1" to true, "p1" to false).inOrder()
        assertThat(posts.posts.value.getValue("p1").isBookmarked).isFalse()
    }
}
