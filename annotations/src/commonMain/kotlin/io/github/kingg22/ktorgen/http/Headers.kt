package io.github.kingg22.ktorgen.http

/**
 * Add headers to a request
 *
 * ```kotlin
 * @Headers("Accept: application/json","Content-Type: application/json")
 * @GET("comments")
 * suspend fun requestWithHeaders(): List<Comment>
 * ```
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class Headers(vararg val value: String)
