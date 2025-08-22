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
 *     // empty definition don't generate code*
 *
 *     @KtorGen
 *     companion object
 * }
 *
 * val route: UserRoute
 *
 * route = _UserRouteImpl(httpClient) // Constructor of impl class
 * route = UserRoute(httpClient) // Factory top-level function (if enabled)
 * route = UserRoute.create(httpClient) // Companion factory (if enabled and Companion is available)
 * route = httpClient.userRoute() // Extension function of HttpClient (if enabled)
 * ```
 *
 * The generated implementation will:
 * - [Be named with an optional prefix or custom name][name].
 * - [Implement the target interface][generate] with the same [visibility modifier][visibilityModifier].
 * - Receive an `HttpClient` as its primary constructor argument.
 * - [Propagate annotations][propagateAnnotations].
 * - Mark all generated code with [@Generated][Generated] and add [comments][customFileHeader].
 * - [Declare a factory top level function][generateTopLevelFunction].
 *
 * @see KtorGenFunction
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
     * Set a base path (or URL) for the current interface, all generated functions going to init with this + it path.
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
     *
     * Example: `fun UserRoute(client: HttpClient): UserRoute`
     */
    val generateTopLevelFunction: Boolean = true,

    /**
     * Whether to generate a `create(client)` extension function of interface's companion object.
     *
     * This **requires** the annotation to be placed on the companion itself or **declared a companion object explicit**.
     *
     * Example: `UserRoute.create(client)`
     */
    val generateCompanionExtFunction: Boolean = false,

    /**
     * Whether to generate an extension function on `HttpClient` to instantiate the API.
     *
     * Example: `fun HttpClient.userRoute(): UserRoute`
     */
    val generateHttpClientExtension: Boolean = false,

    /**
     * _[KtorGenExperimental]_
     *
     * If `true`,
     * the processor will attempt to copy supported annotations from the source interface into the generated class and extension functions.
     */
    @property:KtorGenExperimental
    val propagateAnnotations: Boolean = true,

    /**
     * _[KtorGenExperimental]_
     *
     * Additional annotations or only these annotations to propagate as-is from the interface to the generated
     * implementation class and extension functions, [see more info about functions][functionAnnotations].
     *
     * The annotations need to have empty constructor like [@JvmSynthetic][kotlin.jvm.JvmSynthetic].
     * Annotations requires properties like [@JvmName][kotlin.jvm.JvmName] can't be used.
     * In that case, declare manually a function with the generated class.
     *
     * For example, `[ExperimentalApi::class]`
     * @see KtorGenFunction.annotations
     */
    @property:KtorGenExperimental
    val annotations: Array<KClass<out Annotation>> = [],

    /**
     * _[KtorGenExperimental]_
     *
     * Opt-in annotations that should be propagated to generated class and extension functions,
     * need be marked with [@RequiresOptIn][RequiresOptIn] or [@SubclassOptInRequired][SubclassOptInRequired],
     * otherwise the generated code will not compile because requirements of [@OptIn][OptIn].
     *
     * For example, `[ExperimentalApi::class], [InternalKtorApi::class]`
     */
    @property:KtorGenExperimental
    val optInAnnotations: Array<KClass<out Annotation>> = [],

    /**
     * _[KtorGenExperimental]_
     *
     * Additional annotations or only these annotations to propagate as-is to the generated extension functions:
     * - [Top level Function factory][generateTopLevelFunction]
     * - [Companion factory][generateCompanionExtFunction]
     * - [HttpClient extension][generateHttpClientExtension]
     *
     * The default behavior is [propagate annotations][propagateAnnotations] of [interface][annotations] applicable to functions and
     * [opt-in annotations][optInAnnotations].
     *
     * The annotations need to have empty constructor like [@JvmSynthetic][kotlin.jvm.JvmSynthetic].
     * Annotations requires properties like [@JvmName][kotlin.jvm.JvmName] can't be used.
     * In that case, declare manually a function with the generated class.
     *
     * For example, `[JvmSynthetic::class]`
     */
    @property:KtorGenExperimental
    val functionAnnotations: Array<KClass<out Annotation>> = [],

    /**
     * _[KtorGenExperimental]_
     *
     * Indicate the visibility modifier for all generated code (class, primary constructor, and extension functions)
     *
     * Can be `public` or `internal`.
     * Is not valid: `private` and `protected`.
     * By default, is visibility modifier of the interface.
     *
     * Don't confuse the visibility of generated code with interface visibility.
     * The interface can't be `private`
     *
     * Combination of `internal interface` and `public class` is valid,
     * but can lead to compilation errors if exposed something internal in `public function`.
     * For advanced use cases,
     * use `internal` modifier directly on the interface and manually write functions or only use constructor.
     *
     * @see <a href="https://kotlinlang.org/docs/visibility-modifiers.html#packages">Kotlin Visibility Modifiers</a>
     */
    @property:KtorGenExperimental
    val visibilityModifier: String = KTORGEN_DEFAULT_NAME,

    /**
     * Custom header **comment** inserted at the top of the generated Kotlin file.
     *
     * Useful for license declarations, warnings, or documentation.
     *
     * By default, there is a notice of _generated_ code.
     */
    val customFileHeader: String = KTORGEN_DEFAULT_NAME,

    /**
     * Custom KDoc comment or annotations for the generated implementation class.
     *
     * Useful to indicate that the class is auto-generated and shouldn't be modified or annotated with custom code.
     */
    val customClassHeader: String = "",
)
