package com.example.jetnews.testing

import com.example.jetnews.model.Metadata
import com.example.jetnews.model.Post
import com.example.jetnews.model.PostAuthor

/**
 * A minimal post for JVM tests (no images, no resources).
 */
fun testPost(id: String) = Post(
    id = id,
    title = "Title $id",
    url = "https://example.com/$id",
    metadata = Metadata(PostAuthor("Author"), "July 30", 3),
    imageId = 0,
    imageThumbId = 0
)

val testPosts = List(10) { index -> testPost("post$index") }
