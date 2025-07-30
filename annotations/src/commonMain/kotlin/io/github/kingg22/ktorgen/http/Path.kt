@file:OptIn(InternalKtorGen::class)

package io.github.kingg22.ktorgen.http

import io.github.kingg22.ktorgen.core.InternalKtorGen
import io.github.kingg22.ktorgen.core.KTORGEN_DEFAULT_NAME

/**
 * Named replacement in a URL path segment
 *
 * This can be set if you have a segment in your URL that want to dynamically replace
 *
 * Path parameters type **may not be nullable**.
 *
 * ```kotlin
 * @GET("post/{postId}")
 * suspend fun getPosts(@Path("postId") id: Int): List<Post>
 *
 * foo.getPosts(25)
 * // Generate post/25
 * ```
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class Path(
    /** Name of the placeholder to replace. Default the name of the _function parameter_ */
    val value: String = KTORGEN_DEFAULT_NAME,
    /** Specifies whether the argument value to the annotated method parameter is already URL encoded. */
    val encoded: Boolean = false,
)
