package io.github.kingg22.ktorgen.core

/**
 * Indicates that a method within a `@KtorGen`-annotated interface [should participate in code generation][generate].
 *
 * Example
 * ```kotlin
 * interface UserRoutes {
 *     @GET("/users")
 *     suspend fun getUsers(): List<User>
 *
 *     @KtorGenFunction(generate = false)
 *     @JvmSynthetic
 *     @ExperimentalApi
 *     fun getUsersBlocking(): List<User> = runBlocking { getUsers() }
 * }
 * ```
 * In the above example, the `getUsersBlocking` function will not be overridden by KtorGen.
 *
 * @see KtorGen
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class KtorGenFunction(
    /** Indicate the annotated function going to generate the code or not. */
    val generate: Boolean = true,

    /**
     * Custom KDoc comment for the generated implementation class.
     *
     * Useful to indicate that the class is auto-generated and shouldn't be modified.
     */
    val customHeader: String = "",
)
