package io.github.kingg22.ktorgen.http

/**
 * Add headers to a request
 *
 * Each header must be in the format: `"Header-Name: header-value"`
 *
 * ```kotlin
 * @Headers("Accept: application/json", "Content-Type: application/json")
 * @GET("comments")
 * suspend fun requestWithHeaders(): List<Comment>
 * ```
 *
 * By default, Headers do not overwrite each other:
 * all headers with the same name will be included in the request.
 * Except headers mentioned as _singleton_,
 * e.g. [Content-Type](https://www.rfc-editor.org/rfc/rfc9110.html#name-content-type)
 *
 * @see Header
 * @see HeaderMap
 * @see <a href="https://www.rfc-editor.org/rfc/rfc9110.html">RFC 9110 - HTTP Semantics</a>
 * @see <a href="https://ktor.io/docs/client-requests.html#headers">Ktor Client Request - Headers</a>
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class Headers(
    /** One or more header strings in `"Name: Value"` format. */
    vararg val value: String,
)
