package io.github.ieswar23.vibely.data.remote.dto

/*
 * Gson DTOs mirroring the mock backend's JSON. Fields that the backend may omit are nullable so
 * malformed payloads surface as mapping defaults instead of NullPointerExceptions.
 */

data class UserDto(
    val id: String,
    val name: String,
    val username: String,
    val bio: String?,
    val website: String?,
    val verified: Boolean?,
    val followerCount: Int?,
    val followingCount: Int?,
    val isFollowing: Boolean?,
    val isCurrentUser: Boolean?,
)

data class PostDto(
    val id: String,
    val authorId: String,
    val type: String?,
    val gradient: String?,
    val emoji: String?,
    val overlayText: String?,
    val caption: String?,
    val location: String?,
    val likeCount: Int?,
    val commentCount: Int?,
    val createdAt: Long,
    /** Only present on `"type": "POLL"` posts. */
    val poll: PollDto? = null,
)

data class PollDto(
    val question: String?,
    val options: List<PollOptionDto>?,
    val endsAt: Long?,
    /** Index of the option the requesting user voted for, if any. */
    val votedOption: Int?,
)

data class PollOptionDto(
    val text: String?,
    val votes: Int?,
)

data class FeedPageDto(
    val page: Int,
    val limit: Int,
    val total: Int,
    val hasMore: Boolean,
    val posts: List<PostDto>,
    /** Authors of [posts], side-loaded so the client can render a page without extra requests. */
    val authors: List<UserDto>,
)

data class StoryFrameDto(
    val emoji: String?,
    val gradient: String?,
    val text: String?,
)

data class StoryDto(
    val id: String,
    val userId: String,
    val createdAt: Long,
    val frames: List<StoryFrameDto>?,
)

data class CommentDto(
    val id: String,
    val postId: String,
    val authorId: String,
    val text: String,
    val likeCount: Int?,
    val createdAt: Long,
)

data class ActivityDto(
    val id: String,
    val type: String,
    val actorId: String,
    val postId: String?,
    val text: String?,
    val createdAt: Long,
)

data class CreatePostRequest(
    val type: String,
    val gradient: String?,
    val emoji: String?,
    val overlayText: String?,
    val caption: String,
    val location: String?,
    val poll: CreatePollRequest? = null,
)

data class CreatePollRequest(
    val question: String,
    val options: List<String>,
    val durationDays: Int,
)

data class PollVoteRequest(val optionIndex: Int)

data class CreateCommentRequest(val text: String)

data class ActionResponseDto(val success: Boolean, val message: String?)
