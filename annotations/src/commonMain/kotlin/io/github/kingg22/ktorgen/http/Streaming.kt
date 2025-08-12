package io.github.kingg22.ktorgen.http

/**
 * **Deprecated, not needed**
 *
 * **Use the type HttpStatement, coroutines Flow or Kotlin Result directly.**
 *
 * Treat the response body on methods returning HttpStatement
 *
 * ```kotlin
 *  @Streaming
 *  @GET("posts")
 *  suspend fun getPostsAsStreaming(): HttpStatement
 * ```
 */
@Deprecated(
    "In KtorGen, this annotation is no longer necessary." +
        "The type HttpStatement, coroutines Flow or Kotlin Result have 'smart use', don't need to be marked with annotation to works",
    level = DeprecationLevel.ERROR,
)
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class Streaming
