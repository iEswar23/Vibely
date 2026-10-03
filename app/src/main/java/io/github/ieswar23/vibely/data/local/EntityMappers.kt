package io.github.ieswar23.vibely.data.local

import io.github.ieswar23.vibely.data.local.entity.ActivityWithRelations
import io.github.ieswar23.vibely.data.local.entity.CommentWithAuthor
import io.github.ieswar23.vibely.data.local.entity.PollEntity
import io.github.ieswar23.vibely.data.local.entity.PollOptionEntity
import io.github.ieswar23.vibely.data.local.entity.PostEntity
import io.github.ieswar23.vibely.data.local.entity.PostWithAuthor
import io.github.ieswar23.vibely.data.local.entity.StoryWithUser
import io.github.ieswar23.vibely.data.local.entity.UserEntity
import io.github.ieswar23.vibely.data.remote.decodeHashtags
import io.github.ieswar23.vibely.domain.model.ActivityItem
import io.github.ieswar23.vibely.domain.model.ActivityType
import io.github.ieswar23.vibely.domain.model.Comment
import io.github.ieswar23.vibely.domain.model.Poll
import io.github.ieswar23.vibely.domain.model.PollOption
import io.github.ieswar23.vibely.domain.model.Post
import io.github.ieswar23.vibely.domain.model.PostPreview
import io.github.ieswar23.vibely.domain.model.PostType
import io.github.ieswar23.vibely.domain.model.Story
import io.github.ieswar23.vibely.domain.model.StoryFrame
import io.github.ieswar23.vibely.domain.model.User

fun UserEntity.toDomain() = User(
    id = id,
    name = name,
    username = username,
    bio = bio,
    website = website,
    isVerified = isVerified,
    followerCount = followerCount,
    followingCount = followingCount,
    isFollowing = isFollowing,
    isCurrentUser = isCurrentUser,
)

private fun PostEntity.postType(): PostType {
    val parsed = runCatching { PostType.valueOf(type) }.getOrDefault(PostType.CANVAS)
    // Never hand the UI a poll post without a poll; fall back to showing its caption.
    return if (parsed == PostType.POLL && poll == null) PostType.TEXT else parsed
}

/** Returns null when the author has not been cached yet; such rows are skipped by the UI. */
fun PostWithAuthor.toDomain(): Post? {
    val user = author ?: return null
    return Post(
        id = post.id,
        author = user.toDomain(),
        type = post.postType(),
        gradientKey = post.gradientKey,
        emoji = post.emoji,
        overlayText = post.overlayText,
        caption = post.caption,
        location = post.location,
        hashtags = decodeHashtags(post.hashtags),
        likeCount = post.likeCount,
        commentCount = post.commentCount,
        isLiked = post.isLiked,
        isBookmarked = post.isBookmarked,
        createdAt = post.createdAt,
        poll = post.poll?.toDomain(),
    )
}

fun PollEntity.toDomain() = Poll(
    question = question,
    options = options.map { PollOption(text = it.text, voteCount = it.votes) },
    endsAt = endsAt,
    votedOptionIndex = votedOption,
)

fun Poll.toEntity() = PollEntity(
    question = question,
    options = options.map { PollOptionEntity(text = it.text, votes = it.voteCount) },
    endsAt = endsAt,
    votedOption = votedOptionIndex,
)

fun List<PostWithAuthor>.toDomainPosts(): List<Post> = mapNotNull { it.toDomain() }

fun PostEntity.toPreview() = PostPreview(
    id = id,
    type = postType(),
    gradientKey = gradientKey,
    emoji = emoji,
    caption = caption,
)

fun StoryWithUser.toDomain(): Story? {
    val owner = user ?: return null
    return Story(
        id = story.id,
        user = owner.toDomain(),
        frames = story.frames.map { StoryFrame(emoji = it.emoji, gradientKey = it.gradient, text = it.text) },
        createdAt = story.createdAt,
        isSeen = story.isSeen,
    )
}

fun CommentWithAuthor.toDomain(): Comment? {
    val user = author ?: return null
    return Comment(
        id = comment.id,
        postId = comment.postId,
        author = user.toDomain(),
        text = comment.text,
        likeCount = comment.likeCount,
        isLiked = comment.isLiked,
        createdAt = comment.createdAt,
    )
}

fun ActivityWithRelations.toDomain(): ActivityItem? {
    val user = actor ?: return null
    val kind = runCatching { ActivityType.valueOf(activity.type) }.getOrNull() ?: return null
    return ActivityItem(
        id = activity.id,
        type = kind,
        actor = user.toDomain(),
        post = post?.toPreview(),
        text = activity.text,
        createdAt = activity.createdAt,
        isRead = activity.isRead,
    )
}
