package io.github.kingg22.ktorgen.http

/**
 * Make a PUT request
 *
 * ```kotlin
 * @PUT("putIssue")
 * suspend fun putIssue(@Body issue: Issue)
 * ```
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class PUT(
    /**
     * Relative or absolute URL of the endpoint.
     *
     * Is optional, when is empty or blank, evaluate the [Url] annotation, otherwise the base URL is used instead.
     * @see Url
     */
    val value: String = "",
)
