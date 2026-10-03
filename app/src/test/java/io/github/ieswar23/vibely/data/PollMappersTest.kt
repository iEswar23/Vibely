package io.github.ieswar23.vibely.data

import com.google.common.truth.Truth.assertThat
import io.github.ieswar23.vibely.data.local.entity.PostWithAuthor
import io.github.ieswar23.vibely.data.local.entity.UserEntity
import io.github.ieswar23.vibely.data.local.toDomain
import io.github.ieswar23.vibely.data.remote.dto.PollDto
import io.github.ieswar23.vibely.data.remote.dto.PollOptionDto
import io.github.ieswar23.vibely.data.remote.dto.PostDto
import io.github.ieswar23.vibely.data.remote.toEntity
import io.github.ieswar23.vibely.domain.model.PostType
import org.junit.Test

class PollMappersTest {

    private val author = UserEntity("u21", "Nisha Patel", "nisha.plates", "", null, false, 10, 10, false, false)

    private fun dto(type: String, poll: PollDto?) = PostDto(
        id = "p121",
        authorId = "u21",
        type = type,
        gradient = null,
        emoji = null,
        overlayText = null,
        caption = "Settle this 👇",
        location = "Mumbai, India",
        likeCount = 243,
        commentCount = 3,
        createdAt = 1_790_856_000_000L,
        poll = poll,
    )

    private val snacks = PollDto(
        question = " What's the ultimate monsoon snack? ",
        options = listOf(PollOptionDto("Pakoras with chai", 412), PollOptionDto("Vada pav", null), PollOptionDto(" ", 5)),
        endsAt = 1_791_000_000_000L,
        votedOption = 7,
    )

    @Test
    fun `poll posts map through entity to domain`() {
        val entity = dto("POLL", snacks).toEntity()
        val post = requireNotNull(PostWithAuthor(entity, author).toDomain())

        assertThat(post.type).isEqualTo(PostType.POLL)
        val poll = requireNotNull(post.poll)
        assertThat(poll.question).isEqualTo("What's the ultimate monsoon snack?")
        // Blank options are dropped, missing counts become 0 and an out-of-range vote is ignored.
        assertThat(poll.options.map { it.text to it.voteCount }).containsExactly("Pakoras with chai" to 412, "Vada pav" to 0).inOrder()
        assertThat(poll.votedOptionIndex).isNull()
        assertThat(poll.endsAt).isEqualTo(1_791_000_000_000L)
    }

    @Test
    fun `malformed poll falls back to a text post`() {
        val entity = dto("POLL", snacks.copy(options = listOf(PollOptionDto("Only one", 1)))).toEntity()

        assertThat(entity.type).isEqualTo(PostType.TEXT.name)
        assertThat(entity.poll).isNull()
    }

    @Test
    fun `poll data on a non-poll post is ignored`() {
        val entity = dto("CANVAS", snacks).toEntity()

        assertThat(entity.type).isEqualTo(PostType.CANVAS.name)
        assertThat(entity.poll).isNull()
        assertThat(PostWithAuthor(entity, author).toDomain()?.poll).isNull()
    }
}
