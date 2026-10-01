package io.github.ieswar23.vibely.ui.post

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ieswar23.vibely.ui.common.EmptyState
import io.github.ieswar23.vibely.ui.common.PostActions
import io.github.ieswar23.vibely.ui.common.PostCard
import io.github.ieswar23.vibely.ui.common.SectionHeader
import io.github.ieswar23.vibely.ui.common.postGridItems
import io.github.ieswar23.vibely.ui.navigation.AppNavigator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    navigator: AppNavigator,
    viewModel: PostDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Post") },
                navigationIcon = {
                    IconButton(onClick = { navigator.back() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when (val current = state) {
            PostDetailUiState.Loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            PostDetailUiState.NotFound -> EmptyState(
                emoji = "🫥",
                title = "Post unavailable",
                message = "This post may have been removed.",
                actionLabel = "Go back",
                onAction = { navigator.back() },
                modifier = Modifier.fillMaxWidth().padding(padding),
            )
            is PostDetailUiState.Content -> LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "post") {
                    PostCard(post = current.post, actions = actions)
                }
                if (current.moreFromAuthor.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }, key = "more") {
                        Column {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            SectionHeader(title = "More from ${current.post.author.username}")
                        }
                    }
                    postGridItems(current.moreFromAuthor) { navigator.post(it.id) }
                }
            }
        }
    }
}
