package io.github.ieswar23.vibely.data.remote

import io.github.ieswar23.vibely.data.local.entity.ActivityEntity
import io.github.ieswar23.vibely.data.local.entity.CommentEntity
import io.github.ieswar23.vibely.data.local.entity.PostEntity
import io.github.ieswar23.vibely.data.local.entity.StoryEntity
import io.github.ieswar23.vibely.data.local.entity.StoryFrameEntity
import io.github.ieswar23.vibely.data.local.entity.UserEntity
import io.github.ieswar23.vibely.data.remote.dto.ActivityDto
import io.github.ieswar23.vibely.data.remote.dto.CommentDto
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
    return PostEntity(
        id = id,
        authorId = authorId,
        type = if (type == PostType.TEXT.name) PostType.TEXT.name else PostType.CANVAS.name,
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
