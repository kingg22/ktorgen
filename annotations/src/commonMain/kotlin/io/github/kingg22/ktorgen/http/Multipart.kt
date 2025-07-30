package io.github.kingg22.ktorgen.http

/**
 * Send a request body is multipart
 *
 * Parts should be declared as parameters and annotated with [@Part][Part]
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class Multipart
