package io.github.kingg22.ktorgen.http

import org.intellij.lang.annotations.Language

/** Make a GET request.
 *
 * ```kotlin
 * @GET("issue")
 * suspend fun getIssue(@Query("id") id: String): Issue
 * ```
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class GET(
    /**
     * Relative or absolute URL of the endpoint.
     *
     * Is optional, when is empty or blank, evaluate the [Url] annotation, otherwise the base URL is used instead.
     * @see io.github.kingg22.ktorgen.http.Url
     */
    @Language("http-url-reference")
    val value: String = "",
)
