package io.github.ieswar23.vibely.ui.explore

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ieswar23.vibely.domain.model.HashtagStat
import io.github.ieswar23.vibely.domain.model.User
import io.github.ieswar23.vibely.ui.common.EmptyState
import io.github.ieswar23.vibely.ui.common.SectionHeader
import io.github.ieswar23.vibely.ui.common.UserAvatar
import io.github.ieswar23.vibely.ui.common.VerifiedBadge
import io.github.ieswar23.vibely.ui.common.gridSkeleton
import io.github.ieswar23.vibely.ui.common.postGridItems
import io.github.ieswar23.vibely.ui.navigation.AppNavigator
import io.github.ieswar23.vibely.ui.theme.BrandBrush
import io.github.ieswar23.vibely.util.CountFormatter

@Composable
fun ExploreScreen(
    navigator: AppNavigator,
    viewModel: ExploreViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.searchResults.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        SearchField(query = query, onQueryChange = viewModel::onQueryChange)
        AnimatedContent(
            targetState = query.isBlank(),
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "exploreMode",
        ) { browsing ->
            if (browsing) {
                ExploreGrid(state = state, navigator = navigator, onRetry = viewModel::refresh)
            } else {
                SearchResultsList(results = results, navigator = navigator)
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    val focusManager = LocalFocusManager.current
    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search people and #hashtags") },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) { Icon(Icons.Rounded.Close, contentDescription = "Clear search") }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

@Composable
private fun ExploreGrid(state: ExploreUiState, navigator: AppNavigator, onRetry: () -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (state.trending.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "trending") {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)) {
                        Icon(Icons.AutoMirrored.Rounded.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Trending now", style = MaterialTheme.typography.titleSmall)
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.trending, key = { it.tag }) { stat ->
                            FilterChip(
                                selected = false,
                                onClick = { navigator.hashtag(stat.tag) },
                                label = { Text("#${stat.tag}") },
                                trailingIcon = {
                                    Text(
                                        text = stat.postCount.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                },
                            )
                        }
                    }
                    Spacer(Modifier.size(8.dp))
                }
            }
        }
        when {
            state.isLoading -> gridSkeleton()
            state.loadFailed -> item(span = { GridItemSpan(maxLineSpan) }, key = "error") {
                EmptyState(
                    emoji = "🧭",
                    title = "Explore is taking a break",
                    message = "We couldn't load popular posts right now.",
                    actionLabel = "Retry",
                    onAction = onRetry,
                )
            }
            else -> postGridItems(state.posts) { navigator.post(it.id) }
        }
    }
}

@Composable
private fun SearchResultsList(results: SearchResults, navigator: AppNavigator) {
    if (results.query.isNotEmpty() && results.isEmpty) {
        EmptyState(
            emoji = "🔍",
            title = "No results for \"${results.query}\"",
            message = "Try a different name, username or hashtag.",
            modifier = Modifier.fillMaxWidth(),
        )
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        if (results.users.isNotEmpty()) {
            item(key = "people") { SectionHeader("People") }
            items(results.users, key = { "u-${it.id}" }) { user ->
                UserResultRow(user = user, onClick = { navigator.profile(user.id) })
            }
        }
        if (results.hashtags.isNotEmpty()) {
            item(key = "tags") { SectionHeader("Hashtags") }
            items(results.hashtags, key = { "t-${it.tag}" }) { stat ->
                HashtagResultRow(stat = stat, onClick = { navigator.hashtag(stat.tag) })
            }
        }
    }
}

@Composable
fun UserResultRow(user: User, onClick: () -> Unit, trailing: (@Composable () -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UserAvatar(name = user.name, seed = user.id, size = 48.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(user.username, style = MaterialTheme.typography.titleSmall)
                if (user.isVerified) {
                    Spacer(Modifier.width(4.dp))
                    VerifiedBadge()
                }
            }
            Text(
                text = "${user.name} • ${CountFormatter.compact(user.followerCount)} followers" + if (user.isFollowing) " • Following" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        trailing?.invoke()
    }
}

@Composable
private fun HashtagResultRow(stat: HashtagStat, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(BrandBrush),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Tag, contentDescription = null, tint = Color.White)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text("#${stat.tag}", style = MaterialTheme.typography.titleSmall)
            Text(
                text = if (stat.postCount == 1) "1 post" else "${stat.postCount} posts",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
