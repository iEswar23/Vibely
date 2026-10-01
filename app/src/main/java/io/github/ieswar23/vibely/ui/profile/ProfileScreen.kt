package io.github.ieswar23.vibely.ui.profile

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ieswar23.vibely.domain.model.Post
import io.github.ieswar23.vibely.ui.common.EmptyState
import io.github.ieswar23.vibely.ui.common.FollowButton
import io.github.ieswar23.vibely.ui.common.TonalButton
import io.github.ieswar23.vibely.ui.common.UserAvatar
import io.github.ieswar23.vibely.ui.common.VerifiedBadge
import io.github.ieswar23.vibely.ui.common.gridSkeleton
import io.github.ieswar23.vibely.ui.common.postGridItems
import io.github.ieswar23.vibely.ui.common.rememberRichText
import io.github.ieswar23.vibely.ui.common.shareProfile
import io.github.ieswar23.vibely.ui.navigation.AppNavigator
import io.github.ieswar23.vibely.ui.theme.BrandBrush
import io.github.ieswar23.vibely.util.CountFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navigator: AppNavigator,
    showBack: Boolean = true,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var selectedTab by rememberSaveable { mutableStateOf(viewModel.initialTab) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ProfileEvent.Message -> snackbarHostState.showSnackbar(event.text)
            }
        }
    }

    val content = state as? ProfileUiState.Content
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (content?.isPrivate == true) {
                            Icon(Icons.Rounded.Lock, contentDescription = "Private account", modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(content?.user?.username.orEmpty())
                    }
                },
                navigationIcon = {
                    if (showBack) {
                        IconButton(onClick = { navigator.back() }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (content != null) {
                        IconButton(onClick = { shareProfile(context, content.user) }) {
                            Icon(Icons.Rounded.Share, contentDescription = "Share profile")
                        }
                        if (content.isMe) {
                            IconButton(onClick = navigator::settings) {
                                Icon(Icons.Rounded.Settings, contentDescription = "Settings")
                            }
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when (val current = state) {
            ProfileUiState.Loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            ProfileUiState.NotFound -> EmptyState(
                emoji = "🙈",
                title = "User not found",
                message = "This account may have changed its username.",
                actionLabel = "Go back",
                onAction = { navigator.back() },
                modifier = Modifier.fillMaxWidth().padding(padding),
            )
            is ProfileUiState.Content -> ProfileContent(
                state = current,
                selectedTab = if (current.isMe) selectedTab else ProfileTab.POSTS,
                onTabSelected = { selectedTab = it },
                onFollowClick = viewModel::toggleFollow,
                onEditProfile = navigator::editProfile,
                onShareProfile = { shareProfile(context, current.user) },
                onPostClick = { navigator.post(it.id) },
                onCreate = navigator::create,
                onHashtagClick = navigator::hashtag,
                onMentionClick = navigator::mention,
                contentPadding = padding,
            )
        }
    }
}

@Composable
private fun ProfileContent(
    state: ProfileUiState.Content,
    selectedTab: ProfileTab,
    onTabSelected: (ProfileTab) -> Unit,
    onFollowClick: () -> Unit,
    onEditProfile: () -> Unit,
    onShareProfile: () -> Unit,
    onPostClick: (Post) -> Unit,
    onCreate: () -> Unit,
    onHashtagClick: (String) -> Unit,
    onMentionClick: (String) -> Unit,
    contentPadding: PaddingValues,
) {
    val user = state.user
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }, key = "header") {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    UserAvatar(name = user.name, seed = user.id, size = 88.dp)
                    Spacer(Modifier.width(20.dp))
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatColumn(value = state.posts.size, label = "Posts")
                        StatColumn(value = user.followerCount, label = "Followers")
                        StatColumn(value = user.followingCount, label = "Following")
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(user.name, style = MaterialTheme.typography.titleMedium)
                    if (user.isVerified) {
                        Spacer(Modifier.width(4.dp))
                        VerifiedBadge(size = 16.dp)
                    }
                }
                if (user.bio.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = rememberRichText(user.bio, onHashtagClick, onMentionClick),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                if (!user.website.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Link, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(user.website, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.isMe) {
                        TonalButton(text = "Edit profile", onClick = onEditProfile, modifier = Modifier.weight(1f))
                        TonalButton(text = "Share profile", onClick = onShareProfile, modifier = Modifier.weight(1f))
                    } else {
                        FollowButton(
                            isFollowing = user.isFollowing,
                            onClick = onFollowClick,
                            followLabel = "Follow",
                            modifier = Modifier.weight(1f),
                        )
                        TonalButton(text = "Share", onClick = onShareProfile, modifier = Modifier.weight(1f))
                    }
                }
                if (state.isPrivate) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Private account is on. You can change this in Settings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }, key = "tabs") {
            if (state.isMe) {
                ProfileTabs(selected = selectedTab, onSelected = onTabSelected)
            } else {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(3.dp))
                }
            }
        }

        val posts = if (selectedTab == ProfileTab.SAVED) state.saved else state.posts
        when {
            selectedTab == ProfileTab.POSTS && state.isLoadingPosts -> gridSkeleton(9)
            posts.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }, key = "empty-$selectedTab") {
                when {
                    selectedTab == ProfileTab.SAVED -> EmptyState(
                        emoji = "🔖",
                        title = "Nothing saved yet",
                        message = "Tap the bookmark on any post to keep it here. Only you can see what you've saved.",
                    )
                    state.isMe -> EmptyState(
                        emoji = "📸",
                        title = "Share your first moment",
                        message = "Your posts will appear here.",
                        actionLabel = "Create a post",
                        onAction = onCreate,
                    )
                    else -> EmptyState(
                        emoji = "🌙",
                        title = "No posts yet",
                        message = "When ${user.name} shares something, it'll show up here.",
                    )
                }
            }
            else -> postGridItems(posts, onPostClick)
        }
    }
}

@Composable
private fun StatColumn(value: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedContent(
            targetState = value,
            transitionSpec = {
                val up = targetState > initialState
                (slideInVertically(tween(220)) { if (up) it else -it } + fadeIn()) togetherWith
                    (slideOutVertically(tween(220)) { if (up) -it else it } + fadeOut())
            },
            label = "stat-$label",
        ) { count ->
            Text(
                text = CountFormatter.compact(count),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
        }
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ProfileTabs(selected: ProfileTab, onSelected: (ProfileTab) -> Unit) {
    TabRow(
        selectedTabIndex = selected.ordinal,
        containerColor = MaterialTheme.colorScheme.background,
        indicator = { positions ->
            Box(
                Modifier
                    .tabIndicatorOffset(positions[selected.ordinal])
                    .height(3.dp)
                    .padding(horizontal = 36.dp)
                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                    .background(BrandBrush),
            )
        },
        divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) },
    ) {
        Tab(
            selected = selected == ProfileTab.POSTS,
            onClick = { onSelected(ProfileTab.POSTS) },
            icon = { Icon(Icons.Rounded.GridView, contentDescription = "Posts") },
            selectedContentColor = MaterialTheme.colorScheme.onSurface,
            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Tab(
            selected = selected == ProfileTab.SAVED,
            onClick = { onSelected(ProfileTab.SAVED) },
            icon = { Icon(Icons.Rounded.BookmarkBorder, contentDescription = "Saved") },
            selectedContentColor = MaterialTheme.colorScheme.onSurface,
            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
