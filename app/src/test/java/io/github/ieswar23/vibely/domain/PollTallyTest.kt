package io.github.ieswar23.vibely.domain

import com.google.common.truth.Truth.assertThat
import io.github.ieswar23.vibely.fakes.TestData
import io.github.ieswar23.vibely.fakes.TestData.DAY
import io.github.ieswar23.vibely.fakes.TestData.NOW
import io.github.ieswar23.vibely.util.Clock
import org.junit.Assert.assertThrows
import org.junit.Test
import kotlin.random.Random

class PollTallyTest {

    private var now = NOW
    private val tally = PollTally(Clock { now })

    @Test
    fun `percentages use largest remainder rounding and sum to 100`() {
        // 39.15 / 33.74 / 17.91 / 9.19 → floors 39/33/17/9 (98); the two missing points go to .91 and .74.
        assertThat(PollTally.percentages(listOf(413, 356, 189, 97))).containsExactly(39, 34, 18, 9).inOrder()
        assertThat(PollTally.percentages(listOf(2, 1))).containsExactly(67, 33).inOrder()
        assertThat(PollTally.percentages(listOf(5, 0, 0))).containsExactly(100, 0, 0).inOrder()
    }

    @Test
    fun `equal remainders go to the earlier option`() {
        assertThat(PollTally.percentages(listOf(1, 1, 1))).containsExactly(34, 33, 33).inOrder()
        assertThat(PollTally.percentages(listOf(1, 1, 1, 1, 1, 1, 1))).containsExactly(15, 15, 14, 14, 14, 14, 14).inOrder()
    }

    @Test
    fun `percentages always add up to exactly 100`() {
        val random = Random(seed = 42)
        repeat(500) {
            val votes = List(random.nextInt(2, 5)) { random.nextInt(0, 5_000) }
            if (votes.sum() == 0) return@repeat
            val percents = PollTally.percentages(votes)
            assertThat(percents.sum()).isEqualTo(100)
            // Rounding never moves an option more than one point away from its exact share.
            percents.forEachIndexed { i, p -> assertThat(Math.abs(p - votes[i] * 100.0 / votes.sum())).isLessThan(1.0) }
        }
    }

    @Test
    fun `zero votes gives zero percent, no leader and a friendly label`() {
        val state = tally.state(TestData.poll(votes = listOf(0, 0, 0)))

        assertThat(state.options.map { it.percent }).containsExactly(0, 0, 0)
        assertThat(state.options.none { it.isLeading }).isTrue()
        assertThat(state.totalVotes).isEqualTo(0)
        assertThat(state.summary).isEqualTo("No votes yet  •  3 days left")
    }

    @Test
    fun `options tied for the most votes are all leading`() {
        val state = tally.state(TestData.poll(votes = listOf(40, 40, 20)))

        assertThat(state.options.map { it.isLeading }).containsExactly(true, true, false).inOrder()
    }

    @Test
    fun `casting a vote counts it once and marks the user's choice`() {
        val voted = tally.castVote(TestData.poll(), optionIndex = 0)
        val state = tally.state(voted)

        assertThat(voted.votedOptionIndex).isEqualTo(0)
        assertThat(state.totalVotes).isEqualTo(1_055)
        assertThat(state.options.map { it.percent }).containsExactly(39, 34, 18, 9).inOrder()
        assertThat(state.options[0].isUserChoice).isTrue()
        assertThat(state.hasVoted).isTrue()
        assertThat(state.showResults).isTrue()
        assertThat(state.canVote).isFalse()
    }

    @Test
    fun `one vote per user`() {
        val voted = tally.castVote(TestData.poll(), optionIndex = 1)

        assertThrows(PollVoteException::class.java) { tally.castVote(voted, optionIndex = 2) }
    }

    @Test
    fun `closed polls and unknown options reject votes`() {
        val poll = TestData.poll(endsAt = NOW)

        assertThrows(PollVoteException::class.java) { tally.castVote(poll, optionIndex = 0) }
        assertThrows(PollVoteException::class.java) { tally.castVote(TestData.poll(), optionIndex = 4) }
        assertThrows(PollVoteException::class.java) { tally.castVote(TestData.poll(), optionIndex = -1) }
    }

    @Test
    fun `expiry follows the injected clock`() {
        val poll = TestData.poll(endsAt = NOW + DAY)

        assertThat(tally.state(poll).isClosed).isFalse()
        assertThat(tally.state(poll).showResults).isFalse()

        now = NOW + DAY // closes exactly at endsAt
        val closed = tally.state(poll)
        assertThat(tally.isClosed(poll)).isTrue()
        assertThat(closed.isClosed).isTrue()
        assertThat(closed.showResults).isTrue()
        assertThat(closed.remainingMillis).isEqualTo(0)
        assertThat(PollTally.statusLabel(closed)).isEqualTo("Final results")
    }

    @Test
    fun `time left label counts down from days to minutes`() {
        fun label(remaining: Long) = PollTally.statusLabel(PollTally.state(TestData.poll(endsAt = NOW + remaining), NOW))

        assertThat(label(5 * DAY + 8 * HOUR)).isEqualTo("5 days left")
        assertThat(label(DAY + 23 * HOUR)).isEqualTo("1 day left")
        assertThat(label(3 * HOUR + 59 * MINUTE)).isEqualTo("3 hours left")
        assertThat(label(HOUR)).isEqualTo("1 hour left")
        assertThat(label(12 * MINUTE)).isEqualTo("12 minutes left")
        assertThat(label(5_000)).isEqualTo("1 minute left")
    }

    @Test
    fun `summary combines grouped vote count and status`() {
        val state = PollTally.state(TestData.poll(endsAt = NOW - 1), NOW)

        assertThat(state.summary).isEqualTo("1,054 votes  •  Final results")
        assertThat(PollTally.votesLabel(1)).isEqualTo("1 vote")
    }

    private companion object {
        const val MINUTE = 60_000L
        const val HOUR = 60 * MINUTE
    }
}
