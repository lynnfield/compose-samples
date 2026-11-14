package com.example.jetnews.platform

import android.content.Context
import android.content.Intent
import com.example.jetnews.model.Post
import com.genovich.components.Action

/**
 * Opens the system share sheet for a post.
 */
class SharePost(
    val context: Context
) : Action<Post, Unit>() {

    override suspend fun invoke(input: Post) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, input.title)
            putExtra(Intent.EXTRA_TEXT, input.url)
        }
        context.startActivity(
            Intent.createChooser(intent, "Share post").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
