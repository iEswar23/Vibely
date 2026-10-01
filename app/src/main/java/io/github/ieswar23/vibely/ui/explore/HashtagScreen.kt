package io.github.ieswar23.vibely.ui.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ieswar23.vibely.data.repository.PostRepository
import io.github.ieswar23.vibely.domain.model.Post
import io.github.ieswar23.vibely.ui.common.EmptyState
import io.github.ieswar23.vibely.ui.common.gridSkeleton
import io.github.ieswar23.vibely.ui.common.postGridItems
import io.github.ieswar23.vibely.ui.navigation.AppNavigator
import io.github.ieswar23.vibely.ui.navigation.Routes
import io.github.ieswar23.vibely.ui.theme.BrandBrush
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HashtagUiState(val tag: String, val posts: List<Post> = emptyList(), val isLoading: Boolean = true)

@HiltViewModel
class HashtagViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    postRepository: PostRepository,
) : ViewModel() {
    val tag: String = checkNotNull(savedStateHandle.get<String>(Routes.ARG_TAG)).lowercase()

    val uiState: StateFlow<HashtagUiState> = postRepository.postsByHashtag(tag)
        .map { HashtagUiState(tag = tag, posts = it, isLoading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HashtagUiState(tag))

    init {
        // Hashtag pages search the whole catalogue, not just what the feed has paged in so far.
        viewModelScope.launch { postRepository.refreshExplore() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HashtagScreen(
    navigator: AppNavigator,
    viewModel: HashtagViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("#${state.tag}") },
                navigationIcon = {
                    IconButton(onClick = { navigator.back() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "header") {
                Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(BrandBrush),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Tag, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("#${state.tag}", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = if (state.posts.size == 1) "1 post" else "${state.posts.size} posts",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            when {
                state.isLoading -> gridSkeleton(9)
                state.posts.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }, key = "empty") {
                    EmptyState(
                        emoji = "#️⃣",
                        title = "No posts yet",
                        message = "Be the first to share something with #${state.tag}.",
                        actionLabel = "Create a post",
                        onAction = navigator::create,
                    )
                }
                else -> postGridItems(state.posts) { navigator.post(it.id) }
            }
        }
    }
}
