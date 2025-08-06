package io.github.kingg22.ktorgen.core

import org.intellij.lang.annotations.Language
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
 * - Receive an `HttpClient` as its primary constructor argument.
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

    /** Indicate the annotated interface going to generate the code or not. */
    val generate: Boolean = true,

    /**
     * Set a base path (or URL) for the current interface, all generated function going to init with this + it path.
     *
     * By default, Ktor Client handle base url for the `HttpClient`.
     *
     * Invalid syntax like `https://github.com///path` _(triple slash where is not common to use)_ throw errors.
     * @see io.github.kingg22.ktorgen.http.Url
     * @see io.github.kingg22.ktorgen.http.HTTP.path
     */
    @Language("http-url-reference")
    val basePath: String = "",

    /**
     * Whether to generate a top-level function in the same package as the interface.
     *
     * This function will allow instantiating the implementation by passing an `HttpClient`.
     * Example: `fun UserRoute(client: HttpClient): UserRoute`
     */
    val generateTopLevelFunction: Boolean = true,

    /**
     * Whether to generate a `create(client)` extension function of interface's companion object.
     *
     * This **requires** the annotation to be placed on the companion itself or **declared companion object explicit**.
     * Example: `UserRoute.create(client)`
     */
    val generateCompanionExtFunction: Boolean = false,

    /**
     * Whether to generate an extension function on `HttpClient` to instantiate the API.
     *
     * Example: `fun HttpClient.userRoute(): UserRoute`
     */
    val generateHttpClientExtension: Boolean = false,

    /** If `true`, the processor will attempt to copy supported annotations from the original method into the generated method. */
    @property:KtorGenExperimental
    val propagateAnnotations: Boolean = true,

    /**
     * Additional annotations or only these annotations to propagate as-is from the interface method to the generated implementation.
     *
     * For example, [JvmSynthetic::class], [Deprecated::class]
     */
    @property:KtorGenExperimental
    val annotations: Array<KClass<out Annotation>> = [],

    /**
     * Opt-in annotations that should be propagated, usually marked with `@RequiresOptIn`.
     *
     * For example, [ExperimentalApi::class], [InternalKtorApi::class]
     */
    @property:KtorGenExperimental
    val optInAnnotations: Array<KClass<out Annotation>> = [],

    /**
     * Indicate the visibility modifier of the generated **class**
     *
     * @see <a href="https://kotlinlang.org/docs/visibility-modifiers.html#packages">Kotlin Visibility Modifiers</a>
     */
    @property:KtorGenExperimental
    val visibilityModifier: String = "public",

    /**
     * Custom header **comment** inserted at the top of the generated Kotlin file.
     *
     * Useful for license declarations, warnings, or documentation.
     *
     * By default, is a notice of _generated_ code.
     */
    val customFileHeader: String = KTORGEN_DEFAULT_NAME,

    /**
     * Custom KDoc comment or annotations for the generated implementation class.
     *
     * Useful to indicate that the class is auto-generated and shouldn't be modified or annotate with custom code.
     */
    val customClassHeader: String = "",
)
