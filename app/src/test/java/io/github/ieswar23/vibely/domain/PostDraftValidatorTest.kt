package io.github.ieswar23.vibely.domain

import com.google.common.truth.Truth.assertThat
import io.github.ieswar23.vibely.domain.model.PostDraft
import io.github.ieswar23.vibely.domain.model.PostType
import org.junit.Test

class PostDraftValidatorTest {

    private fun canvas(caption: String = "", headline: String? = null, location: String? = null) =
        PostDraft(PostType.CANVAS, "sunset", "🌅", headline, caption, location)

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
}
