package io.github.kingg22.ktorgen.http

import io.github.kingg22.ktorgen.core.KTORGEN_DEFAULT_NAME

/**
 * Annotate a single part of a multipart request.
 *
 * Where the name of the parameter is used as the name of the part (or the [value] if set),
 * the value of the parameter is used as the body of the part.
 *
 * If the type is [PartData](https://api.ktor.io/ktor-http/io.ktor.http.content/-part-data/index.html)
 * the value will be used directly with its content type.
 *
 * ```kotlin
 * @POST("upload")
 * @Multipart
 * suspend fun uploadFile(
 *   @Part(value = "other_name") description: String,
 *   @Part data: List<PartData>,
 * ): String
 *
 * // Generated code
 * val _content = formData {
 *     this.append(key = """other_name""", value = """$description""")
 * }
 * this.setBody(MultiPartFormDataContent(listOf(data) + _content))
 * ```
 * @see <a href="https://ktor.io/docs/client-requests.html#upload_file">Ktor Client Request - Upload File</a>
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class Part(
    /** The name of the part. Default the name of the _function parameter_ */
    val value: String = KTORGEN_DEFAULT_NAME,
    /** The `Content-Transfer-Encoding` of this part. */
    val encoding: String = "binary",
)
