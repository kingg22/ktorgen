@file:OptIn(KspExperimental::class)

package io.github.kingg22.ktorgen.extractor

import com.google.devtools.ksp.KSTypeNotPresentException
import com.google.devtools.ksp.KSTypesNotPresentException
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ksp.toAnnotationSpec
import io.github.kingg22.ktorgen.DiagnosticSender
import io.github.kingg22.ktorgen.checkImplementation
import io.github.kingg22.ktorgen.core.KtorGenAnnotationPropagation
import io.github.kingg22.ktorgen.core.KtorGenVisibility
import io.github.kingg22.ktorgen.core.KtorGenVisibilityControl
import io.github.kingg22.ktorgen.model.KotlinOptInClassName
import io.github.kingg22.ktorgen.model.annotations.KTORGEN_KMP_FACTORY
import io.github.kingg22.ktorgen.model.annotations.ktorGenAnnotations
import io.github.kingg22.ktorgen.model.options.AnnotationsOptions
import io.github.kingg22.ktorgen.model.options.VisibilityOptions
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.reflect.KClass
import kotlin.sequences.filterNot
import com.google.devtools.ksp.getAnnotationsByType as getAnnotationsByKClass
import com.google.devtools.ksp.isAnnotationPresent as isAnnotationPresentByKClass

// Shortcuts with reified, more elegant
inline fun <reified R : Annotation> KSAnnotated.getAnnotationsByType() = getAnnotationsByKClass(R::class)

inline fun <reified R : Annotation> KSAnnotated.isAnnotationPresent() = isAnnotationPresentByKClass(R::class)

/**
 * Safe get and cast the properties of annotation
 * @see com.google.devtools.ksp.symbol.KSValueArgument.value
 */
inline fun <reified T> KSAnnotation.getArgumentValueByName(name: String): T? = this.arguments.firstOrNull {
    it.name?.asString() == name && it.value != null && it.value is T
}?.value as? T

// try-catch because default values are not working for KMP builds https://github.com/google/ksp/issues/2356

/** Use the same filter of [getAnnotationsByType] */
fun filterAnnotation(ksAnnotation: KSAnnotation, annotationKClass: KClass<out Annotation>) =
    ksAnnotation.shortName.getShortName() == annotationKClass.simpleName &&
        ksAnnotation.annotationType.resolve().declaration.qualifiedName?.asString() == annotationKClass.qualifiedName

/**
 * Catch those exceptions [NoSuchElementException], [KSTypeNotPresentException] and [KSTypesNotPresentException],
 * because [an issue with default values](https://github.com/google/ksp/issues/2356)
 */
internal inline fun <T> catchKSPExceptions(fallback: () -> T, block: () -> T): T {
    contract {
        callsInPlace(fallback, InvocationKind.AT_MOST_ONCE)
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    return try {
        block()
    } catch (_: NoSuchElementException) {
        fallback()
    } catch (_: KSTypeNotPresentException) {
        fallback()
    } catch (_: KSTypesNotPresentException) {
        fallback()
    }
}

/** Extract one annotation of type [A] and map to [R] if present. For more than one annotation use [getAllAnnotation] */
internal inline fun <reified A : Annotation, R : Any> KSAnnotated.getAnnotation(
    crossinline manualExtraction: (KSAnnotation) -> R,
    crossinline mapFromAnnotation: (A) -> R,
): R? {
    contract {
        callsInPlace(manualExtraction, InvocationKind.AT_MOST_ONCE)
        callsInPlace(mapFromAnnotation, InvocationKind.AT_MOST_ONCE)
    }
    return catchKSPExceptions(
        fallback = { this.annotations.singleOrNull { filterAnnotation(it, A::class) }?.let(manualExtraction) },
    ) {
        this.getAnnotationsByType<A>().singleOrNull()?.let(mapFromAnnotation)
    }
}

/** Extract all annotations of type [A] and map each to [R] */
internal inline fun <reified A : Annotation, R : Any> KSAnnotated.getAllAnnotation(
    noinline manualExtraction: (KSAnnotation) -> R,
    noinline mapFromAnnotation: (A) -> R,
): Sequence<R> = catchKSPExceptions(
    fallback = { this.annotations.filter { filterAnnotation(it, A::class) }.map(manualExtraction) },
) {
    this.getAnnotationsByType<A>().map(mapFromAnnotation)
}

private val ktorGenParametersNames = ktorGenAnnotations.mapNotNull(KClass<*>::simpleName) + KTORGEN_KMP_FACTORY

internal fun extractAnnotationsFiltered(
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
            it.typeName != KotlinOptInClassName
        }

    return Triple(propagateAnnotations.toSet(), optIn.toSet(), deferredSymbols)
}

