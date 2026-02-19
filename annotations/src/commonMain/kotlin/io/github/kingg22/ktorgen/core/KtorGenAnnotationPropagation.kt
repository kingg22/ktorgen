package io.github.kingg22.ktorgen.core

import kotlin.reflect.KClass

/**
 * This annotation provides fine-grained control over how annotations from the original interface method
 * are handled and propagated to the generated implementation.
 *
 * It is useful when you want to [selectively propagate certain annotations][propagateAnnotations] from the original method,
 * such as [`@JvmSynthetic`, `@Deprecated`][annotations],
 * or [annotations marked with `@RequiresOptIn`][optInAnnotations],
 * to the generated method.
 */
@KtorGenExperimental
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class KtorGenAnnotationPropagation(
    /**
     * Indicate if annotations from the source method should be copied to the generated method.
     *
     * If `true`, the processor will attempt to copy all annotations.
     * @see optInAnnotations
     * @see annotations
     */
    val propagateAnnotations: Boolean = true,

    /**
     * Additional annotations or only these annotations to propagate as-is from the interface method to the generated implementation.
     *
     * The annotations need to have empty constructor like [@JvmSynthetic][kotlin.jvm.JvmSynthetic].
     * Annotations requires properties like [@JvmName][kotlin.jvm.JvmName] can't be used.
     * In that case, manually declare a function with the generated class.
     *
     * For example, `[JvmSynthetic::class]`
     */
    val annotations: Array<KClass<out Annotation>> = [],

    /**
     * Opt-in annotations, which should be propagated to generated method,
     * need be marked with [@RequiresOptIn][RequiresOptIn] or [@SubclassOptInRequired][SubclassOptInRequired],
     * otherwise the generated code will not compile because requirements of [@OptIn][OptIn].
     *
     * For example, `[ExperimentalApi::class, InternalKtorApi::class]`
     */
    val optInAnnotations: Array<KClass<out Annotation>> = [],

    /**
     * Additional annotations or only these annotations to propagate as-is to the generated extension functions:
     * - [Top level Function factory][KtorGenTopLevelFactory]
     * - [Companion factory][KtorGenCompanionExtFactory]
     * - [HttpClient extension][KtorGenHttpClientExtFactory]
     *
     * The default behavior is [propagate annotations][propagateAnnotations] of [interface][annotations] applicable to functions and
     * [opt-in annotations][optInAnnotations].
     *
     * The annotations need to have empty constructor like [@JvmSynthetic][kotlin.jvm.JvmSynthetic].
     * Annotations requires properties like [@JvmName][kotlin.jvm.JvmName] can't be used.
     * In that case, manually declare a function with the generated class.
     *
     * For example, `[JvmSynthetic::class]`
     */
    val factoryFunctionAnnotations: Array<KClass<out Annotation>> = [],
)
