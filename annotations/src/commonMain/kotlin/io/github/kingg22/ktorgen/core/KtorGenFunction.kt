package io.github.kingg22.ktorgen.core

import kotlin.reflect.KClass

/**
 * Indicates that a method within a `@KtorGen`-annotated interface [should participate in code generation][generate].
 *
 * This annotation provides fine-grained control over how annotations from the original interface method
 * are handled and propagated to the generated implementation.
 *
 * It is useful when you want to [selectively propagate certain annotations][propagateAnnotations] from the original method,
 * such as [`@JvmSynthetic`, `@Deprecated`][annotations],
 * or [annotations marked with `@RequiresOptIn`][optInAnnotations],
 * to the generated method.
 *
 * Example
 * ```kotlin
 * interface UserRoutes {
 *     @GET("/users/{id}")
 *     @KtorGenFunction(
 *         propagateAnnotations = true, // JvmSynthetic is propagated as-is
 *         annotations = [CustomAnnotation::class], // required have empty constructor, e.g. @JvmOverloads
 *         optInAnnotations = [ExperimentalApi::class], // means = don't propagate, prefer optIn this
 *     )
 *     @JvmSynthetic
 *     @ExperimentalApi
 *     suspend fun getUser(@Path id: Int): User
 * }
 * ```
 * @see KtorGen
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class KtorGenFunction(
    /** Indicate the annotated function going to generate the code or not. */
    val generate: Boolean = true,

    /**
     * _[KtorGenExperimental]_ Indicate if annotations from the source method should be copied to the generated method.
     *
     * If `true`, the processor will attempt to copy all annotations.
     * @see optInAnnotations
     * @see annotations
     */
    @property:KtorGenExperimental
    val propagateAnnotations: Boolean = true,

    /**
     * _[KtorGenExperimental]_ Additional annotations or only these annotations to propagate as-is from the interface method to the generated implementation.
     *
     * The annotations need to have empty constructor like [@JvmSynthetic][kotlin.jvm.JvmSynthetic].
     * Annotations requires properties like [@JvmName][kotlin.jvm.JvmName] can't be used.
     * In that case, manually declare a function with the generated class.
     *
     * For example, `[JvmSynthetic::class]`
     */
    @property:KtorGenExperimental
    val annotations: Array<KClass<out Annotation>> = [],

    /**
     * _[KtorGenExperimental]_ Opt-in annotations, which should be propagated to generated method,
     * need be marked with [@RequiresOptIn][RequiresOptIn] or [@SubclassOptInRequired][SubclassOptInRequired],
     * otherwise the generated code will not compile because requirements of [@OptIn][OptIn].
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
