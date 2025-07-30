package io.github.kingg22.ktorgen.http

/**
 * Query parameter appended to the URL that has no value.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class QueryName(
    /** Specifies whether the argument value to the annotated method parameter is already URL encoded. */
    val encoded: Boolean = false,
)
