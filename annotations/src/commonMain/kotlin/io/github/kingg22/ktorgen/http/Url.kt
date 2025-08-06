package io.github.kingg22.ktorgen.http

/**
 * Set URL of the request
 *
 * Can be type of: `String`, Ktor `Url`, Ktor `UrlBuilder`. More detail, see [Url.takeFrom](https://api.ktor.io/ktor-http/io.ktor.http/take-from.html)
 *
 * ```kotlin
 * @GET
 * suspend fun request(@Url url: String): List<Comment>
 *
 * @POST
 * suspend fun request(@Url url: Url): Comment
 *
 * @PUT
 * suspend fun request(@Url builder: UrlBuilder): Comment
 * ```
 *
 * @see <a href="https://ktor.io/docs/client-requests.html#url">Ktor Client Request - URL</a>
 * @see <a href="https://ktor.io/docs/client-default-request.html#url">Ktor Client - Default Request Plugin</a>
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class Url
