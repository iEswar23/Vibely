package io.github.ieswar23.vibely.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ieswar23.vibely.ui.theme.BrandBrush
import io.github.ieswar23.vibely.ui.theme.LocalIsDarkTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** The gradient "Vibely" wordmark used in the home top bar. */
@Composable
fun VibelyWordmark(modifier: Modifier = Modifier) {
    Text(
        text = "Vibely",
        modifier = modifier,
        style = MaterialTheme.typography.headlineMedium.copy(
            brush = BrandBrush,
            fontWeight = FontWeight.ExtraBold,
            fontStyle = FontStyle.Italic,
            letterSpacing = (-0.5).sp,
        ),
    )
}

@Composable
fun VerifiedBadge(modifier: Modifier = Modifier, size: Dp = 14.dp) {
    Icon(
        imageVector = Icons.Rounded.Verified,
        contentDescription = "Verified",
        tint = MaterialTheme.colorScheme.primary,
        modifier = modifier.size(size),
    )
}

/** Paints any content (icons, text) with the brand gradient. */
fun Modifier.brandTint(brush: Brush = BrandBrush): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        drawRect(brush, blendMode = BlendMode.SrcAtop)
    }

/** Primary call-to-action with the brand gradient. */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(12.dp),
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
    leading: (@Composable () -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (enabled) BrandBrush else Brush.linearGradient(listOf(Color.Gray.copy(alpha = 0.35f), Color.Gray.copy(alpha = 0.35f))))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (leading != null) {
                leading()
                Spacer(Modifier.size(6.dp))
            }
            Text(text = text, color = Color.White, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Secondary, neutral button that matches [GradientButton]'s metrics. */
@Composable
fun TonalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge)
    }
}

/** Follow / Following toggle with an animated colour change. */
@Composable
fun FollowButton(
    isFollowing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    followLabel: String = "Follow",
) {
    val padding = if (compact) PaddingValues(horizontal = 14.dp, vertical = 6.dp) else PaddingValues(horizontal = 20.dp, vertical = 10.dp)
    val shape = RoundedCornerShape(if (compact) 10.dp else 12.dp)
    val borderColor by animateColorAsState(
        if (isFollowing) MaterialTheme.colorScheme.outline else Color.Transparent,
        label = "followBorder",
    )
    Box(
        modifier = modifier
            .clip(shape)
            .then(
                if (isFollowing) {
                    Modifier
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .border(1.dp, borderColor, shape)
                } else {
                    Modifier.background(BrandBrush)
                },
            )
            .clickable(onClick = onClick)
            .padding(padding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (isFollowing) "Following" else followLabel,
            color = if (isFollowing) MaterialTheme.colorScheme.onSurface else Color.White,
            style = if (compact) MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold) else MaterialTheme.typography.labelLarge,
        )
    }
}

/**
 * Big gradient heart that pops and fades over a post after a double tap. Each new [trigger]
 * value replays the animation.
 */
@Composable
fun HeartBurst(trigger: Int, modifier: Modifier = Modifier, size: Dp = 112.dp) {
    val scale = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        alpha.snapTo(1f)
        scale.snapTo(0.2f)
        scale.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessMediumLow))
        delay(220)
        launch { scale.animateTo(1.35f, tween(220)) }
        alpha.animateTo(0f, tween(220))
    }
    Icon(
        imageVector = Icons.Rounded.Favorite,
        contentDescription = null,
        tint = Color.White,
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                this.alpha = alpha.value
            }
            .brandTint(Brush.linearGradient(listOf(Color(0xFFFF4D8D), Color(0xFFFF8A65)))),
    )
}

/** Animated loading placeholder. */
fun Modifier.shimmer(shape: Shape = RoundedCornerShape(8.dp)): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing)),
        label = "shimmerProgress",
    )
    val dark = LocalIsDarkTheme.current
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    val highlight = if (dark) MaterialTheme.colorScheme.surfaceContainerHighest else Color.White
    this
        .clip(shape)
        .drawBehind {
            val width = size.width
            drawRect(
                Brush.linearGradient(
                    colors = listOf(base, highlight, base),
                    start = Offset(width * progress - width, 0f),
                    end = Offset(width * progress, size.height),
                ),
            )
        }
}

/** Friendly empty or error state with an emoji illustration and optional call to action. */
@Composable
fun EmptyState(
    emoji: String,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = emoji, fontSize = 44.sp)
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            GradientButton(text = actionLabel, onClick = onAction)
        }
    }
}

/** Section header used in lists ("Today", "Suggested for you", ...). */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
}
