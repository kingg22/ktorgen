package io.github.kingg22.ktorgen.http

/**
 * This is only for easy migration from other libraries, using **this doesn't generate code**
 *
 * From
 * ```kotlin
 * @Headers("Content-Type: application/json", "Accept: application/json")
 * ```
 * to
 * ```kotlin
 * @Header(Header.ContentType, Header.ContentTypes.Application.Json)//type-safe
 * @Header("Accept", "application/json)
 * ```
 * @see Header
 * @see HeaderParam
 * @see Header.Companion
 */
@Deprecated(
    "In KtorGen vararg header with format 'name: value' is not valid, refactor is needed",
    ReplaceWith("io.github.kingg22.ktorgen.http.Header"),
    DeprecationLevel.ERROR,
)
annotation class Headers(vararg val values: String)
