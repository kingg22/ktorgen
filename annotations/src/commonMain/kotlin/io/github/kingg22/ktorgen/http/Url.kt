package io.github.kingg22.ktorgen.http

/**
 * URL resolved by Ktor.
 * See [Ktor Client - base url](https://ktor.io/docs/client-default-request.html#url) and
 * [Ktor - Url](https://api.ktor.io/ktor-http/io.ktor.http/-url.html)
 *
 * ```kotlin
 * @GET
 * suspend fun request(@Url url: String): List<Comment>
 * ```
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class Url
