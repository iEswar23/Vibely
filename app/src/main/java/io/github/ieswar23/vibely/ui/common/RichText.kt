package io.github.ieswar23.vibely.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withLink
import io.github.ieswar23.vibely.util.TextToken
import io.github.ieswar23.vibely.util.TextTokenParser

/**
 * Appends [text] with #hashtags and @mentions styled in the brand colour and made clickable via
 * [LinkAnnotation.Clickable]. Parsing is delegated to the unit-tested [TextTokenParser].
 */
fun AnnotatedString.Builder.appendRichText(
    text: String,
    linkStyle: SpanStyle,
    onHashtagClick: (String) -> Unit,
    onMentionClick: (String) -> Unit,
) {
    TextTokenParser.parse(text).forEach { token ->
        when (token) {
            is TextToken.Plain -> append(token.text)
            is TextToken.Hashtag -> withLink(
                LinkAnnotation.Clickable(
                    tag = "#${token.tag}",
                    styles = TextLinkStyles(style = linkStyle),
                    linkInteractionListener = { onHashtagClick(token.tag) },
                ),
            ) { append(token.text) }
            is TextToken.Mention -> withLink(
                LinkAnnotation.Clickable(
                    tag = "@${token.username}",
                    styles = TextLinkStyles(style = linkStyle),
                    linkInteractionListener = { onMentionClick(token.username) },
                ),
            ) { append(token.text) }
        }
    }
}

/** Builds "**username** caption #tags @mentions" with a clickable, bold username prefix. */
@Composable
fun rememberCaption(
    username: String?,
    text: String,
    onUsernameClick: () -> Unit,
    onHashtagClick: (String) -> Unit,
    onMentionClick: (String) -> Unit,
): AnnotatedString {
    val linkColor = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    return remember(username, text, linkColor, onSurface) {
        buildAnnotatedString {
            if (username != null) {
                withLink(
                    LinkAnnotation.Clickable(
                        tag = "user",
                        styles = TextLinkStyles(style = SpanStyle(fontWeight = FontWeight.SemiBold, color = onSurface)),
                        linkInteractionListener = { onUsernameClick() },
                    ),
                ) { append(username) }
                append("  ")
            }
            appendRichText(
                text = text,
                linkStyle = SpanStyle(color = linkColor, fontWeight = FontWeight.Medium),
                onHashtagClick = onHashtagClick,
                onMentionClick = onMentionClick,
            )
        }
    }
}

/** Plain helper for places that only need styling, without a username prefix. */
@Composable
fun rememberRichText(
    text: String,
    onHashtagClick: (String) -> Unit,
    onMentionClick: (String) -> Unit,
): AnnotatedString = rememberCaption(null, text, {}, onHashtagClick, onMentionClick)
