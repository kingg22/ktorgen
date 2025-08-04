package io.github.kingg22.ktorgen.http

import org.intellij.lang.annotations.Language

/**
 * Make an OPTIONS request
 *
 * ```kotlin
 * @OPTIONS
 * suspend fun getOptions(): HttpResponse
 * ```
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class OPTIONS(
    /**
     * Relative or absolute URL of the endpoint.
     *
     * Is optional, when is empty or blank, evaluate the [Url] annotation, otherwise the base URL is used instead.
     * @see Url
     */
    @Language("http-url-reference")
    val value: String = "",
)
