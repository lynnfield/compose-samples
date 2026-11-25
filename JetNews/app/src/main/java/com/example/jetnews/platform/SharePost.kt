package com.example.jetnews.platform

import android.content.Intent
import com.example.jetnews.model.Post
import com.genovich.components.Action

/**
 * Opens the system share sheet for a post and returns when the share sheet closes.
 */
class SharePost(
    val shareSheet: ActivityResultPort<Intent>
) : Action<Post, Unit>() {

    override suspend fun invoke(input: Post) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, input.title)
            putExtra(Intent.EXTRA_TEXT, input.url)
        }
        shareSheet.launchAndAwait(Intent.createChooser(intent, "Share post"))
    }
}
