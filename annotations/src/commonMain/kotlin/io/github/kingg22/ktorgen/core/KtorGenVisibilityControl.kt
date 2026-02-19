package io.github.kingg22.ktorgen.core

/**
 * Visibility modifier for generated code. Use with [@KtorGenVisibilityControl()][KtorGenVisibilityControl]
 * @see <a href="https://kotlinlang.org/docs/visibility-modifiers.html#packages">Kotlin Visibility Modifiers</a>
 */
enum class KtorGenVisibility {
    DEFAULT,
    PUBLIC,
    INTERNAL,

    @KtorGenExperimental
    PRIVATE,
}

/**
 * _[KtorGenExperimental]_ Indicate the visibility modifier for all generated code
 * (class, primary constructor, and extension functions)
 *
 * Can be `public` or `internal`.
 * Is not valid: `private`.
 * By default, there is a visibility modifier of the interface.
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
@KtorGenExperimental
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class KtorGenVisibilityControl(
    val visibilityModifier: KtorGenVisibility = KtorGenVisibility.DEFAULT,

    /**
     * Control the visibility modifier of the generated class only.
     *
     * Can be `public` or `internal` or `private`.
     * Private classes have restrictions.
     *
     * By default, is the same behavior as [visibilityModifier].
     */
    val classVisibilityModifier: KtorGenVisibility = KtorGenVisibility.DEFAULT,

    /**
     * Control the visibility modifier of the generated primary constructor only.
     *
     * Can be `public` or `internal`.
     * By default, is the same behavior as [visibilityModifier].
     */
    val constructorVisibilityModifier: KtorGenVisibility = KtorGenVisibility.DEFAULT,

    /**
     * Control the visibility modifier of the generated factory functions only.
     *
     * Can be `public` or `internal`.
     * By default, is the same behavior as [visibilityModifier].
     */
    val functionVisibilityModifier: KtorGenVisibility = KtorGenVisibility.DEFAULT,
)
