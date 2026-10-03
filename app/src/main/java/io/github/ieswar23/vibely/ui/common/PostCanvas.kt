package io.github.ieswar23.vibely.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.Poll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ieswar23.vibely.domain.model.PostPreview
import io.github.ieswar23.vibely.domain.model.PostType
import io.github.ieswar23.vibely.ui.theme.CanvasGradients

/**
 * The "photo" of a post: a gradient canvas with soft light blobs, a hero emoji and an optional
 * headline overlay. Rendered entirely in Compose so the app looks rich without network images.
 */
@Composable
fun PostCanvas(
    gradientKey: String?,
    emoji: String?,
    overlayText: String?,
    modifier: Modifier = Modifier,
    emojiSize: TextUnit = 112.sp,
    showOverlay: Boolean = true,
) {
    val gradient = remember(gradientKey) { CanvasGradients.forKey(gradientKey) }
    Box(
        modifier = modifier
            // The light blobs are centred near the edges; keep them inside the canvas so they never
            // bleed over the post header above (very visible in dark theme).
            .clipToBounds()
            .background(gradient.brush)
            .drawBehind {
                drawCircle(Color.White.copy(alpha = 0.14f), radius = size.minDimension * 0.42f, center = Offset(size.width * 0.86f, size.height * 0.12f))
                drawCircle(Color.White.copy(alpha = 0.08f), radius = size.minDimension * 0.30f, center = Offset(size.width * 0.08f, size.height * 0.78f))
                drawCircle(Color.Black.copy(alpha = 0.05f), radius = size.minDimension * 0.22f, center = Offset(size.width * 0.70f, size.height * 0.95f))
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = emoji ?: "✨",
            fontSize = emojiSize,
            style = TextStyle(shadow = Shadow(Color.Black.copy(alpha = 0.18f), Offset(0f, 8f), 24f)),
        )
        if (showOverlay && !overlayText.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.55f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.38f),
                        ),
                    ),
            )
            Text(
                text = overlayText,
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    shadow = Shadow(Color.Black.copy(alpha = 0.3f), Offset(0f, 2f), 8f),
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp),
            )
        }
    }
}

/** Grid tile for a text-only post: a quote card on a neutral surface. */
@Composable
fun TextPostTile(caption: String, modifier: Modifier = Modifier, compact: Boolean = false) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(Modifier.padding(if (compact) 4.dp else 10.dp)) {
            Icon(
                imageVector = Icons.Rounded.FormatQuote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(if (compact) 12.dp else 20.dp),
            )
            if (!compact) {
                Text(
                    text = caption,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        if (!compact) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Notes,
                contentDescription = "Text post",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(14.dp),
            )
        }
    }
}

/** Grid tile for a poll post: the question above a few faux result bars. */
@Composable
fun PollPostTile(question: String, modifier: Modifier = Modifier, compact: Boolean = false) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = if (compact) Alignment.Center else Alignment.TopStart,
    ) {
        if (compact) {
            Icon(
                imageVector = Icons.Rounded.Poll,
                contentDescription = "Poll",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
        } else {
            Column(Modifier.padding(10.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Poll,
                    contentDescription = "Poll",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = question,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
                listOf(0.8f, 0.55f, 0.3f).forEach { fraction ->
                    Box(
                        Modifier
                            .padding(top = 4.dp)
                            .fillMaxWidth(fraction)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    )
                }
            }
        }
    }
}

/** Square thumbnail for grids and activity rows. */
@Composable
fun PostThumbnail(
    type: PostType,
    gradientKey: String?,
    emoji: String?,
    caption: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    when (type) {
        PostType.CANVAS -> PostCanvas(
            gradientKey = gradientKey,
            emoji = emoji,
            overlayText = null,
            showOverlay = false,
            emojiSize = if (compact) 20.sp else 44.sp,
            modifier = modifier,
        )
        PostType.TEXT -> TextPostTile(caption = caption, modifier = modifier, compact = compact)
        PostType.POLL -> PollPostTile(question = caption, modifier = modifier, compact = compact)
    }
}

@Composable
fun PostThumbnail(preview: PostPreview, modifier: Modifier = Modifier, compact: Boolean = false) =
    PostThumbnail(preview.type, preview.gradientKey, preview.emoji, preview.caption, modifier, compact)
