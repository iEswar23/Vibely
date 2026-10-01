package io.github.ieswar23.vibely.ui.create

import com.google.common.truth.Truth.assertThat
import io.github.ieswar23.vibely.MainDispatcherRule
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
}
