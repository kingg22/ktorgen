package io.github.kingg22.ktorgen.core

/**
 * An annotation used to mark an **expect factory function** for multi-platform support in KSP running on each platform.
 *
 * This annotation signals to code generation tools that an actual function declaration
 * should be generated for Kotlin Multiplatform platforms.
 *
 * The signature of the function needs to match a constructor or factory function defined in [@KtorGen][KtorGen].
 * If not, the code generation will fail.
 *
 * It is part of the experimental API.
 *
 * Example:
 * ```kotlin
 * // Only function can be `expect`
 * interface ApiService {
 *     @GET("/users")
 *     suspend fun getUsers(): List<User>
 * }
 *
 * // KtorGen going to generate an actual function for each platform with valid implementation
 * @KtorGenFunctionKmp
 * expect fun ApiService(httpClient: HttpClient): ApiService
 * ```
 * @see KtorGen
 * @see KtorGenFunction
 */
@KtorGenExperimental
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class KtorGenFunctionKmp(
    /**
     * Custom KDoc comment for the generated actual function.
     *
     * Useful to indicate that the class is auto-generated and shouldn't be modified.
     */
    val customHeader: String = "",
)
