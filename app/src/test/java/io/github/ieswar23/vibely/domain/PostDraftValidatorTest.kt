package io.github.ieswar23.vibely.domain

import com.google.common.truth.Truth.assertThat
import io.github.ieswar23.vibely.domain.model.PollDraft
import io.github.ieswar23.vibely.domain.model.PollDuration
import io.github.ieswar23.vibely.domain.model.PostDraft
import io.github.ieswar23.vibely.domain.model.PostType
import org.junit.Test

class PostDraftValidatorTest {

    private fun canvas(caption: String = "", headline: String? = null, location: String? = null) =
        PostDraft(PostType.CANVAS, "sunset", "🌅", headline, caption, location)

    private fun poll(question: String = "Tabs or spaces?", vararg options: String = arrayOf("Tabs", "Spaces")) =
        PostDraft(PostType.POLL, null, null, null, caption = "", location = null, poll = PollDraft(question, options.toList(), PollDuration.THREE_DAYS))

    @Test
    fun `canvas post without caption is valid`() {
        assertThat(PostDraftValidator.validate(canvas()).isValid).isTrue()
    }

    @Test
    fun `canvas post needs a background and emoji`() {
        val result = PostDraftValidator.validate(canvas().copy(emoji = null))
        assertThat(result.mediaError).isNotNull()
    }

    @Test
    fun `long headline and location are rejected`() {
        val result = PostDraftValidator.validate(canvas(headline = "h".repeat(41), location = "l".repeat(61)))
        assertThat(result.headlineError).isNotNull()
        assertThat(result.locationError).isNotNull()
    }

    @Test
    fun `poll with a question and two distinct options is valid without a caption`() {
        val result = PostDraftValidator.validate(poll())

        assertThat(result.isValid).isTrue()
        assertThat(result.pollOptionErrors).containsExactly(null, null)
    }

    @Test
    fun `poll needs a question`() {
        val result = PostDraftValidator.validate(poll(question = "   "))

        assertThat(result.isValid).isFalse()
        assertThat(result.pollQuestionError).isEqualTo("Ask a question")
    }

    @Test
    fun `blank and too long options are flagged individually`() {
        val result = PostDraftValidator.validate(poll("Best snack?", "Vada pav", " ", "x".repeat(41)))

        assertThat(result.pollOptionErrors).containsExactly(null, "Add an option", "Up to 40 characters").inOrder()
        assertThat(result.isValid).isFalse()
    }

    @Test
    fun `duplicate options are flagged on the later copy, ignoring case and spaces`() {
        val result = PostDraftValidator.validate(poll("Best snack?", "Vada pav", "Poha", " vada PAV "))

        assertThat(result.pollOptionErrors).containsExactly(null, null, "Options must be different").inOrder()
    }

    @Test
    fun `poll option count must be between two and four`() {
        val tooFew = PostDraftValidator.validate(poll("Best snack?", "Vada pav"))
        val tooMany = PostDraftValidator.validate(poll("Best snack?", "A", "B", "C", "D", "E"))

        assertThat(tooFew.pollOptionErrors).containsExactly(null, "Add an option").inOrder()
        assertThat(tooMany.pollOptionErrors.last()).isEqualTo("Polls can have up to 4 options")
        assertThat(tooMany.isValid).isFalse()
    }

    @Test
    fun `long poll question is rejected and poll type without poll data is invalid`() {
        assertThat(PostDraftValidator.validate(poll(question = "q".repeat(141))).pollQuestionError).contains("140")

        val missing = PostDraftValidator.validate(poll().copy(poll = null))
        assertThat(missing.isValid).isFalse()
        assertThat(missing.pollOptionErrors).hasSize(2)
    }

    @Test
    fun `poll fields are ignored for other post types`() {
        val result = PostDraftValidator.validate(canvas().copy(poll = PollDraft("", emptyList(), PollDuration.ONE_DAY)))

        assertThat(result.isValid).isTrue()
        assertThat(result.pollOptionErrors).isEmpty()
    }
}
