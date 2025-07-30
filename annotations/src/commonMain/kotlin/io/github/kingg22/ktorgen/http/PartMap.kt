package io.github.kingg22.ktorgen.http

/**
 * Denote name and value parts of a multipart request.
 *
 * If the type is List<PartData> the value will be used directly with its content type.
 *
 * ```kotlin
 * @POST("upload")
 * @Multipart
 * suspend fun uploadFile(
 *     @PartMap description: Map<String, String>,
 *     @PartMap data: List<PartData>,
 *     @PartMap vararg extras: Pair<String, String>,
 * ): String
 * ```
 * @see Multipart
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class PartMap(
    /** The `Content-Transfer-Encoding` of this part. */
    val encoding: String = "binary",
)
