package io.github.kingg22.ktorgen.http

/**
 * Add headers to a request
 *
 * ```kotlin
 * @GET("comments")
 * suspend fun requestWithHeaders(@HeaderMap headerMap : Map<String,String>): List<Comment>
 * ```
 *
 * @see Headers
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class HeaderMap
