package io.github.ieswar23.vibely.domain

import io.github.ieswar23.vibely.domain.model.PostDraft
import io.github.ieswar23.vibely.domain.model.PostType
import io.github.ieswar23.vibely.util.TextTokenParser

data class DraftValidation(
    val captionError: String? = null,
    val headlineError: String? = null,
    val locationError: String? = null,
    val mediaError: String? = null,
) {
    val isValid: Boolean
        get() = captionError == null && headlineError == null && locationError == null && mediaError == null
}

/** Business rules for publishing a post. Pure Kotlin so it is exhaustively unit tested. */
object PostDraftValidator {
    const val MAX_CAPTION_LENGTH = 2_200
    const val MAX_HASHTAGS = 30
    const val MAX_HEADLINE_LENGTH = 40
    const val MAX_LOCATION_LENGTH = 60
    const val MIN_TEXT_POST_LENGTH = 3

    fun validate(draft: PostDraft): DraftValidation {
        val caption = draft.caption.trim()
        val hashtagCount = TextTokenParser.hashtags(caption).size

        val captionError = when {
            draft.type == PostType.TEXT && caption.isEmpty() -> "Write something to share"
            draft.type == PostType.TEXT && caption.length < MIN_TEXT_POST_LENGTH ->
                "Text posts need at least $MIN_TEXT_POST_LENGTH characters"
            caption.length > MAX_CAPTION_LENGTH -> "Captions can be up to 2,200 characters"
            hashtagCount > MAX_HASHTAGS -> "Use up to $MAX_HASHTAGS hashtags"
            else -> null
        }
        val headlineError = when {
            draft.type != PostType.CANVAS -> null
            (draft.overlayText?.trim()?.length ?: 0) > MAX_HEADLINE_LENGTH ->
                "Headlines can be up to $MAX_HEADLINE_LENGTH characters"
            else -> null
        }
        val locationError = if ((draft.location?.trim()?.length ?: 0) > MAX_LOCATION_LENGTH) {
            "Locations can be up to $MAX_LOCATION_LENGTH characters"
        } else {
            null
        }
        val mediaError = if (draft.type == PostType.CANVAS && (draft.emoji.isNullOrBlank() || draft.gradientKey.isNullOrBlank())) {
            "Pick a background and an emoji"
        } else {
            null
        }
        return DraftValidation(captionError, headlineError, locationError, mediaError)
    }
}
