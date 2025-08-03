package io.github.kingg22.ktorgen.http

/**
 * Adds a single HTTP header whose value is provided at call time.
 *
 * The `value` parameter specifies only the header name.
 * The header value is taken from the annotated function parameter.
 *
 * ```kotlin
 * @GET("comments")
 * suspend fun getComments(@Header("Content-Type") name: String): List<Comment>
 *
 * // Call:
 * getComments("application/json")
 * // Resulting header: "Content-Type: application/json"
 * ```
 *
 * By default, Headers do not overwrite each other:
 * all headers with the same name will be included in the request.
 * Except headers mentioned as _singleton_,
 * e.g. [Content-Type](https://www.rfc-editor.org/rfc/rfc9110.html#name-content-type)
 *
 * @see Headers
 * @see HeaderMap
 * @see <a href="https://www.rfc-editor.org/rfc/rfc9110.html">RFC 9110 - HTTP Semantics</a>
 * @see <a href="https://ktor.io/docs/client-requests.html#headers">Ktor Client Request - Headers</a>
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class Header(
    /** The header name (without the `:` and value part). */
    val value: String,
)
