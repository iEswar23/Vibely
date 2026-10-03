package io.github.ieswar23.vibely.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.ieswar23.vibely.domain.model.Post
import io.github.ieswar23.vibely.util.CountFormatter

/** Square post tile with a small like-count badge, used by Explore, Profile and Hashtag grids. */
@Composable
fun PostGridTile(post: Post, onClick: (Post) -> Unit, modifier: Modifier = Modifier, showLikes: Boolean = true) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick(post) },
    ) {
        PostThumbnail(
            type = post.type,
            gradientKey = post.gradientKey,
            emoji = post.emoji,
            caption = post.poll?.question ?: post.caption,
            modifier = Modifier.fillMaxSize(),
        )
        if (showLikes) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.Favorite,
                    contentDescription = null,
                    tint = if (post.type != io.github.ieswar23.vibely.domain.model.PostType.CANVAS) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
                    modifier = Modifier.size(12.dp),
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    text = CountFormatter.compact(post.likeCount),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (post.type != io.github.ieswar23.vibely.domain.model.PostType.CANVAS) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
                )
            }
        }
    }
}

fun LazyGridScope.postGridItems(posts: List<Post>, onClick: (Post) -> Unit) {
    items(posts, key = { it.id }, contentType = { "post" }) { post ->
        PostGridTile(post = post, onClick = onClick, modifier = Modifier.animateItem())
    }
}

/** Placeholder grid shown while explore / profile posts load. */
fun LazyGridScope.gridSkeleton(count: Int = 12) {
    items(count, contentType = { "skeleton" }) {
        Box(
            Modifier
                .aspectRatio(1f)
                .shimmer(RoundedCornerShape(6.dp)),
        )
    }
}

/** Placeholder for the home feed while the first page loads. */
@Composable
fun FeedSkeleton(modifier: Modifier = Modifier) {
    Column(modifier) {
        repeat(2) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(38.dp).shimmer(CircleShape))
                Spacer(Modifier.width(10.dp))
                Column {
                    Box(Modifier.width(140.dp).height(12.dp).shimmer())
                    Spacer(Modifier.height(6.dp))
                    Box(Modifier.width(90.dp).height(10.dp).shimmer())
                }
            }
            Box(Modifier.fillMaxWidth().aspectRatio(1f).shimmer(RoundedCornerShape(0.dp)))
            Column(Modifier.padding(16.dp)) {
                Box(Modifier.width(80.dp).height(12.dp).shimmer())
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth(0.9f).height(10.dp).shimmer())
                Spacer(Modifier.height(6.dp))
                Box(Modifier.fillMaxWidth(0.6f).height(10.dp).shimmer())
            }
        }
    }
}
