package io.github.ieswar23.vibely.util

/** A slice of user-generated text: plain text, a #hashtag or an @mention. */
sealed interface TextToken {
    val text: String

    data class Plain(override val text: String) : TextToken

    /** [tag] is the normalised (lower-case, no '#') hashtag. */
    data class Hashtag(override val text: String, val tag: String) : TextToken

    /** [username] is the mentioned handle without the leading '@'. */
    data class Mention(override val text: String, val username: String) : TextToken
}

/**
 * Splits captions and comments into tokens so the UI can style and link hashtags and mentions.
 *
 * Rules:
 *  - a token must start the text or follow a non-word character (so `mail@site.com` is not a mention);
 *  - hashtags are letters, digits and underscores and must contain at least one letter;
 *  - mentions may contain dots (`@priya.wanders`) but never end with one.
 */
object TextTokenParser {

    private val pattern = Regex(
        "(?<![\\w@#])#(\\w*[A-Za-z]\\w*)|(?<![\\w@#.])@([A-Za-z0-9_](?:[A-Za-z0-9_.]*[A-Za-z0-9_])?)",
    )

    fun parse(text: String): List<TextToken> {
        if (text.isEmpty()) return emptyList()
        val tokens = mutableListOf<TextToken>()
        var cursor = 0
        for (match in pattern.findAll(text)) {
            if (match.range.first > cursor) {
                tokens += TextToken.Plain(text.substring(cursor, match.range.first))
            }
            val hashtag = match.groups[1]?.value
            tokens += if (hashtag != null) {
                TextToken.Hashtag(match.value, hashtag.lowercase())
            } else {
                TextToken.Mention(match.value, match.groups[2]!!.value)
            }
            cursor = match.range.last + 1
        }
        if (cursor < text.length) tokens += TextToken.Plain(text.substring(cursor))
        return tokens
    }

    /** Distinct, lower-cased hashtags in order of first appearance. */
    fun hashtags(text: String): List<String> =
        parse(text).filterIsInstance<TextToken.Hashtag>().map { it.tag }.distinct()

    /** Distinct mentioned usernames in order of first appearance. */
    fun mentions(text: String): List<String> =
        parse(text).filterIsInstance<TextToken.Mention>().map { it.username }.distinct()
}
