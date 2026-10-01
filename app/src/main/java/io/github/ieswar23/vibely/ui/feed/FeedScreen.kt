package io.github.ieswar23.vibely.ui.feed

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import io.github.ieswar23.vibely.ui.common.EmptyState
import io.github.ieswar23.vibely.ui.common.FeedSkeleton
import io.github.ieswar23.vibely.ui.common.PostActions
import io.github.ieswar23.vibely.ui.common.PostCard
import io.github.ieswar23.vibely.ui.common.VibelyWordmark
import io.github.ieswar23.vibely.ui.navigation.AppNavigator
import io.github.ieswar23.vibely.ui.navigation.Routes
import kotlinx.coroutines.launch

@Composable
fun FeedScreen(
    navigator: AppNavigator,
    viewModel: FeedViewModel = hiltViewModel(),
) {
    val posts = viewModel.feed.collectAsLazyPagingItems()
    val stories by viewModel.storiesState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is FeedEvent.Message -> snackbarHostState.showSnackbar(event.text)
                is FeedEvent.BookmarkChanged -> {
                    val result = snackbarHostState.showSnackbar(
                        message = if (event.saved) "Saved to your collection" else "Removed from saved",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.undoBookmark(event.postId, event.saved)
                }
            }
        }
    }

    val actions = remember(viewModel, navigator) {
        PostActions(
            onLike = viewModel::onLikeClick,
            onDoubleTapLike = viewModel::onDoubleTapLike,
            onComment = { navigator.comments(it.id) },
            onBookmark = viewModel::onBookmarkClick,
            onAuthorClick = { navigator.profile(it.author.id) },
            onHashtagClick = navigator::hashtag,
            onMentionClick = navigator::mention,
        )
    }

    FeedContent(
        posts = posts,
        stories = stories,
        actions = actions,
        snackbarHostState = snackbarHostState,
        onRefresh = {
            posts.refresh()
            viewModel.refreshStories()
        },
        onStoryClick = { navigator.story(it.user.id) },
        onCreateClick = navigator::create,
        onSavedClick = { navigator.profile(Routes.MY_PROFILE_SAVED_KEY) },
        onSettingsClick = navigator::settings,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedContent(
    posts: LazyPagingItems<io.github.ieswar23.vibely.domain.model.Post>,
    stories: StoriesUiState,
    actions: PostActions,
    snackbarHostState: SnackbarHostState,
    onRefresh: () -> Unit,
    onStoryClick: (io.github.ieswar23.vibely.domain.model.Story) -> Unit,
    onCreateClick: () -> Unit,
    onSavedClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val refreshState = posts.loadState.refresh
    val isRefreshing = refreshState is LoadState.Loading && posts.itemCount > 0

    LaunchedEffect(refreshState) {
        if (refreshState is LoadState.Error && posts.itemCount > 0) {
            snackbarHostState.showSnackbar("Couldn't refresh your feed")
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    VibelyWordmark(
                        modifier = Modifier.padding(start = 2.dp),
                    )
                },
                actions = {
                    IconButton(onClick = onSavedClick) {
                        Icon(Icons.Rounded.BookmarkBorder, contentDescription = "Saved posts")
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Settings")
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                item(key = "stories", contentType = "stories") {
                    Column {
                        StoriesRow(state = stories, onStoryClick = onStoryClick, onAddStoryClick = onCreateClick)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }

                if (posts.itemCount == 0) {
                    when {
                        refreshState is LoadState.Error -> item(key = "error") {
                            EmptyState(
                                emoji = "📡",
                                title = "Couldn't load your feed",
                                message = "Check your connection and give it another try.",
                                actionLabel = "Retry",
                                onAction = { posts.retry() },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        // Only a completed load with no more pages is truly empty; anything else is
                        // still warming up (including the very first frame before Paging starts).
                        refreshState is LoadState.NotLoading && posts.loadState.append.endOfPaginationReached -> item(key = "empty") {
                            EmptyState(
                                emoji = "🌱",
                                title = "Your feed is quiet",
                                message = "Share your first moment or follow a few creators to get started.",
                                actionLabel = "Create a post",
                                onAction = onCreateClick,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        else -> item(key = "skeleton") { FeedSkeleton() }
                    }
                }

                items(
                    count = posts.itemCount,
                    key = posts.itemKey { it.id },
                    contentType = posts.itemContentType { "post" },
                ) { index ->
                    posts[index]?.let { post -> PostCard(post = post, actions = actions) }
                }

                item(key = "append", contentType = "footer") {
                    FeedFooter(
                        appendState = posts.loadState.append,
                        hasItems = posts.itemCount > 0,
                        onRetry = { posts.retry() },
                        onBackToTop = { scope.launch { listState.animateScrollToItem(0) } },
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedFooter(appendState: LoadState, hasItems: Boolean, onRetry: () -> Unit, onBackToTop: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        when {
            appendState is LoadState.Loading -> CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
            appendState is LoadState.Error -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Couldn't load more posts", style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = onRetry) { Text("Retry") }
            }
            appendState.endOfPaginationReached && hasItems -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "✨ You're all caught up",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "You've seen every new post from the last few weeks.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                TextButton(onClick = onBackToTop) { Text("Back to top") }
            }
        }
    }
}
