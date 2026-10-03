package io.github.ieswar23.vibely.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "users", indices = [Index(value = ["username"], unique = true)])
data class UserEntity(
    @PrimaryKey val id: String,
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

@Entity(
    tableName = "posts",
    indices = [Index("authorId"), Index("createdAt")],
)
data class PostEntity(
    @PrimaryKey val id: String,
    val authorId: String,
    val type: String,
    val gradientKey: String?,
    val emoji: String?,
    val overlayText: String?,
    val caption: String,
    val location: String?,
    /** Hashtags stored as ",travel,food," so a single LIKE query can match whole tags. */
    val hashtags: String,
    val likeCount: Int,
    val commentCount: Int,
    val isLiked: Boolean,
    val isBookmarked: Boolean,
    val bookmarkedAt: Long?,
    val createdAt: Long,
    /** Poll columns (`poll_question`, `poll_options`, ...); all NULL for non-poll posts. Added in DB version 2. */
    @Embedded(prefix = "poll_") val poll: PollEntity? = null,
)

data class PollEntity(
    val question: String,
    /** Options and their vote counts, serialised as JSON via [io.github.ieswar23.vibely.data.local.Converters]. */
    val options: List<PollOptionEntity>,
    val endsAt: Long,
    /** Option the current user voted for; null until they vote. */
    val votedOption: Int?,
)

data class PollOptionEntity(
    val text: String,
    val votes: Int,
)

data class PostWithAuthor(
    @Embedded val post: PostEntity,
    @Relation(parentColumn = "authorId", entityColumn = "id")
    val author: UserEntity?,
)

@Entity(tableName = "stories", indices = [Index("userId")])
data class StoryEntity(
    @PrimaryKey val id: String,
    val userId: String,
    /** Frames serialised as JSON via [io.github.ieswar23.vibely.data.local.Converters]. */
    val frames: List<StoryFrameEntity>,
    val createdAt: Long,
    val isSeen: Boolean,
)

data class StoryFrameEntity(
    val emoji: String,
    val gradient: String,
    val text: String,
)

data class StoryWithUser(
    @Embedded val story: StoryEntity,
    @Relation(parentColumn = "userId", entityColumn = "id")
    val user: UserEntity?,
)

@Entity(tableName = "comments", indices = [Index("postId")])
data class CommentEntity(
    @PrimaryKey val id: String,
    val postId: String,
    val authorId: String,
    val text: String,
    val likeCount: Int,
    val isLiked: Boolean,
    val createdAt: Long,
)

data class CommentWithAuthor(
    @Embedded val comment: CommentEntity,
    @Relation(parentColumn = "authorId", entityColumn = "id")
    val author: UserEntity?,
)

@Entity(tableName = "activity")
data class ActivityEntity(
    @PrimaryKey val id: String,
    val type: String,
    val actorId: String,
    val postId: String?,
    val text: String?,
    val createdAt: Long,
    val isRead: Boolean,
)

data class ActivityWithRelations(
    @Embedded val activity: ActivityEntity,
    @Relation(parentColumn = "actorId", entityColumn = "id")
    val actor: UserEntity?,
    @Relation(parentColumn = "postId", entityColumn = "id")
    val post: PostEntity?,
)

/**
 * Feed membership + paging bookkeeping. A post is part of the home feed when it has a key:
 * [page] is the API page it came from, or [LOCAL_PAGE] for posts the user published on-device.
 * Posts cached by other screens (Explore, profiles) have no key and stay out of the feed.
 */
@Entity(tableName = "feed_remote_keys", indices = [Index("page")])
data class FeedRemoteKeyEntity(
    @PrimaryKey val postId: String,
    val page: Int,
    /** Null on the last page of the feed. */
    val nextPage: Int?,
    val fetchedAt: Long,
) {
    companion object {
        const val LOCAL_PAGE = 0
    }
}
