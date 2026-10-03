package io.github.ieswar23.vibely.domain

import io.github.ieswar23.vibely.domain.model.PollDraft
import io.github.ieswar23.vibely.domain.model.PollDuration
import io.github.ieswar23.vibely.domain.model.PostDraft
import io.github.ieswar23.vibely.domain.model.PostType
import io.github.ieswar23.vibely.util.TextTokenParser

data class DraftValidation(
    val captionError: String? = null,
    val headlineError: String? = null,
    val locationError: String? = null,
    val mediaError: String? = null,
    val pollQuestionError: String? = null,
    /** One entry per poll option (null when that option is fine); empty for non-poll drafts. */
    val pollOptionErrors: List<String?> = emptyList(),
) {
    val isValid: Boolean
        get() = captionError == null && headlineError == null && locationError == null && mediaError == null &&
            pollQuestionError == null && pollOptionErrors.all { it == null }
}

/** Business rules for publishing a post. Pure Kotlin so it is exhaustively unit tested. */
object PostDraftValidator {
    const val MAX_CAPTION_LENGTH = 2_200
    const val MAX_HASHTAGS = 30
    const val MAX_HEADLINE_LENGTH = 40
    const val MAX_LOCATION_LENGTH = 60
    const val MIN_TEXT_POST_LENGTH = 3
    const val MAX_POLL_QUESTION_LENGTH = 140
    const val MAX_POLL_OPTION_LENGTH = 40
    const val MIN_POLL_OPTIONS = 2
    const val MAX_POLL_OPTIONS = 4

    private val EMPTY_POLL = PollDraft(question = "", options = emptyList(), duration = PollDuration.ONE_DAY)

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
        // A poll post without poll data is validated as an empty poll so every field gets flagged.
        val poll = if (draft.type == PostType.POLL) draft.poll ?: EMPTY_POLL else null
        return DraftValidation(
            captionError = captionError,
            headlineError = headlineError,
            locationError = locationError,
            mediaError = mediaError,
            pollQuestionError = poll?.let { questionError(it.question) },
            pollOptionErrors = poll?.let { optionErrors(it.options) }.orEmpty(),
        )
    }

    private fun questionError(question: String): String? {
        val text = question.trim()
        return when {
            text.isEmpty() -> "Ask a question"
            text.length > MAX_POLL_QUESTION_LENGTH -> "Questions can be up to $MAX_POLL_QUESTION_LENGTH characters"
            else -> null
        }
    }

    /**
     * Blank, too-long and duplicate (case-insensitive) options are flagged individually so the composer
     * can point at the exact field. A duplicate is reported on the later copy only. With fewer than
     * [MIN_POLL_OPTIONS] fields the missing ones are reported as blank, and fields beyond
     * [MAX_POLL_OPTIONS] are rejected.
     */
    private fun optionErrors(options: List<String>): List<String?> {
        val fields = options + List((MIN_POLL_OPTIONS - options.size).coerceAtLeast(0)) { "" }
        val seen = mutableSetOf<String>()
        return fields.mapIndexed { index, raw ->
            val text = raw.trim()
            val key = text.lowercase()
            when {
                index >= MAX_POLL_OPTIONS -> "Polls can have up to $MAX_POLL_OPTIONS options"
                text.isEmpty() -> "Add an option"
                text.length > MAX_POLL_OPTION_LENGTH -> "Up to $MAX_POLL_OPTION_LENGTH characters"
                !seen.add(key) -> "Options must be different"
                else -> null
            }
        }
    }
}
