package io.github.ieswar23.vibely.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.ieswar23.vibely.domain.model.Story
import io.github.ieswar23.vibely.domain.model.User
import io.github.ieswar23.vibely.ui.common.StoryRing
import io.github.ieswar23.vibely.ui.common.UserAvatar
import io.github.ieswar23.vibely.ui.common.shimmer
import io.github.ieswar23.vibely.ui.theme.BrandBrush

private val BubbleSize = 68.dp

@Composable
fun StoriesRow(
    state: StoriesUiState,
    onStoryClick: (Story) -> Unit,
    onAddStoryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "you") {
            YourStoryBubble(me = state.currentUser, onClick = onAddStoryClick)
        }
        if (state.isLoading) {
            items(6, key = { "placeholder-$it" }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(BubbleSize + 6.dp)) {
                    Box(Modifier.size(BubbleSize).shimmer(CircleShape))
                    Spacer(Modifier.height(6.dp))
                    Box(Modifier.width(48.dp).height(9.dp).shimmer())
                }
            }
        } else {
            items(state.stories, key = { it.id }) { story ->
                StoryBubble(story = story, onClick = { onStoryClick(story) }, modifier = Modifier.animateItem())
            }
        }
    }
}

@Composable
private fun StoryBubble(story: Story, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .width(BubbleSize + 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        UserAvatar(
            name = story.user.name,
            seed = story.user.id,
            size = BubbleSize,
            ring = if (story.isSeen) StoryRing.SEEN else StoryRing.UNSEEN,
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = story.user.username,
            style = MaterialTheme.typography.labelSmall,
            color = if (story.isSeen) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun YourStoryBubble(me: User?, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(BubbleSize + 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(BubbleSize)) {
            if (me != null) {
                UserAvatar(
                    name = me.name,
                    seed = me.id,
                    size = BubbleSize - 8.dp,
                    modifier = Modifier.align(Alignment.Center),
                )
            } else {
                Box(Modifier.size(BubbleSize - 8.dp).align(Alignment.Center).shimmer(CircleShape))
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(24.dp)
                    .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(BrandBrush),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Create a post", tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(
            text = "New post",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}
