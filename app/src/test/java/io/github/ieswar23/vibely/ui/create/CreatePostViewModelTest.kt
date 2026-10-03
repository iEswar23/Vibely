package io.github.ieswar23.vibely.ui.create

import com.google.common.truth.Truth.assertThat
import io.github.ieswar23.vibely.MainDispatcherRule
import io.github.ieswar23.vibely.domain.model.PollDuration
import io.github.ieswar23.vibely.domain.model.PostType
import io.github.ieswar23.vibely.fakes.FakePostRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class CreatePostViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakePostRepository()
    private val viewModel = CreatePostViewModel(repository)

    @Test
    fun `blank text post is rejected and errors are shown`() {
        viewModel.onTypeChange(PostType.TEXT)
        viewModel.onCaptionChange("   ")

        viewModel.publish()

        val state = viewModel.state.value
        assertThat(state.showErrors).isTrue()
        assertThat(state.validation.captionError).isNotNull()
        assertThat(repository.createdDrafts).isEmpty()
    }

    @Test
    fun `canvas post can be published without a caption`() = runTest {
        viewModel.onGradientSelected("ocean")
        viewModel.onEmojiSelected("🏝️")
        viewModel.onHeadlineChange("  Island time  ")

        viewModel.publish()

        val draft = repository.createdDrafts.single()
        assertThat(draft.type).isEqualTo(PostType.CANVAS)
        assertThat(draft.gradientKey).isEqualTo("ocean")
        assertThat(draft.emoji).isEqualTo("🏝️")
        assertThat(draft.overlayText).isEqualTo("Island time")
        assertThat(viewModel.events.first()).isEqualTo(CreatePostEvent.Published)
        // Form resets after publishing.
        assertThat(viewModel.state.value.headline).isEmpty()
    }

    @Test
    fun `caption over the limit is rejected`() {
        viewModel.onCaptionChange("a".repeat(2_201))

        viewModel.publish()

        assertThat(viewModel.state.value.validation.captionError).contains("2,200")
        assertThat(repository.createdDrafts).isEmpty()
    }

    @Test
    fun `more than thirty hashtags is rejected`() {
        viewModel.onCaptionChange((1..31).joinToString(" ") { "#tag$it" })

        assertThat(viewModel.state.value.hashtagCount).isEqualTo(31)
        viewModel.publish()

        assertThat(viewModel.state.value.validation.captionError).contains("30 hashtags")
        assertThat(repository.createdDrafts).isEmpty()
    }

    @Test
    fun `text post drops canvas fields and trims location`() = runTest {
        viewModel.onTypeChange(PostType.TEXT)
        viewModel.onCaptionChange("Filter coffee tastes better in steel. #coffee")
        viewModel.onLocationChange("   ")

        viewModel.publish()

        val draft = repository.createdDrafts.single()
        assertThat(draft.gradientKey).isNull()
        assertThat(draft.emoji).isNull()
        assertThat(draft.location).isNull()
        assertThat(draft.caption).isEqualTo("Filter coffee tastes better in steel. #coffee")
    }

    @Test
    fun `publish failure keeps the draft and reports an error`() = runTest {
        repository.failNextWrite = true
        viewModel.onCaptionChange("Sunday bake #sourdough")

        viewModel.publish()

        val state = viewModel.state.value
        assertThat(state.isPublishing).isFalse()
        assertThat(state.caption).isEqualTo("Sunday bake #sourdough")
        assertThat(viewModel.events.first()).isInstanceOf(CreatePostEvent.Error::class.java)
    }

    @Test
    fun `poll is published with trimmed question, options and duration`() = runTest {
        viewModel.onTypeChange(PostType.POLL)
        viewModel.onPollQuestionChange("  What's the ultimate monsoon snack? ")
        viewModel.onPollOptionChange(0, " Pakoras with chai ")
        viewModel.onPollOptionChange(1, "Vada pav")
        viewModel.onAddPollOption()
        viewModel.onPollOptionChange(2, "Roasted bhutta")
        viewModel.onPollDurationChange(PollDuration.THREE_DAYS)
        viewModel.onCaptionChange("Settle this 👇")

        assertThat(viewModel.state.value.canPublish).isTrue()
        viewModel.publish()

        val draft = repository.createdDrafts.single()
        assertThat(draft.type).isEqualTo(PostType.POLL)
        assertThat(draft.gradientKey).isNull()
        assertThat(draft.emoji).isNull()
        assertThat(draft.caption).isEqualTo("Settle this 👇")
        val poll = requireNotNull(draft.poll)
        assertThat(poll.question).isEqualTo("What's the ultimate monsoon snack?")
        assertThat(poll.options).containsExactly("Pakoras with chai", "Vada pav", "Roasted bhutta").inOrder()
        assertThat(poll.duration).isEqualTo(PollDuration.THREE_DAYS)
        assertThat(viewModel.events.first()).isEqualTo(CreatePostEvent.Published)
    }

    @Test
    fun `invalid poll is not published and errors point at the fields`() {
        viewModel.onTypeChange(PostType.POLL)
        viewModel.onPollQuestionChange("Tabs or spaces?")
        viewModel.onPollOptionChange(0, "Tabs")
        viewModel.onPollOptionChange(1, "tabs")

        viewModel.publish()

        val state = viewModel.state.value
        assertThat(state.showErrors).isTrue()
        assertThat(state.validation.pollQuestionError).isNull()
        assertThat(state.validation.pollOptionErrors).containsExactly(null, "Options must be different").inOrder()
        assertThat(repository.createdDrafts).isEmpty()
    }

    @Test
    fun `poll options can be added up to four and removed down to two`() {
        viewModel.onTypeChange(PostType.POLL)
        assertThat(viewModel.state.value.pollOptions).hasSize(2)
        assertThat(viewModel.state.value.canRemovePollOption).isFalse()

        repeat(5) { viewModel.onAddPollOption() }
        assertThat(viewModel.state.value.pollOptions).hasSize(4)
        assertThat(viewModel.state.value.canAddPollOption).isFalse()

        viewModel.onPollOptionChange(1, "keep me")
        viewModel.onRemovePollOption(0)
        viewModel.onRemovePollOption(0)
        viewModel.onRemovePollOption(0)
        assertThat(viewModel.state.value.pollOptions).hasSize(2)
        assertThat(viewModel.state.value.pollOptions.first()).isEmpty()
        assertThat(viewModel.state.value.pollOptions).doesNotContain("keep me")
    }

    @Test
    fun `poll needs a question before it can be shared and other types ignore poll fields`() = runTest {
        viewModel.onTypeChange(PostType.POLL)
        assertThat(viewModel.state.value.canPublish).isFalse()

        viewModel.onPollQuestionChange("Half-filled poll")
        viewModel.onTypeChange(PostType.CANVAS)
        viewModel.publish()

        assertThat(repository.createdDrafts.single().poll).isNull()
    }
}
