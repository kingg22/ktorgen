package io.github.kingg22.ktorgen.http

/**
 * Indicate the request body is multipart
 *
 * Parts should be declared as parameters and annotated with [@Part][Part]
 *
 * @see <a href="https://ktor.io/docs/client-requests.html#-595bwt_205">Ktor Client Request - Multipart</a>
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class Multipart
