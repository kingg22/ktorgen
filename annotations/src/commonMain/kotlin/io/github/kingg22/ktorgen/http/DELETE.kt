package io.github.kingg22.ktorgen.http

import org.intellij.lang.annotations.Language

/**
 * Make a DELETE request.
 *
 * ```kotlin
 * @DELETE("deleteIssue")
 * suspend fun deleteIssue(@Query("id") id: String)
 * ```
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class DELETE(
    /**
     * Relative or absolute URL of the endpoint.
     *
     * Is optional, when is empty or blank, evaluate the [Url] annotation, otherwise the base URL is used instead.
     * @see io.github.kingg22.ktorgen.http.Url
     */
    @Language("http-url-reference")
    val value: String = "",
)
