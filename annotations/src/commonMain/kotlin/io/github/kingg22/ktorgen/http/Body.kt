package io.github.kingg22.ktorgen.http

/**
 * For upload data in an HTTP Body on a POST/PUT request
 *
 * Instead of sending in as request parameters or form-style request body.
 * ```kotlin
 * @POST("create")
 * suspend fun upload(@Body issue: Issue)
 * ```
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class Body
