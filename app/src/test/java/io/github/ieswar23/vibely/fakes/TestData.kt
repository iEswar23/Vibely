package io.github.ieswar23.vibely.fakes

import io.github.ieswar23.vibely.domain.model.Poll
import io.github.ieswar23.vibely.domain.model.PollOption
import io.github.ieswar23.vibely.domain.model.Post
import io.github.ieswar23.vibely.domain.model.PostType
import io.github.ieswar23.vibely.domain.model.User

object TestData {
    /** Fixed "now" for anything time-dependent (poll expiry). */
    const val NOW = 1_790_856_000_000L
    const val DAY = 24 * 60 * 60_000L

    val me = user(id = "u_me", username = "aarav.kapoor", name = "Aarav Kapoor", isCurrentUser = true, followers = 1284, following = 412)
    val priya = user(id = "u01", username = "priya.wanders", name = "Priya Sharma", followers = 48200, following = 611, isFollowing = true)
    val rohan = user(id = "u02", username = "rohan.lifts", name = "Rohan Mehta", followers = 15400, following = 380)

    fun user(
        id: String,
        username: String,
        name: String,
        followers: Int = 100,
        following: Int = 50,
        isFollowing: Boolean = false,
        isCurrentUser: Boolean = false,
    ) = User(
        id = id,
        name = name,
        username = username,
        bio = "",
        website = null,
        isVerified = false,
        followerCount = followers,
        followingCount = following,
        isFollowing = isFollowing,
        isCurrentUser = isCurrentUser,
    )

    fun post(
        id: String,
        author: User = priya,
        likes: Int = 10,
        isLiked: Boolean = false,
        isBookmarked: Boolean = false,
        caption: String = "Sunrise over the hills #travel",
    ) = Post(
        id = id,
        author = author,
        type = PostType.CANVAS,
        gradientKey = "sunset",
        emoji = "🌅",
        overlayText = null,
        caption = caption,
        location = null,
        hashtags = listOf("travel"),
        likeCount = likes,
        commentCount = 0,
        isLiked = isLiked,
        isBookmarked = isBookmarked,
        createdAt = 1_700_000_000_000L,
    )

    fun poll(
        votes: List<Int> = listOf(412, 356, 189, 97),
        endsAt: Long = NOW + 3 * DAY,
        votedOptionIndex: Int? = null,
        question: String = "What's the ultimate monsoon snack?",
    ) = Poll(
        question = question,
        options = votes.mapIndexed { index, count -> PollOption(text = SNACKS.getOrElse(index) { "Option ${index + 1}" }, voteCount = count) },
        endsAt = endsAt,
        votedOptionIndex = votedOptionIndex,
    )

    fun pollPost(id: String, poll: Poll = poll(), author: User = priya) =
        post(id = id, author = author, caption = "Settle this for tomorrow's reel").copy(
            type = PostType.POLL,
            gradientKey = null,
            emoji = null,
            hashtags = emptyList(),
            poll = poll,
        )

    private val SNACKS = listOf("Pakoras with chai", "Vada pav", "Roasted bhutta", "Maggi at a hill stop")
}
