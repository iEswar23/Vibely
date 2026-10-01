package io.github.ieswar23.vibely.domain.model

data class User(
    val id: String,
    val name: String,
    val username: String,
    val bio: String,
    val website: String?,
    val isVerified: Boolean,
    val followerCount: Int,
    val followingCount: Int,
    val isFollowing: Boolean,
    val isCurrentUser: Boolean,
)

enum class PostType { CANVAS, TEXT }

data class Post(
    val id: String,
    val author: User,
    val type: PostType,
    /** Key into the gradient palette (see `ui.theme.CanvasGradients`); null for text posts. */
    val gradientKey: String?,
    val emoji: String?,
    val overlayText: String?,
    val caption: String,
    val location: String?,
    val hashtags: List<String>,
    val likeCount: Int,
    val commentCount: Int,
    val isLiked: Boolean,
    val isBookmarked: Boolean,
    val createdAt: Long,
)

data class StoryFrame(
    val emoji: String,
    val gradientKey: String,
    val text: String,
)

data class Story(
    val id: String,
    val user: User,
    val frames: List<StoryFrame>,
    val createdAt: Long,
    val isSeen: Boolean,
)

data class Comment(
    val id: String,
    val postId: String,
    val author: User,
    val text: String,
    val likeCount: Int,
    val isLiked: Boolean,
    val createdAt: Long,
)

enum class ActivityType { LIKE, COMMENT, FOLLOW, MENTION }

/** Lightweight post preview shown as a thumbnail next to activity rows. */
data class PostPreview(
    val id: String,
    val type: PostType,
    val gradientKey: String?,
    val emoji: String?,
    val caption: String,
)

data class ActivityItem(
    val id: String,
    val type: ActivityType,
    val actor: User,
    val post: PostPreview?,
    val text: String?,
    val createdAt: Long,
    val isRead: Boolean,
)

data class HashtagStat(val tag: String, val postCount: Int)

/** What the user composed on the Create screen, before it is published. */
data class PostDraft(
    val type: PostType,
    val gradientKey: String?,
    val emoji: String?,
    val overlayText: String?,
    val caption: String,
    val location: String?,
)

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val privateAccount: Boolean = false,
)
