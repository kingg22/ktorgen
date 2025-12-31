package io.github.kingg22.ktorgen.core

/**
 * Each generated class, function, and files is annotated with this to identify them if they need to be excluded,
 * for instance, by static analysis tools.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.FILE)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
annotation class Generated
