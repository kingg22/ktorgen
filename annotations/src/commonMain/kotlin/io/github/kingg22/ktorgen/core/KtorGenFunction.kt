package io.github.kingg22.ktorgen.core

import kotlin.reflect.KClass

/**
 * Indicates that a method within a `@KtorGen`-annotated interface should participate in code generation.
 *
 * This annotation provides fine-grained control over how annotations from the original interface method
 * are handled and propagated to the generated implementation.
 *
 * It is useful when you want to selectively propagate certain annotations from the original method, such as `@JvmSynthetic`,
 * `@Deprecated`, or annotations marked with `@RequiresOptIn`, to the generated method.
 *
 * Example
 * ```kotlin
 * interface UserRoutes {
 *     @KtorGenMethod(
 *         propagateAnnotations = true,
 *         annotations = [CustomAnnotation::class], // required it don't need values, e.g. @JvmOverloads
 *         optInAnnotations = [ExperimentalApi::class], // means = don't propagate, prefer optIn this
 *     )
 *     @JvmSynthetic
 *     @ExperimentalApi
 *     suspend fun getUser(id: Int): User
 * }
 * ```
 *
 * @property annotations A list of specific annotations to propagate regardless of their type hierarchy.
 *                       Useful for custom or third-party annotations (e.g., `@JvmSynthetic`, `@Deprecated`).
 *
 * @property optInAnnotations Annotations that are marked with `@RequiresOptIn` which should be explicitly re-added
 *                             to the generated method. This ensures the generated method retains the same
 *                             experimental API visibility requirements as the original.
 *
 * @see KtorGen
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class KtorGenFunction(
    /** Indicate the annotated function going to generate the code or not. */
    val generate: Boolean = true,

    /** If `true`, the processor will attempt to copy supported annotations from the original method into the generated method. */
    @property:KtorGenExperimental
    val propagateAnnotations: Boolean = true,

    /**
     * Additional annotations or only these annotations to propagate as-is from the interface method to the generated implementation.
     *
     * For example, `[JvmSynthetic::class, Deprecated::class]`
     */
    @property:KtorGenExperimental
    val annotations: Array<KClass<out Annotation>> = [],

    /**
     * Opt-in annotations that should be propagated, usually marked with [@RequiresOptIn][RequiresOptIn].
     *
     * For example, `[ExperimentalApi::class, InternalKtorApi::class]`
     */
    @property:KtorGenExperimental
    val optInAnnotations: Array<KClass<out Annotation>> = [],

    /**
     * Custom KDoc comment for the generated implementation class.
     *
     * Useful to indicate that the class is auto-generated and shouldn't be modified.
     */
    val customHeader: String = "",
)
