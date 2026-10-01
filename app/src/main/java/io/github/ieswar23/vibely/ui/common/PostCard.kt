package io.github.ieswar23.vibely.ui.common

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ieswar23.vibely.domain.model.Post
import io.github.ieswar23.vibely.domain.model.PostType
import io.github.ieswar23.vibely.ui.theme.LikeRed
import io.github.ieswar23.vibely.util.CountFormatter
import io.github.ieswar23.vibely.util.TimeAgo

/** User intents a post card can emit; grouped to keep call sites tidy. */
data class PostActions(
    val onLike: (Post) -> Unit,
    val onDoubleTapLike: (Post) -> Unit,
    val onComment: (Post) -> Unit,
    val onBookmark: (Post) -> Unit,
    val onAuthorClick: (Post) -> Unit,
    val onHashtagClick: (String) -> Unit,
    val onMentionClick: (String) -> Unit,
)

@Composable
fun PostCard(
    post: Post,
    actions: PostActions,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val now = LocalNow.current
    var burstTrigger by remember { mutableIntStateOf(0) }
    val currentPost by rememberUpdatedState(post)

    val doubleTapModifier = Modifier.pointerInput(post.id) {
        detectTapGestures(
            onDoubleTap = {
                burstTrigger++
                actions.onDoubleTapLike(currentPost)
            },
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        PostHeader(
            post = post,
            timeAgo = TimeAgo.short(post.createdAt, now),
            onAuthorClick = { actions.onAuthorClick(post) },
            onShare = { sharePost(context, post) },
            onCopy = { copyToClipboard(context, "Caption", post.caption) },
            onBookmark = { actions.onBookmark(post) },
        )

        when (post.type) {
            PostType.CANVAS -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .then(doubleTapModifier),
                contentAlignment = Alignment.Center,
            ) {
                PostCanvas(
                    gradientKey = post.gradientKey,
                    emoji = post.emoji,
                    overlayText = post.overlayText,
                    modifier = Modifier.fillMaxSize(),
                )
                HeartBurst(trigger = burstTrigger)
            }
            PostType.TEXT -> Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .heightIn(min = 140.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .then(doubleTapModifier),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = rememberRichText(post.caption, actions.onHashtagClick, actions.onMentionClick),
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, lineHeight = 26.sp, fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                )
                HeartBurst(trigger = burstTrigger, size = 88.dp)
            }
        }

        PostActionRow(
            post = post,
            onLike = { actions.onLike(post) },
            onComment = { actions.onComment(post) },
            onShare = { sharePost(context, post) },
            onBookmark = { actions.onBookmark(post) },
        )

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = CountFormatter.likes(post.likeCount),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (post.type == PostType.CANVAS && post.caption.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                ExpandableCaption(post = post, actions = actions)
            }
            if (post.commentCount > 0) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (post.commentCount == 1) "View 1 comment" else "View all ${post.commentCount} comments",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { actions.onComment(post) },
                )
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun PostHeader(
    post: Post,
    timeAgo: String,
    onAuthorClick: () -> Unit,
    onShare: () -> Unit,
    onCopy: () -> Unit,
    onBookmark: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UserAvatar(name = post.author.name, seed = post.author.id, size = 38.dp, onClick = onAuthorClick)
        Spacer(Modifier.width(10.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onAuthorClick),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = post.author.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (post.author.isVerified) {
                    Spacer(Modifier.width(4.dp))
                    VerifiedBadge()
                }
                Text(
                    text = "  •  $timeAgo",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "@${post.author.username}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                if (!post.location.isNullOrBlank()) {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Rounded.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(12.dp),
                    )
                    Text(
                        text = post.location,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Rounded.MoreVert, contentDescription = "More options", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(if (post.isBookmarked) "Remove from saved" else "Save") },
                    leadingIcon = { Icon(if (post.isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder, null) },
                    onClick = { menuOpen = false; onBookmark() },
                )
                DropdownMenuItem(
                    text = { Text("Share") },
                    leadingIcon = { Icon(Icons.Rounded.Share, null) },
                    onClick = { menuOpen = false; onShare() },
                )
                if (post.caption.isNotBlank()) {
                    DropdownMenuItem(
                        text = { Text("Copy caption") },
                        leadingIcon = { Icon(Icons.Rounded.ContentCopy, null) },
                        onClick = { menuOpen = false; onCopy() },
                    )
                }
                DropdownMenuItem(
                    text = { Text("View profile") },
                    leadingIcon = { Icon(Icons.Rounded.Person, null) },
                    onClick = { menuOpen = false; onAuthorClick() },
                )
            }
        }
    }
}

@Composable
private fun PostActionRow(
    post: Post,
    onLike: () -> Unit,
    onComment: () -> Unit,
    onShare: () -> Unit,
    onBookmark: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        BouncyToggleIcon(
            active = post.isLiked,
            onClick = onLike,
            activeIcon = { Icon(Icons.Rounded.Favorite, contentDescription = "Unlike", tint = LikeRed) },
            inactiveIcon = { Icon(Icons.Rounded.FavoriteBorder, contentDescription = "Like", tint = MaterialTheme.colorScheme.onSurface) },
        )
        IconButton(onClick = onComment) {
            Icon(Icons.Rounded.ChatBubbleOutline, contentDescription = "Comment", tint = MaterialTheme.colorScheme.onSurface)
        }
        IconButton(onClick = onShare) {
            Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Share", tint = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.weight(1f))
        BouncyToggleIcon(
            active = post.isBookmarked,
            onClick = onBookmark,
            activeIcon = { Icon(Icons.Rounded.Bookmark, contentDescription = "Remove from saved", tint = MaterialTheme.colorScheme.onSurface) },
            inactiveIcon = { Icon(Icons.Rounded.BookmarkBorder, contentDescription = "Save", tint = MaterialTheme.colorScheme.onSurface) },
        )
    }
}

/** Icon button that does a quick springy scale whenever it becomes active. */
@Composable
fun BouncyToggleIcon(
    active: Boolean,
    onClick: () -> Unit,
    activeIcon: @Composable () -> Unit,
    inactiveIcon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale = remember { Animatable(1f) }
    var previous by remember { mutableStateOf(active) }
    LaunchedEffect(active) {
        if (active && !previous) {
            scale.animateTo(1.3f, tween(90))
            scale.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMedium))
        }
        previous = active
    }
    IconButton(onClick = onClick, modifier = modifier) {
        Box(Modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value }) {
            if (active) activeIcon() else inactiveIcon()
        }
    }
}

@Composable
private fun ExpandableCaption(post: Post, actions: PostActions) {
    var expanded by rememberSaveable(post.id) { mutableStateOf(false) }
    var overflowing by remember { mutableStateOf(false) }
    val caption = rememberCaption(
        username = post.author.username,
        text = post.caption,
        onUsernameClick = { actions.onAuthorClick(post) },
        onHashtagClick = actions.onHashtagClick,
        onMentionClick = actions.onMentionClick,
    )
    Column(Modifier.animateContentSize()) {
        Text(
            text = caption,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = if (expanded) Int.MAX_VALUE else 2,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { if (!expanded) overflowing = it.hasVisualOverflow },
        )
        if (overflowing && !expanded) {
            Text(
                text = "more",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable { expanded = true },
            )
        }
    }
}
