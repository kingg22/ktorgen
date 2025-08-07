@file:OptIn(KspExperimental::class, ExperimentalContracts::class)

package io.github.kingg22.ktorgen.extractor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/** Safe get and cast the properties of annotation */
inline fun <reified T> KSAnnotation.getArgumentValueByName(name: String): T? = this.arguments.firstOrNull {
    it.name?.asString() == name && it.value != null && it.value is T
}?.value as? T

// try-catch because default values is not working for KMP builds https://github.com/google/ksp/issues/2356

/** Extract one annotation of type [A] and map to [R] if present. For more than one annotation use [getAllAnnotation] */
inline fun <reified A : Annotation, R : Any> KSAnnotated.getAnnotation(
    crossinline manualExtraction: (KSAnnotation) -> R,
    crossinline mapFromAnnotation: (A) -> R,
): R? {
    contract {
        callsInPlace(manualExtraction, InvocationKind.AT_MOST_ONCE)
        callsInPlace(mapFromAnnotation, InvocationKind.AT_MOST_ONCE)
    }
    return try {
        this.getAnnotationsByType(A::class).singleOrNull()?.let(mapFromAnnotation)
    } catch (_: Exception) {
        this.annotations.singleOrNull { it.shortName.getShortName() == A::class.simpleName!! }?.let(manualExtraction)
    }
}

/** Extract all annotations of type [A] and map each to [R] */
inline fun <reified A : Annotation, R : Any> KSAnnotated.getAllAnnotation(
    noinline manualExtraction: (KSAnnotation) -> R,
    noinline mapFromAnnotation: (A) -> R,
): Sequence<R> = try {
    this.getAnnotationsByType(A::class).map(mapFromAnnotation)
} catch (_: Exception) {
    this.annotations.filter { it.shortName.getShortName() == A::class.simpleName!! }.map(manualExtraction)
}
