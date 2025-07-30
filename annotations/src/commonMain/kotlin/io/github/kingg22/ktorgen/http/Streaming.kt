package io.github.kingg22.ktorgen.http

/**
 * Treat the response body on methods returning HttpStatement
 *
 * ```kotlin
 *  @Streaming
 *  @GET("posts")
 *  suspend fun getPostsAsStreaming(): HttpStatement
 * ```
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class Streaming
