package io.github.kingg22.ktorgen.http

/**
 * Annotate a single part of a multipart request.
 *
 * ```kotlin
 * @POST("upload")
 * @Multipart
 * suspend fun uploadFile(@Part("description") description: String, @Part("extra") data: List<PartData>): String
 * ```
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class Part(
    /** The name of the part */
    val value: String = "",
    /** The `Content-Transfer-Encoding` of this part. */
    val encoding: String = "binary",
)
