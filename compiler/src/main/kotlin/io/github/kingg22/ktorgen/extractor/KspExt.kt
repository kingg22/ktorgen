package io.github.kingg22.ktorgen.extractor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.sequences.forEach

/** Safe get and cast the properties of annotation */
inline fun <reified T> KSAnnotation.getArgumentValueByName(name: String): T? = this.arguments.firstOrNull {
    it.name?.asString() == name && it.value != null && it.value is T
}?.value as? T

// try-catch because default values is not working for KMP builds https://github.com/google/ksp/issues/2356

/** Callbacks are invoked when the annotation is present, else NO OP */
@OptIn(KspExperimental::class, ExperimentalContracts::class)
inline fun <reified A : Annotation> KSAnnotated.getAnnotation(
    crossinline manualExtraction: (KSAnnotation) -> Unit,
    crossinline mapFromAnnotation: (A) -> Unit,
) {
    contract {
        callsInPlace(manualExtraction, InvocationKind.AT_MOST_ONCE)
        callsInPlace(mapFromAnnotation, InvocationKind.AT_MOST_ONCE)
    }
    try {
        this.getAnnotationsByType(A::class).firstOrNull()?.let(mapFromAnnotation)
    } catch (_: Exception) {
        this.annotations.firstOrNull { it.shortName.getShortName() == A::class.simpleName!! }?.let(manualExtraction)
    }
}

/** Callbacks are invoked when one or more annotation is present, else NO OP */
@OptIn(KspExperimental::class)
inline fun <reified A : Annotation> KSAnnotated.getAllAnnotation(
    crossinline manualExtraction: (KSAnnotation) -> Unit,
    crossinline mapFromAnnotation: (A) -> Unit,
) {
    try {
        this.getAnnotationsByType(A::class).forEach(mapFromAnnotation)
    } catch (_: Exception) {
        this.annotations.filter { it.shortName.getShortName() == A::class.simpleName!! }.forEach(manualExtraction)
    }
}
