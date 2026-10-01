package io.github.ieswar23.vibely.ui.comments

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ieswar23.vibely.domain.model.Comment
import io.github.ieswar23.vibely.domain.model.Post
import io.github.ieswar23.vibely.ui.common.EmptyState
import io.github.ieswar23.vibely.ui.common.LocalNow
import io.github.ieswar23.vibely.ui.common.UserAvatar
import io.github.ieswar23.vibely.ui.common.rememberCaption
import io.github.ieswar23.vibely.ui.common.shimmer
import io.github.ieswar23.vibely.ui.navigation.AppNavigator
import io.github.ieswar23.vibely.ui.theme.LikeRed
import io.github.ieswar23.vibely.util.CountFormatter
import io.github.ieswar23.vibely.util.TimeAgo

private val QuickReactions = listOf("❤️", "🙌", "🔥", "👏", "😍", "😂", "😮", "🎉")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsScreen(
    navigator: AppNavigator,
    viewModel: CommentsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                CommentsEvent.CommentPosted -> {
                    val lastIndex = listState.layoutInfo.totalItemsCount - 1
                    if (lastIndex >= 0) listState.animateScrollToItem(lastIndex)
                }
                is CommentsEvent.Message -> snackbarHostState.showSnackbar(event.text)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Comments") },
                navigationIcon = {
                    IconButton(onClick = { navigator.back() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            CommentInputBar(
                state = state,
                onInputChange = viewModel::onInputChange,
                onReaction = viewModel::appendToInput,
                onSend = viewModel::send,
            )
        },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            state.post?.let { post ->
                item(key = "header") {
                    CaptionHeader(post = post, navigator = navigator)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
            when {
                state.isLoading -> items(5, key = { "skeleton-$it" }) { CommentSkeleton() }
                state.loadFailed -> item(key = "error") {
                    EmptyState(
                        emoji = "💬",
                        title = "Couldn't load comments",
                        message = "Something went wrong while loading the conversation.",
                        actionLabel = "Retry",
                        onAction = viewModel::refresh,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                state.comments.isEmpty() -> item(key = "empty") {
                    EmptyState(
                        emoji = "💬",
                        title = "No comments yet",
                        message = "Start the conversation.",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                else -> items(state.comments, key = { it.id }) { comment ->
                    CommentRow(
                        comment = comment,
                        onLike = { viewModel.toggleCommentLike(comment) },
                        onReply = { viewModel.replyTo(comment) },
                        navigator = navigator,
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}

@Composable
private fun CaptionHeader(post: Post, navigator: AppNavigator) {
    val now = LocalNow.current
    Row(modifier = Modifier.padding(16.dp)) {
        UserAvatar(name = post.author.name, seed = post.author.id, size = 36.dp, onClick = { navigator.profile(post.author.id) })
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = rememberCaption(
                    username = post.author.username,
                    text = post.caption,
                    onUsernameClick = { navigator.profile(post.author.id) },
                    onHashtagClick = navigator::hashtag,
                    onMentionClick = navigator::mention,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.size(4.dp))
            Text(
                text = TimeAgo.short(post.createdAt, now),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CommentRow(
    comment: Comment,
    onLike: () -> Unit,
    onReply: () -> Unit,
    navigator: AppNavigator,
    modifier: Modifier = Modifier,
) {
    val now = LocalNow.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 6.dp),
    ) {
        UserAvatar(name = comment.author.name, seed = comment.author.id, size = 34.dp, onClick = { navigator.profile(comment.author.id) })
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = rememberCaption(
                    username = comment.author.username,
                    text = comment.text,
                    onUsernameClick = { navigator.profile(comment.author.id) },
                    onHashtagClick = navigator::hashtag,
                    onMentionClick = navigator::mention,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.size(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = TimeAgo.short(comment.createdAt, now),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (comment.likeCount > 0) {
                    Text(
                        text = CountFormatter.likes(comment.likeCount),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "Reply",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable(onClick = onReply),
                )
            }
        }
        IconButton(onClick = onLike) {
            Icon(
                imageVector = if (comment.isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = if (comment.isLiked) "Unlike comment" else "Like comment",
                tint = if (comment.isLiked) LikeRed else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun CommentSkeleton() {
    Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(34.dp).shimmer(RoundedCornerShape(50)))
        Spacer(Modifier.width(12.dp))
        Column {
            Box(Modifier.width(180.dp).size(width = 180.dp, height = 10.dp).shimmer())
            Spacer(Modifier.size(6.dp))
            Box(Modifier.size(width = 110.dp, height = 10.dp).shimmer())
        }
    }
}

@Composable
private fun CommentInputBar(
    state: CommentsUiState,
    onInputChange: (String) -> Unit,
    onReaction: (String) -> Unit,
    onSend: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .imePadding(),
        ) {
            LazyRow(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(QuickReactions) { emoji ->
                    Text(
                        text = emoji,
                        fontSize = 24.sp,
                        modifier = Modifier
                            .clickable { onReaction(emoji) }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
            Row(
                modifier = Modifier.padding(start = 12.dp, end = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val me = state.currentUser
                if (me != null) UserAvatar(name = me.name, seed = me.id, size = 36.dp)
                Spacer(Modifier.width(10.dp))
                TextField(
                    value = state.input,
                    onValueChange = onInputChange,
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(state.post?.let { "Add a comment for @${it.author.username}…" } ?: "Add a comment…")
                    },
                    maxLines = 4,
                    shape = RoundedCornerShape(22.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                )
                Box(Modifier.size(width = 64.dp, height = 48.dp), contentAlignment = Alignment.Center) {
                    if (state.isSending) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text(
                            text = "Post",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (state.canSend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .clickable(enabled = state.canSend, onClick = onSend)
                                .padding(8.dp),
                        )
                    }
                }
            }
        }
    }
}
