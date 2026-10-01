package io.github.ieswar23.vibely.ui.activity

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ieswar23.vibely.domain.model.ActivityItem
import io.github.ieswar23.vibely.domain.model.ActivityType
import io.github.ieswar23.vibely.ui.common.EmptyState
import io.github.ieswar23.vibely.ui.common.FollowButton
import io.github.ieswar23.vibely.ui.common.LocalNow
import io.github.ieswar23.vibely.ui.common.PostThumbnail
import io.github.ieswar23.vibely.ui.common.SectionHeader
import io.github.ieswar23.vibely.ui.common.UserAvatar
import io.github.ieswar23.vibely.ui.common.shimmer
import io.github.ieswar23.vibely.ui.explore.UserResultRow
import io.github.ieswar23.vibely.ui.navigation.AppNavigator
import io.github.ieswar23.vibely.ui.theme.LikeRed
import io.github.ieswar23.vibely.ui.theme.Pink500
import io.github.ieswar23.vibely.ui.theme.Violet600
import io.github.ieswar23.vibely.util.TimeAgo
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ActivityScreen(
    navigator: AppNavigator,
    viewModel: ActivityViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }
    // Give the unread highlight a moment on screen before clearing the badge.
    LaunchedEffect(Unit) {
        delay(1_500)
        viewModel.markAllRead()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Activity") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            LazyColumn(Modifier.fillMaxSize()) {
                if (state.isLoading) {
                    items(8, key = { "skeleton-$it" }) { ActivitySkeletonRow() }
                } else if (state.sections.isEmpty()) {
                    item(key = "empty") {
                        EmptyState(
                            emoji = "🔔",
                            title = "No activity yet",
                            message = "When people like, comment on or follow you, you'll see it here.",
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                state.sections.forEach { (bucket, items) ->
                    stickyHeader(key = "header-${bucket.name}") {
                        SectionHeader(
                            title = bucket.label,
                            modifier = Modifier.background(MaterialTheme.colorScheme.background),
                        )
                    }
                    items(items, key = { it.id }) { item ->
                        ActivityRow(
                            item = item,
                            onActorClick = { navigator.profile(item.actor.id) },
                            onRowClick = {
                                val post = item.post
                                if (post != null) navigator.post(post.id) else navigator.profile(item.actor.id)
                            },
                            onFollowClick = { viewModel.toggleFollow(item.actor) },
                        )
                    }
                }
                if (state.suggestions.isNotEmpty()) {
                    item(key = "suggested") { SectionHeader("Suggested for you") }
                    items(state.suggestions, key = { "s-${it.id}" }) { user ->
                        UserResultRow(
                            user = user,
                            onClick = { navigator.profile(user.id) },
                            trailing = { FollowButton(isFollowing = user.isFollowing, onClick = { viewModel.toggleFollow(user) }, compact = true) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityRow(
    item: ActivityItem,
    onActorClick: () -> Unit,
    onRowClick: () -> Unit,
    onFollowClick: () -> Unit,
) {
    val now = LocalNow.current
    val background by animateColorAsState(
        targetValue = if (item.isRead) Color.Transparent else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
        animationSpec = tween(600),
        label = "unread",
    )
    val (badgeIcon, badgeColor) = badgeFor(item.type)
    val action = when (item.type) {
        ActivityType.LIKE -> " liked your post."
        ActivityType.COMMENT -> " commented: ${item.text.orEmpty()}"
        ActivityType.FOLLOW -> " started following you."
        ActivityType.MENTION -> " mentioned you: ${item.text.orEmpty()}"
    }
    val onSurface = MaterialTheme.colorScheme.onSurface
    val subtle = MaterialTheme.colorScheme.onSurfaceVariant
    val text = remember(item, now, onSurface, subtle) {
        buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = onSurface)) { append(item.actor.username) }
            withStyle(SpanStyle(color = onSurface)) { append(action) }
            withStyle(SpanStyle(color = subtle)) { append("  ${TimeAgo.short(item.createdAt, now)}") }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .clickable(onClick = onRowClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            UserAvatar(name = item.actor.name, seed = item.actor.id, size = 46.dp, onClick = onActorClick)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 4.dp, y = 4.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(badgeColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(badgeIcon, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
            }
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 3,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        val post = item.post
        when {
            item.type == ActivityType.FOLLOW -> FollowButton(
                isFollowing = item.actor.isFollowing,
                onClick = onFollowClick,
                compact = true,
                followLabel = "Follow back",
            )
            post != null -> PostThumbnail(
                preview = post,
                compact = true,
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
        }
    }
}

private fun badgeFor(type: ActivityType): Pair<ImageVector, Color> = when (type) {
    ActivityType.LIKE -> Icons.Rounded.Favorite to LikeRed
    ActivityType.COMMENT -> Icons.AutoMirrored.Rounded.Chat to Violet600
    ActivityType.FOLLOW -> Icons.Rounded.PersonAdd to Pink500
    ActivityType.MENTION -> Icons.Rounded.AlternateEmail to Color(0xFF0EA5E9)
}

@Composable
private fun ActivitySkeletonRow() {
    Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(46.dp).shimmer(CircleShape))
        Spacer(Modifier.width(14.dp))
        Box(Modifier.weight(1f).size(height = 12.dp, width = 0.dp).shimmer())
        Spacer(Modifier.width(12.dp))
        Box(Modifier.size(46.dp).shimmer(RoundedCornerShape(8.dp)))
    }
}
