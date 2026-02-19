package io.github.kingg22.ktorgen.core

/**
 * An annotation used to mark an **expect factory function** for multi-platform support in KSP running on each platform.
 * @see KtorGenKmpFactory
 */
@Deprecated(
    "Renamed, use KtorGenKmpFactory instead, to remove in 0.9.0",
    ReplaceWith("KtorGenKmpFactory"),
    DeprecationLevel.ERROR,
)
@KtorGenExperimental
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class KtorGenFunctionKmp
