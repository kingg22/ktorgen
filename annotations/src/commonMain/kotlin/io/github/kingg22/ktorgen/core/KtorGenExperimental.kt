package io.github.kingg22.ktorgen.core

/**
 * Experimental API, the processor can omit the annotation, or generated code has errors.
 *
 * To enable the experimental API, add the following to your `build.gradle(.kts)`:
 * ```kotlin
 * ksp {
 *   arg("ktorgen.experimental", "true")
 * }
 * ```
 * Otherwise, the processor will throw an error and/or skip the experimental API.
 *
 * This annotation is restricted to this package, the generated code is going to omit this annotation (or opt in of this).
 * Please don't use this annotation in your code.
 */
@RequiresOptIn("KtorGen experimental API, the generated code may be fail or not generated", RequiresOptIn.Level.ERROR)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
annotation class KtorGenExperimental
