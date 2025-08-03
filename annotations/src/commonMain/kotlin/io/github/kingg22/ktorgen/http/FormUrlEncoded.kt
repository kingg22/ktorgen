package io.github.kingg22.ktorgen.http

/**
 * Indicate the body will use form URL encoding.
 *
 * Will have the header `application/x-www-form-urlencoded` MIME type.
 *
 * Fields parameters **needed** to annotate with [@Field][Field] or [@FieldMap][FieldMap]
 *
 * Field names and values will be UTF-8 encoded before being URI-encoded in accordance to
 * [RFC-3986]("https://datatracker.ietf.org/doc/html/rfc3986")
 *
 * @see <a href="https://ktor.io/docs/client-requests.html#form_parameters">Ktor Client Request - Form parameters</a>
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class FormUrlEncoded
