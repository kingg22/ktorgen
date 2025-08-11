package io.github.kingg22.ktorgen.http

import io.github.kingg22.ktorgen.core.KTORGEN_DEFAULT_NAME

/**
 * A named pair for a form-encoded request.
 *
 * **Needs** to be used in combination with [@FormUrlEncoded][FormUrlEncoded]
 *
 * ```kotlin
 * @POST
 * @FormUrlEncoded
 * suspend fun example(@Field name: String, @Field("job") occupation: String): Employee
 *
 * foo.example("Bob Smith", "President")
 * // Generate name=Bob+Smith&job=President
 * ```
 *
 * Arrays/vararg example:
 * ```kotlin
 * @POST
 * @FormUrlEncoded
 * suspend fun example(@Field vararg name: String): List<Employee>
 *
 * example("Bob Smith", "Jane Doe")
 * // Generate name=Bob+Smith&name=Jane+Doe
 * ```
 *
 * @see FieldMap
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class Field(
    /** Name of the parameter. Default the name of the _function parameter_ */
    val value: String = KTORGEN_DEFAULT_NAME,
    /** Specifies whether the names and values are already URL encoded. */
    val encoded: Boolean = false,
)
