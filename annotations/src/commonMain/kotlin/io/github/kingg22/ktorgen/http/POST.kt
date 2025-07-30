package io.github.kingg22.ktorgen.http

/**
 * Make a POST request
 *
 * ```kotlin
 * @POST("issue")
 * suspend fun postIssue(@Body issue: Issue)
 * ```
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class POST(
    /**
     * Relative or absolute URL of the endpoint.
     *
     * Is optional, when is empty or blank, evaluate the [Url] annotation, otherwise the base URL is used instead.
     * @see io.github.kingg22.ktorgen.http.Url
     */
    val value: String = "",
)