fun String.containsAnyOf(values: Iterable<String>, ignoreCase: Boolean = false): Boolean =
    values.any { this.contains(it, ignoreCase) }

context(timer: DiagnosticSender)
private fun mergeOptIns(
    existing: Set<AnnotationSpec>,
    extra: Set<AnnotationSpec>,
    optInAnnotationSpec: AnnotationSpec?,
): AnnotationSpec? {
    val existingClasses = existing.flatMap { it.members }.toSet()
    val extraClasses = extra.flatMap { it.members }.toSet()
    val optInAnnotation = optInAnnotationSpec?.members ?: emptyList()
    timer.addStep("Merging optIns, existing: $existingClasses, extra: $extraClasses, optIn: $optInAnnotation")

    val merged = (optInAnnotation + existingClasses + extraClasses).distinct().filterNot { codeBlock ->
        codeBlock.toString().containsAnyOf(ktorGenParametersNames)
    }
    timer.addStep("Merged optIns: $merged")

    return if (merged.isEmpty()) {
        null
    } else {
        AnnotationSpec
            .builder(KotlinOptInClassName)
            .apply { merged.forEach { addMember(it) } }
            .build()
    }
}

private fun AnnotationsOptions.mergeAnnotations(
    extra: Set<AnnotationSpec>,
    extraOptIns: Set<AnnotationSpec>,
): Set<AnnotationSpec> = (annotations + extra)
    .filterNot { ann ->
        extraOptIns.any { it.typeName == ann.typeName } || optIns.any { it.typeName == ann.typeName }
    }
    .toSet()

fun String?.replaceExact(oldValue: String, newValue: String): String = if (this == null || this == oldValue) {
    newValue
} else {
    this
}

private fun KSAnnotated.isAllowedForFunction(): Boolean {
    val targetAnnotation = annotations.firstOrNull {
        it.shortName.getShortName() == Target::class.simpleName!!
    } ?: return true // si no declara @Target, asumimos que es usable en cualquier sitio

    val allowedTargets = targetAnnotation.arguments
        .flatMap { it.value as? List<*> ?: emptyList<Any>() }
        .mapNotNull { it?.toString()?.substringAfterLast('.') } // e.g., "CLASS", "FUNCTION"

    return allowedTargets.any { it.equals(AnnotationTarget.FUNCTION.name, true) }
}

private fun KSAnnotation.isAllowedForFunction() =
    (annotationType.resolve().declaration as? KSClassDeclaration)?.isAllowedForFunction() ?: false

private fun KSType.isAllowedForFunction() = (this.declaration as? KSAnnotated)?.isAllowedForFunction() ?: false

private fun KClass<out Annotation>.isAllowedForFunction(): Boolean {
    // Obtenemos el @Target si existe
    val target = this.annotations.filterIsInstance<Target>().firstOrNull()
        ?: return true // si no declara @Target, asumimos que es usable en cualquier sitio

    // Revisamos si incluye FUNCTION en los targets
    return AnnotationTarget.FUNCTION in target.allowedTargets
}

infix fun <A, B, C> Pair<A, B>.to(that: C): Triple<A, B, C> = Triple(first, second, that)

/**
 * Extract annotations and merge with previous options because want to propagate
 * @receiver Target to extract annotations
 * @param options Previous options
 * @param extractForFunction If true, extract annotations for functions +
 * @return new Annotation Options and deferred symbols
 */
