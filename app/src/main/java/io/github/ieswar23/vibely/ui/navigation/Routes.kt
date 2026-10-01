package io.github.ieswar23.vibely.ui.navigation

import android.net.Uri

object Routes {
    const val HOME = "home"
    const val EXPLORE = "explore"
    const val CREATE = "create"
    const val ACTIVITY = "activity"
    const val MY_PROFILE = "me"

    /** Profile keys understood by ProfileViewModel in addition to user ids and "@username". */
    const val MY_PROFILE_KEY = "me"
    const val MY_PROFILE_SAVED_KEY = "me-saved"

    const val ARG_USER_KEY = "userKey"
    const val ARG_POST_ID = "postId"
    const val ARG_USER_ID = "userId"
    const val ARG_TAG = "tag"

    const val PROFILE = "profile/{$ARG_USER_KEY}"
    const val POST = "post/{$ARG_POST_ID}"
    const val COMMENTS = "comments/{$ARG_POST_ID}"
    const val STORY = "story/{$ARG_USER_ID}"
    const val HASHTAG = "tag/{$ARG_TAG}"
    const val SETTINGS = "settings"
    const val EDIT_PROFILE = "edit_profile"

    /** Accepts a user id or "@username" (used for mentions). */
    fun profile(userKey: String) = "profile/${Uri.encode(userKey)}"
    fun post(postId: String) = "post/${Uri.encode(postId)}"
    fun comments(postId: String) = "comments/${Uri.encode(postId)}"
    fun story(userId: String) = "story/${Uri.encode(userId)}"
    fun hashtag(tag: String) = "tag/${Uri.encode(tag.removePrefix("#"))}"

    /** Screens that take the whole window and hide the bottom navigation. */
    val fullScreenRoutes = setOf(CREATE, COMMENTS, STORY, SETTINGS, EDIT_PROFILE)
}
