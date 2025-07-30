package io.github.kingg22.ktorgen.http

/**
 * Add a header to a request
 *
 * ```kotlin
 * @GET("comments")
 * suspend fun request(@Header("Content-Type") name: String): List<Comment>
 *
 * request("Hello World")
 * // Generate header "Content-Type:Hello World"
 * ```
 *
 * Header with null values will be ignored
 * @see Headers
 * @see HeaderMap
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class Header(val value: String)
