package io.github.ieswar23.vibely.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.ieswar23.vibely.ui.theme.CanvasGradients
import io.github.ieswar23.vibely.ui.theme.StoryRingBrush

enum class StoryRing { NONE, UNSEEN, SEEN }

/** "Priya Sharma" -> "PS", "aarav" -> "A". */
fun initialsOf(name: String): String =
    name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.take(2)
        .joinToString("") { it.first().uppercase() }
        .ifEmpty { "?" }

/**
 * Initials avatar on a deterministic gradient. With [ring] it renders the story ring:
 * the brand gradient for unseen stories and a subtle grey ring once watched.
 */
@Composable
fun UserAvatar(
    name: String,
    seed: String,
    size: Dp,
    modifier: Modifier = Modifier,
    ring: StoryRing = StoryRing.NONE,
    onClick: (() -> Unit)? = null,
) {
    val gradient = remember(seed) { CanvasGradients.forSeed(seed) }
    val initials = remember(name) { initialsOf(name) }
    val fontSize = with(LocalDensity.current) { (size * 0.36f).toSp() }
    val ringWidth = if (size >= 72.dp) 3.dp else 2.dp
    val clickable = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .then(clickable)
            .semantics { contentDescription = "$name avatar" },
        contentAlignment = Alignment.Center,
    ) {
        val inner = when (ring) {
            StoryRing.NONE -> Modifier
            StoryRing.UNSEEN -> Modifier.border(ringWidth, StoryRingBrush, CircleShape).padding(ringWidth + 2.dp)
            StoryRing.SEEN -> Modifier
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                .padding(ringWidth + 2.dp)
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(inner)
                .clip(CircleShape)
                .background(gradient.brush),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initials,
                color = Color.White,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
