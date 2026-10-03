package io.github.ieswar23.vibely.ui.feed

import com.google.common.truth.Truth.assertThat
import io.github.ieswar23.vibely.MainDispatcherRule
import io.github.ieswar23.vibely.fakes.FakePostRepository
import io.github.ieswar23.vibely.fakes.FakeStoryRepository
import io.github.ieswar23.vibely.fakes.FakeUserRepository
import io.github.ieswar23.vibely.fakes.TestData
import kotlinx.coroutines.CompletableDeferred
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
    private val openPoll = TestData.pollPost(id = "poll1")
    private val closedPoll = TestData.pollPost(id = "poll2", poll = TestData.poll(endsAt = TestData.NOW - 1))

    private lateinit var posts: FakePostRepository
    private lateinit var stories: FakeStoryRepository
    private lateinit var viewModel: FeedViewModel

    @Before
    fun setUp() {
        posts = FakePostRepository(listOf(unliked, liked, openPoll, closedPoll))
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

    @Test
    fun `vote shows instantly while the request is in flight and sticks on success`() = runTest {
        val response = CompletableDeferred<Unit>()
        posts.pendingVoteResponse = response

        viewModel.onVote(openPoll, optionIndex = 0)

        // Optimistic: the tally already includes the vote before the "network" answers.
        val pending = posts.posts.value.getValue("poll1").poll!!
        assertThat(pending.votedOptionIndex).isEqualTo(0)
        assertThat(pending.options[0].voteCount).isEqualTo(413)

        response.complete(Unit)

        assertThat(posts.voteCalls).containsExactly("poll1" to 0)
        assertThat(posts.posts.value.getValue("poll1").poll!!.votedOptionIndex).isEqualTo(0)
    }

    @Test
    fun `failed vote is rolled back and reported`() = runTest {
        val response = CompletableDeferred<Unit>()
        posts.pendingVoteResponse = response
        posts.failNextWrite = true

        viewModel.onVote(openPoll, optionIndex = 2)
        assertThat(posts.posts.value.getValue("poll1").poll!!.options[2].voteCount).isEqualTo(190)

        response.complete(Unit)

        assertThat(posts.posts.value.getValue("poll1")).isEqualTo(openPoll)
        assertThat(viewModel.events.first()).isEqualTo(FeedEvent.Message("Couldn't save your vote. Please try again."))
    }

    @Test
    fun `only one vote per user`() = runTest {
        viewModel.onVote(openPoll, optionIndex = 1)
        val afterFirstVote = posts.posts.value.getValue("poll1")

        // Taps on the now-voted card, and stale snapshots of the old card, never count a second vote.
        viewModel.onVote(afterFirstVote, optionIndex = 2)
        viewModel.onVote(openPoll, optionIndex = 3)

        assertThat(posts.voteCalls).containsExactly("poll1" to 1, "poll1" to 3).inOrder()
        val poll = posts.posts.value.getValue("poll1").poll!!
        assertThat(poll.votedOptionIndex).isEqualTo(1)
        assertThat(poll.options.map { it.voteCount }).containsExactly(412, 357, 189, 97).inOrder()
        assertThat(viewModel.events.first()).isEqualTo(FeedEvent.Message("You've already voted in this poll"))
    }

    @Test
    fun `voting on a closed poll explains why`() = runTest {
        viewModel.onVote(closedPoll, optionIndex = 0)

        assertThat(posts.posts.value.getValue("poll2")).isEqualTo(closedPoll)
        assertThat(viewModel.events.first()).isEqualTo(FeedEvent.Message("This poll has ended"))
    }
}
