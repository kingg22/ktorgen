package io.github.kingg22.ktorgen.core

/** Experimental API, the compiler can omit the property or annotation, or generated code has errors. */
@RequiresOptIn("KtorGen experimental API, the generated code may be fail or not generated", RequiresOptIn.Level.ERROR)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
annotation class KtorGenExperimental
