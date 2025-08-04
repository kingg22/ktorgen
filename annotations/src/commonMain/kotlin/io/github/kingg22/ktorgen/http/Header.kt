package io.github.kingg22.ktorgen.http

import org.intellij.lang.annotations.Language

/**
 * Adds a single HTTP header whose value is provided at call time.
 *
 * The `value` parameter specifies only the header name.
 * The header value is taken from the annotated function parameter.
 *
 * ```kotlin
 * @GET("comments")
 * suspend fun getComments(
 *     @Header(Headers.Authorization) token: String, // type safe
 * ): List<Comment>
 *
 * // Call:
 * getComments(token = "Bearer superSecureToken")
 * // Resulting header: "Authorization: Bearer superSecureToken"
 * ```
 *
 * By default, Headers do not overwrite each other:
 * all headers with the same name will be included in the request.
 * Except headers mentioned as _singleton_,
 * e.g. [Content-Type](https://www.rfc-editor.org/rfc/rfc9110.html#name-content-type)
 *
 * @see Headers
 * @see Headers.Companion
 * @see HeaderMap
 * @see <a href="https://www.rfc-editor.org/rfc/rfc9110.html">RFC 9110 - HTTP Semantics</a>
 * @see <a href="https://ktor.io/docs/client-requests.html#headers">Ktor Client Request - Headers</a>
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class Header(
    /** The header name (without the `:` and value part). */
    @Language("http-header-reference")
    val value: String,
)
