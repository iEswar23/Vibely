package io.github.ieswar23.vibely.ui.common

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Poll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ieswar23.vibely.domain.PollOptionResult
import io.github.ieswar23.vibely.domain.PollTally
import io.github.ieswar23.vibely.domain.model.Poll
import io.github.ieswar23.vibely.ui.theme.BrandBrush

private val OptionHeight = 44.dp
private val OptionCorner = 12.dp
private val OptionShape = RoundedCornerShape(OptionCorner)

/**
 * A poll attached to a post. Before you vote the options are tappable; once you've voted (or the
 * poll has closed) they turn into animated result bars with your choice ticked.
 *
 * @param onVote called with the option index; null renders a non-interactive preview (Create screen).
 */
@Composable
fun PollCard(
    poll: Poll,
    onVote: ((Int) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val now = LocalNow.current
    val state = remember(poll, now) { PollTally.state(poll, now) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp, vertical = 18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.Poll,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Poll",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = poll.question,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, lineHeight = 25.sp, fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(14.dp))

        Crossfade(targetState = state.showResults, label = "pollMode") { showResults ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.options.forEachIndexed { index, option ->
                    if (showResults) {
                        PollResultRow(option = option)
                    } else {
                        PollVoteButton(text = option.text, onClick = onVote?.let { vote -> { vote(index) } })
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text = state.summary,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PollVoteButton(text: String, onClick: (() -> Unit)?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = OptionHeight)
            .clip(OptionShape)
            .border(1.5.dp, MaterialTheme.colorScheme.primary, OptionShape)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PollResultRow(option: PollOptionResult) {
    val fill = remember { Animatable(0f) }
    LaunchedEffect(option.percent) {
        fill.animateTo(option.percent / 100f, tween(durationMillis = 700, easing = FastOutSlowInEasing))
    }
    val emphasised = option.isUserChoice || option.isLeading
    val description = buildString {
        append("${option.text}, ${option.percent} percent")
        if (option.isUserChoice) append(", your vote")
    }
    val leadingColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    val otherColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = OptionHeight)
            .clip(OptionShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            // The bar is drawn behind the row so it always matches the row's height, even for two-line options.
            .drawBehind {
                val barSize = Size(size.width * fill.value, size.height)
                val radius = CornerRadius(OptionCorner.toPx())
                when {
                    option.isUserChoice -> drawRoundRect(BrandBrush, size = barSize, cornerRadius = radius, alpha = 0.32f)
                    option.isLeading -> drawRoundRect(leadingColor, size = barSize, cornerRadius = radius)
                    else -> drawRoundRect(otherColor, size = barSize, cornerRadius = radius)
                }
            }
            .semantics(mergeDescendants = true) { contentDescription = description },
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = option.text,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (emphasised) FontWeight.SemiBold else FontWeight.Normal),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (option.isUserChoice) {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = "${option.percent}%",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = if (emphasised) FontWeight.Bold else FontWeight.Medium),
                color = if (emphasised) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
