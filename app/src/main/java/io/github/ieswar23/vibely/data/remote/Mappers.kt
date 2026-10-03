package io.github.ieswar23.vibely.data.remote

import io.github.ieswar23.vibely.data.local.entity.ActivityEntity
import io.github.ieswar23.vibely.data.local.entity.CommentEntity
import io.github.ieswar23.vibely.data.local.entity.PollEntity
import io.github.ieswar23.vibely.data.local.entity.PollOptionEntity
import io.github.ieswar23.vibely.data.local.entity.PostEntity
import io.github.ieswar23.vibely.data.local.entity.StoryEntity
import io.github.ieswar23.vibely.data.local.entity.StoryFrameEntity
import io.github.ieswar23.vibely.data.local.entity.UserEntity
import io.github.ieswar23.vibely.data.remote.dto.ActivityDto
import io.github.ieswar23.vibely.data.remote.dto.CommentDto
import io.github.ieswar23.vibely.data.remote.dto.PollDto
import io.github.ieswar23.vibely.data.remote.dto.PostDto
import io.github.ieswar23.vibely.data.remote.dto.StoryDto
import io.github.ieswar23.vibely.data.remote.dto.UserDto
import io.github.ieswar23.vibely.domain.model.PostType
import io.github.ieswar23.vibely.util.TextTokenParser

fun UserDto.toEntity() = UserEntity(
    id = id,
    name = name,
    username = username,
    bio = bio.orEmpty(),
    website = website,
    isVerified = verified ?: false,
    followerCount = followerCount ?: 0,
    followingCount = followingCount ?: 0,
    isFollowing = isFollowing ?: false,
    isCurrentUser = isCurrentUser ?: false,
)

fun PostDto.toEntity(): PostEntity {
    val text = caption.orEmpty()
    val pollEntity = if (type == PostType.POLL.name) poll?.toEntity() else null
    return PostEntity(
        id = id,
        authorId = authorId,
        type = when {
            pollEntity != null -> PostType.POLL.name
            // A poll without usable poll data still shows its caption as a text post.
            type == PostType.TEXT.name || type == PostType.POLL.name -> PostType.TEXT.name
            else -> PostType.CANVAS.name
        },
        gradientKey = gradient,
        emoji = emoji,
        overlayText = overlayText,
        caption = text,
        location = location?.takeIf { it.isNotBlank() },
        hashtags = encodeHashtags(TextTokenParser.hashtags(text)),
        likeCount = likeCount ?: 0,
        commentCount = commentCount ?: 0,
        isLiked = false,
        isBookmarked = false,
        bookmarkedAt = null,
        createdAt = createdAt,
        poll = pollEntity,
    )
}

/** Returns null for malformed polls (no question, fewer than two named options or no end time). */
fun PollDto.toEntity(): PollEntity? {
    val title = question?.trim().orEmpty()
    val choices = options.orEmpty()
        .filter { !it.text.isNullOrBlank() }
        .map { PollOptionEntity(text = it.text!!.trim(), votes = (it.votes ?: 0).coerceAtLeast(0)) }
    val end = endsAt
    if (title.isEmpty() || choices.size < 2 || end == null) return null
    return PollEntity(
        question = title,
        options = choices,
        endsAt = end,
        votedOption = votedOption?.takeIf { it in choices.indices },
    )
}

fun StoryDto.toEntity() = StoryEntity(
    id = id,
    userId = userId,
    frames = frames.orEmpty().map {
        StoryFrameEntity(emoji = it.emoji ?: "✨", gradient = it.gradient ?: "violet", text = it.text.orEmpty())
    },
    createdAt = createdAt,
    isSeen = false,
)

fun CommentDto.toEntity() = CommentEntity(
    id = id,
    postId = postId,
    authorId = authorId,
    text = text,
    likeCount = likeCount ?: 0,
    isLiked = false,
    createdAt = createdAt,
)

fun ActivityDto.toEntity() = ActivityEntity(
    id = id,
    type = type,
    actorId = actorId,
    postId = postId,
    text = text,
    createdAt = createdAt,
    isRead = false,
)

/** ",travel,food," – the surrounding commas let SQL `LIKE '%,tag,%'` match whole tags only. */
fun encodeHashtags(tags: List<String>): String = tags.joinToString(separator = ",", prefix = ",", postfix = ",")

fun decodeHashtags(column: String): List<String> = column.split(',').filter { it.isNotBlank() }
