package io.github.kingg22.ktorgen.core

import kotlin.reflect.KClass

/**
 * Indicates that an interface should be used for Ktor Client code generation.
 *
 * This annotation controls how the generated class and companion/helper functions are emitted.
 * It is expected to be used on the interface itself or optionally on its companion object for factory generation.
 *
 * Basic usage:
 *
 * ```kotlin
 * interface UserRoute {
 *     // Interface definition
 *
 *     @KtorGen
 *     companion object
 * }
 *
 * val route = UserRoute.create(httpClient) // Companion factory (if enabled and Companion is available)
 * // or
 * val route = UserRoute(httpClient) // Top-level factory function (if enabled)
 * ```
 *
 * The generated implementation will:
 * - Be named with an optional prefix or custom name.
 * - Implement the target interface.
 * - Propagated annotations.
 * - Receive an `HttpClient` as its constructor argument.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class KtorGen(
    /**
     * Custom name for the generated implementation class.
     *
     * If not specified, a default naming convention will be used (e.g. `_XImpl`).
     *
     * Example: `name = "UserRoutes"` → class `_UserRoutesImpl` will be generated.
     */
    val name: String = KTORGEN_DEFAULT_NAME,

    /**
     * Whether to generate a top-level function in the same package as the interface.
     *
     * This function will allow instantiating the implementation by passing an `HttpClient`.
     * Example: `fun UserRoute(client: HttpClient): UserRoute`
     */
    val generateTopLevelFunction: Boolean = true,

    /**
     * Whether to generate a `create(client)` function inside the interface's companion object.
     *
     * This requires the annotation to be placed on the companion itself or declared companion object explicit.
     * Example: `UserRoute.create(client)`
     */
    val generateCompanionExtFunction: Boolean = false,

    /**
     * Whether to generate an extension function on `HttpClient` to instantiate the API.
     *
     * Example: `fun HttpClient.userRoute(): UserRoute`
     */
    val generateExtensions: Boolean = false,

    /**
     * Adds `@JvmStatic` modifier to the companion `create` function (if generated).
     *
     * This makes the factory accessible from Java code as a static method.
     */
    val jvmStatic: Boolean = false,

    /** Marks the generated factory function as `@JsExport`, for JavaScript interop in Kotlin/JS. */
    val jsStatic: Boolean = false,

    /** Indicate the generated class, constructor, and functions all are going to be public. */
    val generatePublic: Boolean = false,

    /** If `true`, the processor will attempt to copy supported annotations from the original method into the generated method. */
    val propagateAnnotations: Boolean = true,

    /**
     * Additional annotations or only these annotations to propagate as-is from the interface method to the generated implementation.
     *
     * For example, [JvmSynthetic::class], [Deprecated::class]
     */
    val annotations: Array<KClass<out Annotation>> = [],

    /**
     * Opt-in annotations that should be propagated, usually marked with `@RequiresOptIn`.
     *
     * For example, [ExperimentalApi::class], [InternalKtorApi::class]
     */
    val optInAnnotations: Array<KClass<out Annotation>> = [],

    /**
     * Indicate the visibility modifier of the generated **class**
     *
     * For generated function, extension functions, and constructor are controlate by [generatePublic] + this.
     * @see <a href="https://kotlinlang.org/docs/visibility-modifiers.html#packages">Kotlin Visibility Modifiers</a>
     */
    val visibilityModifier: String = "public",

    /**
     * Custom header comment inserted at the top of the generated Kotlin file.
     *
     * Useful for license declarations, warnings, or documentation.
     * **Is not validated.**
     */
    val customFileHeader: String = KTORGEN_DEFAULT_NAME,

    /**
     * Custom KDoc comment or annotations for the generated implementation class.
     *
     * Useful to indicate that the class is auto-generated and shouldn't be modified or annotate with custom code.
     */
    val customClassHeader: String = KTORGEN_DEFAULT_NAME,
)
