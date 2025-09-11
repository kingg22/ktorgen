@file:JvmName("KspExt")
@file:JvmMultifileClass
@file:OptIn(KspExperimental::class, ExperimentalContracts::class)

package io.github.kingg22.ktorgen.extractor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ksp.toAnnotationSpec
import io.github.kingg22.ktorgen.model.annotations.ktorGenAnnotations
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.reflect.KClass

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
    } catch (_: NoSuchElementException) {
        this.annotations.singleOrNull { it.shortName.getShortName() == A::class.simpleName!! }?.let(manualExtraction)
    }
}

/** Extract all annotations of type [A] and map each to [R] */
inline fun <reified A : Annotation, R : Any> KSAnnotated.getAllAnnotation(
    noinline manualExtraction: (KSAnnotation) -> R,
    noinline mapFromAnnotation: (A) -> R,
): Sequence<R> = try {
    this.getAnnotationsByType(A::class).map(mapFromAnnotation)
} catch (_: NoSuchElementException) {
    this.annotations.filter { it.shortName.getShortName() == A::class.simpleName!! }.map(manualExtraction)
}

fun extractAnnotationsFiltered(declaration: KSAnnotated): Pair<Set<AnnotationSpec>, Set<AnnotationSpec>> {
    val ktorGenParametersNames = ktorGenAnnotations.mapNotNull(KClass<*>::simpleName)

    val optIn = declaration.annotations
        .filterNot { it.shortName.getShortName() in ktorGenParametersNames }
        .filter { it.shortName.getShortName() == "OptIn" }
        .map(KSAnnotation::toAnnotationSpec)
        .toSet()

    val propagateAnnotations = declaration.annotations
        .filterNot { it.shortName.getShortName() in ktorGenParametersNames }
        .map(KSAnnotation::toAnnotationSpec)
        .filterNot { it in optIn }
        .toSet()

    return propagateAnnotations to optIn
}

fun mergeOptIns(existing: Set<AnnotationSpec>, extra: Set<AnnotationSpec>): AnnotationSpec? {
    val existingClasses = existing.flatMap { it.members }.toSet()
    val extraClasses = extra.flatMap { it.members }.toSet()

    val merged = (existingClasses + extraClasses).distinct()

    return if (merged.isEmpty()) {
        null
    } else {
        AnnotationSpec
            .builder(ClassName("kotlin", "OptIn"))
            .apply { merged.forEach { addMember(it) } }
            .build()
    }
}
