package io.github.kingg22.ktorgen.http

/**
 * Query parameter keys and values appended to the URL.
 *
 * A `null` value for the map, as a key is not allowed.
 *
 * @see Query
 * @see QueryName
 * @see <a href="https://ktor.io/docs/client-requests.html#query_parameters">Ktor Client Request - Query parameter</a>
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class QueryMap(
    /** Specifies whether the argument value to the annotated method parameter is already URL encoded. */
    val encoded: Boolean = false,
)
