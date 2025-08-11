package io.github.kingg22.ktorgen.http

/**
 * Denote name and value parts of a multipart request.
 *
 * ```kotlin
 * @POST("upload")
 * @Multipart
 * suspend fun uploadFile(
 *     @PartMap description: Map<String, String>,
 *     @PartMap vararg extras: Pair<String, String>,
 * ): String
 * ```
 * @see Multipart
 * @see Part
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class PartMap(
    /** The `Content-Transfer-Encoding` of this part. */
    val encoding: String = "binary",
)
