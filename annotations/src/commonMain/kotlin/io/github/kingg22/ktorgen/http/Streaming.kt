package io.github.kingg22.ktorgen.http

import io.github.kingg22.ktorgen.core.KtorGenExperimental

/**
 * Treat the response body on methods returning HttpStatement
 *
 * ```kotlin
 *  @Streaming
 *  @GET("posts")
 *  suspend fun getPostsAsStreaming(): HttpStatement
 * ```
 */
@KtorGenExperimental // Because currently is not validated, but can use return type
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class Streaming
// my idea is provided a useful trick with flow and not suspend methods