context(timer: DiagnosticSender)
private fun KSAnnotated.extractAndMergeAnnotations(
    options: AnnotationsOptions,
    extractForFunction: Boolean = false,
): Pair<AnnotationsOptions, List<KSAnnotated>> {
    timer.addStep("Extracting annotations and optIns to propagate")
    val (annotations, optIns, unresolvedSymbols) = extractAnnotationsFiltered(this)
    val (functionAnnotations, functionOptIn, symbols) = if (extractForFunction) {
        extractAnnotationsFiltered(this) { it.isAllowedForFunction() }
    } else {
        emptySet<AnnotationSpec>() to emptySet<AnnotationSpec>() to emptyList()
    }

    timer.addStep(
        "${symbols.size} unresolved symbols of function annotations. Found ${functionAnnotations.size} annotations, ${functionOptIn.size} optIns",
    )
    timer.addStep(
        "${unresolvedSymbols.size} unresolved symbols of interface annotations. Found ${annotations.size} annotations, ${optIns.size} optIns",
    )
    timer.addStep("Extracted annotations of declaration, merging optIns and options")

    val mergedAnnotations = options.mergeAnnotations(annotations, optIns)

    val mergedOptIn =
        mergeOptIns(optIns, options.optIns, mergedAnnotations.find { it.typeName == KotlinOptInClassName })

    return options.copy(
        annotations = mergedAnnotations.filterNot { ann -> ann.typeName == KotlinOptInClassName }.toSet(),
        factoryFunctionAnnotations = (options.factoryFunctionAnnotations + functionAnnotations + functionOptIn)
            .filterNot { ann ->
                options.optIns.any { it.typeName == ann.typeName } || (optIns.any { it.typeName == ann.typeName })
            }.toSet(),
        optInAnnotation = mergedOptIn,
        optIns = if (mergedOptIn != null) emptySet() else options.optIns,
    ).also {
        timer.addStep("Updated options with annotations and optIns propagated: $it")
    } to (unresolvedSymbols + symbols)
}

context(timer: DiagnosticSender)
internal fun KSAnnotated.extractAnnotationOptions(
    extractForFunction: Boolean = false,
): Pair<AnnotationsOptions, List<KSAnnotated>> {
    val ksClassDeclaration = this as? KSClassDeclaration
    val isDeclaredAtInterface = ksClassDeclaration?.isCompanionObject?.not() ?: false
    val isDeclaredAtCompanionObject = ksClassDeclaration?.isCompanionObject ?: false
    val isDeclaredAtFunction = this is KSFunctionDeclaration

    val annotationsOptions = this.getAnnotation<KtorGenAnnotationPropagation, AnnotationsOptions>(manualExtraction = {
        val annotationList = (it.getArgumentValueByName<List<KSType>>("annotations") ?: emptyList())
            .filterNot { t -> t.declaration.simpleName.getShortName() in ktorGenParametersNames }
            .asSequence()

        AnnotationsOptions(
            propagateAnnotations = it.getArgumentValueByName("propagateAnnotations") ?: true,
            annotations = annotationList
                .mapNotNull { a -> a.declaration.qualifiedName?.asString() }
                .map { n -> AnnotationSpec.builder(ClassName.bestGuess(n)).build() }
                .toSet(),
            optIns = (it.getArgumentValueByName<List<KSType>>("optInAnnotations") ?: emptyList())
                .mapNotNull { a -> a.declaration.qualifiedName?.asString() }
                .map { n -> AnnotationSpec.builder(ClassName.bestGuess(n)).build() }
                .toSet(),
            factoryFunctionAnnotations = annotationList
                .filter { a -> a.isAllowedForFunction() }
                .mapNotNull { a -> a.declaration.qualifiedName?.asString() }
                .map { n -> AnnotationSpec.builder(ClassName.bestGuess(n)).build() }
                .toSet()
                .plus(
                    (it.getArgumentValueByName<List<KSType>>("factoryFunctionAnnotations") ?: emptyList())
                        .asSequence()
                        .filterNot { t -> t.declaration.simpleName.getShortName() in ktorGenParametersNames }
                        .mapNotNull { a -> a.declaration.qualifiedName?.asString() }
                        .map { n -> AnnotationSpec.builder(ClassName.bestGuess(n)).build() }
                        .toSet(),
                ),
            optInAnnotation = null,
            isDeclaredAtInterface = isDeclaredAtInterface,
            isDeclaredAtCompanionObject = isDeclaredAtCompanionObject,
            isDeclaredAtFunctionLevel = isDeclaredAtFunction,
        )
    }) { annotation ->
        val annotationList = annotation.annotations.filterNot { it.simpleName!! in ktorGenParametersNames }
            .asSequence()

        AnnotationsOptions(
            propagateAnnotations = annotation.propagateAnnotations,
            annotations = annotationList
                .map { a -> AnnotationSpec.builder(a).build() }
                .toSet(),
            optIns = annotation.optInAnnotations
                .filterNot { it.simpleName!! in ktorGenParametersNames }
                .map { a -> AnnotationSpec.builder(a).build() }
                .toSet(),
            factoryFunctionAnnotations = annotationList
                .filter { a -> a.isAllowedForFunction() }
                .map { a -> AnnotationSpec.builder(a).build() }
                .plus(
                    annotation.factoryFunctionAnnotations
                        .filterNot { it.simpleName!! in ktorGenParametersNames }
                        .map { a -> AnnotationSpec.builder(a).build() },
                )
                .toSet(),
            optInAnnotation = null,
            isDeclaredAtInterface = isDeclaredAtInterface,
            isDeclaredAtCompanionObject = isDeclaredAtCompanionObject,
            isDeclaredAtFunctionLevel = isDeclaredAtFunction,
        )
    } ?: return AnnotationsOptions.NO_ANNOTATIONS to emptyList()

    timer.addStep("Retrieved annotations options: $annotationsOptions")

    if (annotationsOptions.propagateAnnotations) {
        return this.extractAndMergeAnnotations(
            options = annotationsOptions,
            extractForFunction = extractForFunction,
        )
    }

    return annotationsOptions to emptyList()
}

