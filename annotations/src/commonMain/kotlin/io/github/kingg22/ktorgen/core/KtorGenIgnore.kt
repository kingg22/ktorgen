package io.github.kingg22.ktorgen.core

/**
 * Indicates that the annotated interface or function should not be generated implementation.
 *
 * This is useful when you want to manually implement the interface or when delegation is not desired for other reasons.
 */
@Target(AnnotationTarget.TYPE, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class KtorGenIgnore
