package io.github.ieswar23.vibely.ui.common

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import io.github.ieswar23.vibely.domain.model.Post
import io.github.ieswar23.vibely.domain.model.User

/** Opens the Android share sheet with a text summary of [post]. */
fun sharePost(context: Context, post: Post) {
    val text = buildString {
        append("Check out @${post.author.username}'s post on Vibely")
        if (post.caption.isNotBlank()) append(":\n\n").append(post.caption)
        append("\n\nhttps://vibely.app/p/${post.id}")
    }
    shareText(context, text, "Share post")
}

fun shareProfile(context: Context, user: User) {
    shareText(context, "Follow ${user.name} (@${user.username}) on Vibely\nhttps://vibely.app/${user.username}", "Share profile")
}

private fun shareText(context: Context, text: String, title: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(send, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}
