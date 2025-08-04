package io.github.kingg22.ktorgen.http

import org.intellij.lang.annotations.Language

/**
 * Make a request with a custom HTTP verb.
 *
 * ```kotlin
 * @HTTP(method = "CUSTOM", path = "custom/endpoint/")
 * suspend fun customEndpoint(): Response
 * ```
 *
 * @see <a href="https://ktor.io/docs/client-requests.html#http-method">Ktor Client Request - Http Method</a>
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class HTTP(
    /** HTTP method verb. */
    @Language("http-method-reference")
    val method: String,
    /**
     * Relative or absolute URL of the endpoint.
     *
     * Is optional, when is empty or blank, evaluate the [Url] annotation, otherwise the base URL is used instead.
     * @see io.github.kingg22.ktorgen.http.Url
     */
    @Language("http-url-reference")
    val path: String = "",
    /** Override default rules, e.g., GET is not allowed to have a body */
    val hasBody: Boolean = false,
)
