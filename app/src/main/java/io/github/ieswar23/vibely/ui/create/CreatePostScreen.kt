package io.github.ieswar23.vibely.ui.create

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ieswar23.vibely.domain.PostDraftValidator
import io.github.ieswar23.vibely.domain.model.PostType
import io.github.ieswar23.vibely.ui.common.GradientButton
import io.github.ieswar23.vibely.ui.common.PostCanvas
import io.github.ieswar23.vibely.ui.common.rememberRichText
import io.github.ieswar23.vibely.ui.theme.CanvasGradients
import io.github.ieswar23.vibely.util.CountFormatter

private val EmojiChoices = listOf(
    "✨", "🌅", "🏔️", "🏝️", "🌸", "🌧️", "🌙", "🍜", "☕", "🍰", "🥗", "🍕",
    "🏋️", "🏃", "🧘", "🚴", "💻", "📱", "🎧", "🎸", "🎶", "🏏", "🏆", "🎉",
    "❤️", "🔥", "🐾", "📚", "🎨", "✈️",
)

private val LocationSuggestions = listOf("Bengaluru, India", "Mumbai, India", "Goa, India", "New Delhi, India", "Pune, India", "London, UK")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    onClose: () -> Unit,
    onPublished: () -> Unit,
    viewModel: CreatePostViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                CreatePostEvent.Published -> onPublished()
                is CreatePostEvent.Error -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New post") },
                navigationIcon = {
                    IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, contentDescription = "Discard") }
                },
                actions = {
                    Box(Modifier.padding(end = 12.dp), contentAlignment = Alignment.Center) {
                        if (state.isPublishing) {
                            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.5.dp)
                        } else {
                            GradientButton(
                                text = "Share",
                                onClick = viewModel::publish,
                                enabled = state.canPublish,
                                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                            )
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                SegmentedButton(
                    selected = state.type == PostType.CANVAS,
                    onClick = { viewModel.onTypeChange(PostType.CANVAS) },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                    icon = { Icon(Icons.Rounded.Palette, contentDescription = null, modifier = Modifier.size(18.dp)) },
                ) { Text("Canvas") }
                SegmentedButton(
                    selected = state.type == PostType.TEXT,
                    onClick = { viewModel.onTypeChange(PostType.TEXT) },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                    icon = { Icon(Icons.Rounded.TextFields, contentDescription = null, modifier = Modifier.size(18.dp)) },
                ) { Text("Text") }
            }

            PreviewCard(state = state)

            AnimatedVisibility(
                visible = state.type == PostType.CANVAS,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column {
                    FieldLabel("Background")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(CanvasGradients.all, key = { it.key }) { gradient ->
                            val selected = gradient.key == state.gradientKey
                            val ring by animateDpAsState(if (selected) 3.dp else 0.dp, label = "ring")
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .border(ring, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    .padding(if (selected) 5.dp else 0.dp)
                                    .clip(CircleShape)
                                    .background(gradient.brush)
                                    .clickable { viewModel.onGradientSelected(gradient.key) },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (selected) Icon(Icons.Rounded.Check, contentDescription = gradient.label, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    FieldLabel("Emoji")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(EmojiChoices) { emoji ->
                            val selected = emoji == state.emoji
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                    )
                                    .border(
                                        width = if (selected) 2.dp else 0.dp,
                                        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = RoundedCornerShape(14.dp),
                                    )
                                    .clickable { viewModel.onEmojiSelected(emoji) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(emoji, fontSize = 24.sp)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = state.headline,
                        onValueChange = viewModel::onHeadlineChange,
                        label = { Text("Headline (optional)") },
                        singleLine = true,
                        isError = state.showErrors && state.validation.headlineError != null,
                        supportingText = {
                            val error = state.validation.headlineError
                            Text(if (state.showErrors && error != null) error else "${state.headline.trim().length}/${PostDraftValidator.MAX_HEADLINE_LENGTH}")
                        },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 16.dp),
                    )
                }
            }

            OutlinedTextField(
                value = state.caption,
                onValueChange = viewModel::onCaptionChange,
                label = { Text(if (state.type == PostType.TEXT) "What's on your mind?" else "Write a caption…") },
                minLines = 3,
                maxLines = 8,
                isError = state.showErrors && state.validation.captionError != null,
                supportingText = {
                    val error = state.validation.captionError
                    if (state.showErrors && error != null) {
                        Text(error)
                    } else {
                        Text(
                            "${CountFormatter.grouped(state.captionLength)}/${CountFormatter.grouped(PostDraftValidator.MAX_CAPTION_LENGTH)}" +
                                "  •  ${state.hashtagCount} hashtag${if (state.hashtagCount == 1) "" else "s"}",
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp),
            )

            OutlinedTextField(
                value = state.location,
                onValueChange = viewModel::onLocationChange,
                label = { Text("Add location") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.LocationOn, contentDescription = null) },
                isError = state.showErrors && state.validation.locationError != null,
                supportingText = state.validation.locationError?.takeIf { state.showErrors }?.let { error -> { Text(error) } },
                trailingIcon = if (state.location.isNotEmpty()) {
                    { IconButton(onClick = { viewModel.onLocationChange("") }) { Icon(Icons.Rounded.Close, contentDescription = "Clear location") } }
                } else {
                    null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 4.dp),
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(LocationSuggestions) { suggestion ->
                    AssistChip(onClick = { viewModel.onLocationChange(suggestion) }, label = { Text(suggestion) })
                }
            }

            state.validation.mediaError?.takeIf { state.showErrors }?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}

@Composable
private fun PreviewCard(state: CreatePostUiState) {
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .animateContentSize(),
    ) {
        Crossfade(targetState = state.type, label = "previewType") { type ->
            when (type) {
                PostType.CANVAS -> Crossfade(targetState = state.gradientKey, label = "previewGradient") { key ->
                    PostCanvas(
                        gradientKey = key,
                        emoji = state.emoji,
                        overlayText = state.headline.ifBlank { null },
                        emojiSize = 96.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.25f),
                    )
                }
                PostType.TEXT -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(24.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (state.caption.isBlank()) {
                        Text(
                            text = "Your words, front and centre.",
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            text = rememberRichText(state.caption, {}, {}),
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, lineHeight = 26.sp, fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
    )
}
