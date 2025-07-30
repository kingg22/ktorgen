package io.github.kingg22.ktorgen.http

/**
 * Named key/value pairs for a form-encoded request.
 *
 * **Needs** to be used in combination with [@FormUrlEncoded][FormUrlEncoded]
 *
 * ```kotlin
 * @POST
 * @FormUrlEncoded
 * suspend fun example(@FieldMap things: Map<String, String>): Response
 *
 * example(mapOf("name" to "Bob Smith", "position" to "President"))
 * // Generate name=Bob+Smith&position=President
 * ```
 *
 * [Pair] / vararg example:
 * ```kotlin
 * @POST
 * @FormUrlEncoded
 * suspend fun example(@FieldMap vararg fullNames: Pair<String, String>): Response
 *
 * example("first_name" to "Bob", "last_name" to "Smith", "alias" to "Jane Doe")
 * // Generate first_name=Bob&last_name=Smith&alias=Jane+Doe
 * ```
 * @see Field
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class FieldMap(
    /** Specifies whether the names and values are already URL encoded. */
    val encoded: Boolean = false,
)