private fun KtorGenVisibility.toString(defaultVisibility: String): String = when (this) {
    KtorGenVisibility.DEFAULT -> defaultVisibility
    KtorGenVisibility.INTERNAL -> "internal"
    KtorGenVisibility.PUBLIC -> "public"
    KtorGenVisibility.PRIVATE -> "private"
}

internal fun KSAnnotation.enumArg(
    name: String,
    fallback: String,
    filter: (entry: String) -> String = { it.replaceExact("DEFAULT", fallback) },
): String {
    return (this.getArgumentValueByName<KSClassDeclaration>(name) ?: return fallback)
        .apply {
            checkImplementation(this.classKind == ClassKind.ENUM_CLASS || this.classKind == ClassKind.ENUM_ENTRY) {
                "'$name' must be an enum class or enum entry, but was ${this.classKind}"
            }
        }
        .simpleName
        .asString()
        .let(filter)
        .uppercase()
}

internal fun KSClassDeclaration.extractVisibilityOptions(defaultVisibility: String): VisibilityOptions =
    this.getAnnotation<KtorGenVisibilityControl, VisibilityOptions>(manualExtraction = {
        val visibilityModifier = it.enumArg("visibilityModifier", defaultVisibility)

        VisibilityOptions(
            classVisibilityModifier = it.enumArg("classVisibilityModifier", visibilityModifier),
            constructorVisibilityModifier = it.enumArg("constructorVisibilityModifier", visibilityModifier),
            factoryFunctionVisibilityModifier = it.enumArg("factoryFunctionVisibilityModifier", visibilityModifier),
        )
    }) {
        val visibilityModifier = it.visibilityModifier
            .toString(defaultVisibility)
            .uppercase()

        VisibilityOptions(
            classVisibilityModifier = it.classVisibilityModifier.toString(visibilityModifier).uppercase(),
            constructorVisibilityModifier = it.constructorVisibilityModifier.toString(visibilityModifier).uppercase(),
            factoryFunctionVisibilityModifier = it.factoryFunctionVisibilityModifier.toString(
                visibilityModifier,
            ).uppercase(),
        )
    } ?: VisibilityOptions.default(defaultVisibility)
