package io.github.kingg22.ktorgen.http

/**
 * Add headers to a request
 *
 * ```kotlin
 * @GET("comments")
 * suspend fun requestWithHeaders(@HeaderMap headerMap : Map<String,String>): List<Comment>
 * ```
 *
 * By default, Headers do not overwrite each other:
 * all headers with the same name will be included in the request.
 * Except headers mentioned as _singleton_,
 * e.g. [Content-Type](https://www.rfc-editor.org/rfc/rfc9110.html#name-content-type)
 *
 * @see Header
 * @see Headers
 * @see <a href="https://www.rfc-editor.org/rfc/rfc9110.html">RFC 9110 - HTTP Semantics</a>
 * @see <a href="https://ktor.io/docs/client-requests.html#headers">Ktor Client Request - Headers</a>
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class HeaderMap
