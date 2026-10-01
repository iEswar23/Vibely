package io.github.ieswar23.vibely.ui.story

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ieswar23.vibely.domain.model.Story
import io.github.ieswar23.vibely.ui.common.LocalNow
import io.github.ieswar23.vibely.ui.common.PostCanvas
import io.github.ieswar23.vibely.ui.common.UserAvatar
import io.github.ieswar23.vibely.ui.common.VerifiedBadge
import io.github.ieswar23.vibely.util.TimeAgo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val FRAME_DURATION_MS = 5_000
private const val DISMISS_THRESHOLD_PX = 280f

/**
 * Full-screen story viewer: segmented progress bars that auto-advance, tap left/right to navigate,
 * press and hold to pause, swipe down to close.
 */
@Composable
fun StoryViewerScreen(
    onClose: () -> Unit,
    onOpenProfile: (String) -> Unit,
    viewModel: StoryViewerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var closed by remember { mutableStateOf(false) }
    val close = {
        if (!closed) {
            closed = true
            onClose()
        }
    }

    LaunchedEffect(state.isFinished) {
        if (state.isFinished) close()
    }
    BackHandler(onBack = close)
    LightSystemBarIcons()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        val story = state.currentStory
        if (state.isLoading || story == null) {
            CircularProgressIndicator(color = Color.White)
        } else {
            StoryPage(
                story = story,
                frameIndex = state.frameIndex,
                restartToken = state.restartToken,
                onNext = viewModel::next,
                onPrevious = viewModel::previous,
                onClose = close,
                onOpenProfile = { onOpenProfile(story.user.id) },
            )
        }
    }
}

@Composable
private fun StoryPage(
    story: Story,
    frameIndex: Int,
    restartToken: Int,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onClose: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var paused by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val settleOffset = remember { Animatable(0f) }
    val progress = remember(story.id, frameIndex, restartToken) { Animatable(0f) }
    // Only treat a press as a "hold" after a short delay so quick taps don't flash the UI.
    var holding by remember { mutableStateOf(false) }
    LaunchedEffect(paused) {
        holding = false
        if (paused) {
            delay(180)
            holding = true
        }
    }
    val chromeAlpha by animateFloatAsState(if (holding && dragOffset == 0f) 0f else 1f, tween(200), label = "chrome")
    val now = LocalNow.current

    // Auto-advance. Pausing cancels the animation; resuming continues from the current value.
    LaunchedEffect(progress, paused) {
        if (paused) return@LaunchedEffect
        val remaining = ((1f - progress.value) * FRAME_DURATION_MS).toInt()
        progress.animateTo(1f, tween(durationMillis = remaining, easing = LinearEasing))
        onNext()
    }

    val offset = dragOffset + settleOffset.value
    val dismissProgress = (offset / (DISMISS_THRESHOLD_PX * 2)).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationY = offset
                val scale = 1f - dismissProgress * 0.12f
                scaleX = scale
                scaleY = scale
                alpha = 1f - dismissProgress * 0.4f
            }
            .clip(RoundedCornerShape(if (offset > 0f) 24.dp else 0.dp))
            .pointerInput(story.id) {
                detectTapGestures(
                    onPress = {
                        paused = true
                        tryAwaitRelease()
                        paused = false
                    },
                    onLongPress = { /* Holding keeps the story paused; release resumes. */ },
                    onTap = { position ->
                        if (position.x < size.width / 3f) onPrevious() else onNext()
                    },
                )
            }
            .pointerInput(story.id) {
                detectVerticalDragGestures(
                    onDragStart = { paused = true },
                    onVerticalDrag = { change, amount ->
                        change.consume()
                        dragOffset = (dragOffset + amount).coerceAtLeast(0f)
                    },
                    onDragEnd = {
                        if (dragOffset > DISMISS_THRESHOLD_PX) {
                            onClose()
                        } else {
                            val start = dragOffset
                            dragOffset = 0f
                            scope.launch {
                                settleOffset.snapTo(start)
                                settleOffset.animateTo(0f, tween(220))
                            }
                            paused = false
                        }
                    },
                    onDragCancel = {
                        dragOffset = 0f
                        paused = false
                    },
                )
            },
    ) {
        AnimatedContent(
            targetState = story.frames[frameIndex.coerceIn(0, story.frames.lastIndex)],
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(220)) },
            label = "storyFrame",
            modifier = Modifier.fillMaxSize(),
        ) { frame ->
            Box(Modifier.fillMaxSize()) {
                PostCanvas(
                    gradientKey = frame.gradientKey,
                    emoji = frame.emoji,
                    overlayText = null,
                    showOverlay = false,
                    emojiSize = 150.sp,
                    modifier = Modifier.fillMaxSize(),
                )
                if (frame.text.isNotBlank()) {
                    Text(
                        text = frame.text,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        style = TextStyle(
                            fontSize = 30.sp,
                            fontWeight = FontWeight.ExtraBold,
                            shadow = Shadow(Color.Black.copy(alpha = 0.35f), blurRadius = 12f),
                        ),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(horizontal = 32.dp, vertical = 96.dp),
                    )
                }
            }
        }

        // Top chrome: progress segments + author row.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = chromeAlpha }
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.35f), Color.Transparent),
                    ),
                )
                .statusBarsPadding()
                .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                story.frames.indices.forEach { index ->
                    val fill = when {
                        index < frameIndex -> 1f
                        index == frameIndex -> progress.value
                        else -> 0f
                    }
                    ProgressSegment(fill = fill, modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                UserAvatar(name = story.user.name, seed = story.user.id, size = 34.dp, onClick = onOpenProfile)
                Spacer(Modifier.width(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onOpenProfile),
                ) {
                    Text(
                        text = story.user.username,
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    if (story.user.isVerified) {
                        Spacer(Modifier.width(4.dp))
                        VerifiedBadge(size = 13.dp)
                    }
                    Text(
                        text = "  ${TimeAgo.short(story.createdAt, now)}",
                        color = Color.White.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close story", tint = Color.White)
                }
            }
        }

        if (holding && dragOffset == 0f) {
            Icon(
                Icons.Rounded.Pause,
                contentDescription = "Paused",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 24.dp),
            )
        }
    }
}

@Composable
private fun ProgressSegment(fill: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(3.dp)
            .drawBehind {
                val radius = CornerRadius(size.height / 2, size.height / 2)
                drawRoundRect(Color.White.copy(alpha = 0.35f), cornerRadius = radius)
                drawRoundRect(
                    Color.White,
                    size = size.copy(width = size.width * fill.coerceIn(0f, 1f)),
                    cornerRadius = radius,
                )
            },
    )
}

/** Stories are always shown on a dark canvas, so force light status bar icons while visible. */
@Composable
private fun LightSystemBarIcons() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = view.context.findActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val previous = controller?.isAppearanceLightStatusBars
        controller?.isAppearanceLightStatusBars = false
        onDispose {
            if (controller != null && previous != null) controller.isAppearanceLightStatusBars = previous
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
