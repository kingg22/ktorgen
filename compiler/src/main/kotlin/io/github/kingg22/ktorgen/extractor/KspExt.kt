@file:JvmName("KspExt")
@file:JvmMultifileClass
@file:OptIn(KspExperimental::class, ExperimentalContracts::class)

package io.github.kingg22.ktorgen.extractor

import com.google.devtools.ksp.KSTypeNotPresentException
import com.google.devtools.ksp.KSTypesNotPresentException
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ksp.toAnnotationSpec
import io.github.kingg22.ktorgen.model.Options
import io.github.kingg22.ktorgen.model.annotations.ktorGenAnnotations
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.reflect.KClass
import com.google.devtools.ksp.getAnnotationsByType as getAnnotationsByKClass
import com.google.devtools.ksp.isAnnotationPresent as isAnnotationPresentByKClass

// Shortcuts with reified, more elegant
inline fun <reified R : Annotation> KSAnnotated.getAnnotationsByType() = getAnnotationsByKClass(R::class)

inline fun <reified R : Annotation> KSAnnotated.isAnnotationPresent() = isAnnotationPresentByKClass(R::class)

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
        this.getAnnotationsByType<A>().singleOrNull()?.let(mapFromAnnotation)
    } catch (_: NoSuchElementException) {
        this.annotations.singleOrNull { it.shortName.getShortName() == A::class.simpleName!! }?.let(manualExtraction)
    } catch (_: KSTypeNotPresentException) {
        this.annotations.singleOrNull { it.shortName.getShortName() == A::class.simpleName!! }?.let(manualExtraction)
    } catch (_: KSTypesNotPresentException) {
        this.annotations.singleOrNull { it.shortName.getShortName() == A::class.simpleName!! }?.let(manualExtraction)
    }
}

/** Extract all annotations of type [A] and map each to [R] */
inline fun <reified A : Annotation, R : Any> KSAnnotated.getAllAnnotation(
    noinline manualExtraction: (KSAnnotation) -> R,
    noinline mapFromAnnotation: (A) -> R,
): Sequence<R> = try {
    this.getAnnotationsByType<A>().map(mapFromAnnotation)
} catch (_: NoSuchElementException) {
    this.annotations.filter { it.shortName.getShortName() == A::class.simpleName!! }.map(manualExtraction)
} catch (_: KSTypeNotPresentException) {
    this.annotations.filter { it.shortName.getShortName() == A::class.simpleName!! }.map(manualExtraction)
} catch (_: KSTypesNotPresentException) {
    this.annotations.filter { it.shortName.getShortName() == A::class.simpleName!! }.map(manualExtraction)
}

private val ktorGenParametersNames = ktorGenAnnotations.mapNotNull(KClass<*>::simpleName)

fun extractAnnotationsFiltered(
    declaration: KSAnnotated,
    filter: (KSAnnotation) -> Boolean = { true },
): Triple<Set<AnnotationSpec>, Set<AnnotationSpec>, List<KSAnnotated>> {
    val deferredSymbols = mutableListOf<KSAnnotated>()

    val (propagateAnnotations, optIn) = declaration.annotations
        .filterNot { it.shortName.getShortName() in ktorGenParametersNames }
        .filterNot {
            val type = it.annotationType.resolve()
            val isError = type.isError
            if (isError) deferredSymbols += it.annotationType
            isError
        }
        .filter(filter)
        .mapNotNull {
            try {
                it.toAnnotationSpec()
            } catch (e: Throwable) {
                if ((e is IllegalStateException || e is IllegalArgumentException) &&
                    e.message?.contains("not resolv") == true
                ) {
                    deferredSymbols += it.annotationType
                    null
                } else {
                    throw e
                }
            }
        }
        .distinct()
        .partition {
            // true is simple annotation, false indicate optIn annotation
            it.typeName != ClassName("kotlin", "OptIn")
        }

    return Triple(propagateAnnotations.toSet(), optIn.toSet(), deferredSymbols)
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

fun Options.mergeAnnotations(extra: Set<AnnotationSpec>, extraOptIns: Set<AnnotationSpec>) = (annotations + extra)
    .filterNot { ann ->
        ann.typeName == ClassName("kotlin", "OptIn") ||
            extraOptIns.any { it.typeName == ann.typeName } ||
            optIns.any { it.typeName == ann.typeName }
    }
    .toSet()
