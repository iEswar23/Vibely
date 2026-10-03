package io.github.ieswar23.vibely.domain

import io.github.ieswar23.vibely.domain.model.Poll
import io.github.ieswar23.vibely.util.Clock
import io.github.ieswar23.vibely.util.CountFormatter
import io.github.ieswar23.vibely.util.TimeAgo

/** One option as the poll card renders it. */
data class PollOptionResult(
    val text: String,
    val votes: Int,
    /** Whole-number share of the vote. Across a poll these always add up to 100 (or all 0 with no votes). */
    val percent: Int,
    /** True for every option tied for the most votes; false for all options while nobody has voted. */
    val isLeading: Boolean,
    val isUserChoice: Boolean,
)

/** Everything the UI needs to draw a poll at a given moment. */
data class PollState(
    val options: List<PollOptionResult>,
    val totalVotes: Int,
    val hasVoted: Boolean,
    val isClosed: Boolean,
    /** Time until voting closes; zero once it has. */
    val remainingMillis: Long,
) {
    /** Results are revealed once you've voted or the poll has ended. */
    val showResults: Boolean get() = hasVoted || isClosed
    val canVote: Boolean get() = !hasVoted && !isClosed

    /** "1,284 votes · 5 days left" or "1,284 votes · Final results". */
    val summary: String get() = "${PollTally.votesLabel(totalVotes)}  •  ${PollTally.statusLabel(this)}"
}

/** Thrown when a vote is not allowed (already voted, poll closed or unknown option). */
class PollVoteException(message: String) : IllegalStateException(message)

/**
 * Vote, percentage and expiry rules for polls. Pure Kotlin with time injected through [Clock], so
 * every rule is unit tested without Android or real time.
 */
class PollTally(private val clock: Clock) {

    fun state(poll: Poll): PollState = state(poll, clock.now())

    fun isClosed(poll: Poll): Boolean = clock.now() >= poll.endsAt

    /**
     * Returns [poll] with the current user's vote counted. One vote per user: voting again, voting
     * on a closed poll or for an option that doesn't exist throws [PollVoteException].
     */
    fun castVote(poll: Poll, optionIndex: Int): Poll {
        if (poll.votedOptionIndex != null) throw PollVoteException("You've already voted in this poll")
        if (isClosed(poll)) throw PollVoteException("This poll has ended")
        if (optionIndex !in poll.options.indices) throw PollVoteException("That option doesn't exist")
        return poll.copy(
            options = poll.options.mapIndexed { index, option ->
                if (index == optionIndex) option.copy(voteCount = option.voteCount + 1) else option
            },
            votedOptionIndex = optionIndex,
        )
    }

    companion object {

        fun state(poll: Poll, now: Long): PollState {
            val votes = poll.options.map { it.voteCount.coerceAtLeast(0) }
            val percents = percentages(votes)
            val top = votes.maxOrNull() ?: 0
            return PollState(
                options = poll.options.mapIndexed { index, option ->
                    PollOptionResult(
                        text = option.text,
                        votes = votes[index],
                        percent = percents[index],
                        isLeading = top > 0 && votes[index] == top,
                        isUserChoice = poll.votedOptionIndex == index,
                    )
                },
                totalVotes = votes.sum(),
                hasVoted = poll.votedOptionIndex != null,
                isClosed = now >= poll.endsAt,
                remainingMillis = (poll.endsAt - now).coerceAtLeast(0),
            )
        }

        /**
         * Whole-number percentages that always sum to exactly 100 (largest remainder / Hamilton method):
         * every option gets the floor of its share, then the points still missing go to the options with
         * the largest remainders (ties: more votes first, then earlier option). No votes → all zeros.
         */
        fun percentages(votes: List<Int>): List<Int> {
            val total = votes.sumOf { it.toLong() }
            if (total <= 0L) return votes.map { 0 }
            val scaled = votes.map { it.toLong() * 100 }
            val floors = scaled.map { (it / total).toInt() }.toMutableList()
            val missing = 100 - floors.sum()
            votes.indices
                .sortedWith(
                    compareByDescending<Int> { scaled[it] % total }
                        .thenByDescending { votes[it] }
                        .thenBy { it },
                )
                .take(missing)
                .forEach { floors[it] += 1 }
            return floors
        }

        /** "Final results", "5 days left", "1 day left", "3 hours left", "12 minutes left". */
        fun statusLabel(state: PollState): String {
            if (state.isClosed) return "Final results"
            val remaining = state.remainingMillis
            return when {
                remaining >= TimeAgo.DAY -> plural(remaining / TimeAgo.DAY, "day") + " left"
                remaining >= TimeAgo.HOUR -> plural(remaining / TimeAgo.HOUR, "hour") + " left"
                // Round the last hour up so a poll never claims "0 minutes left" while still open.
                else -> plural(((remaining + TimeAgo.MINUTE - 1) / TimeAgo.MINUTE).coerceAtLeast(1), "minute") + " left"
            }
        }

        /** "No votes yet", "1 vote", "1,284 votes". */
        fun votesLabel(total: Int): String = when (total) {
            0 -> "No votes yet"
            1 -> "1 vote"
            else -> "${CountFormatter.grouped(total)} votes"
        }

        private fun plural(count: Long, unit: String) = if (count == 1L) "1 $unit" else "$count ${unit}s"
    }
}
