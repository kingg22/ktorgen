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
import io.github.kingg22.ktorgen.KtorGenLogger.Companion.COOKIE_ON_FUNCTION_WITHOUT_VALUE
import io.github.kingg22.ktorgen.checkImplementation
import io.github.kingg22.ktorgen.core.KtorGenAnnotationPropagation
import io.github.kingg22.ktorgen.core.KtorGenVisibility
import io.github.kingg22.ktorgen.core.KtorGenVisibilityControl
import io.github.kingg22.ktorgen.http.Cookie
import io.github.kingg22.ktorgen.model.KTORGEN_DEFAULT_VALUE
import io.github.kingg22.ktorgen.model.KotlinOptInClassName
import io.github.kingg22.ktorgen.model.annotations.CookieValues
import io.github.kingg22.ktorgen.model.annotations.KTORGEN_KMP_FACTORY
import io.github.kingg22.ktorgen.model.annotations.ktorGenAnnotations
import io.github.kingg22.ktorgen.model.annotations.removeWhitespace
import io.github.kingg22.ktorgen.model.options.AnnotationsOptions
import io.github.kingg22.ktorgen.model.options.VisibilityOptions
import io.github.kingg22.ktorgen.requireNotNull
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
internal inline fun <T> catchKSPExceptions(fallback: (Exception) -> T, block: () -> T): T {
    contract {
        callsInPlace(fallback, InvocationKind.AT_MOST_ONCE)
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    return try {
        block()
    } catch (e: NoSuchElementException) {
        fallback(e)
    } catch (e: KSTypeNotPresentException) {
        fallback(e)
    } catch (e: KSTypesNotPresentException) {
        fallback(e)
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
): Triple<Set<AnnotationSpec>, AnnotationSpec?, List<KSAnnotated>> {
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

    checkImplementation(optIn.size <= 1) {
        "Found more than one optIn annotation: $optIn, on declaration: $declaration"
    }

    return Triple(propagateAnnotations.toSet(), optIn.singleOrNull(), deferredSymbols)
}

fun String.containsAnyOf(values: Iterable<String>, ignoreCase: Boolean = false): Boolean =
    values.any { this.contains(it, ignoreCase) }

context(timer: DiagnosticSender)
private fun mergeOptIns(extra: Set<AnnotationSpec>, vararg optInAnnotationSpec: AnnotationSpec?): AnnotationSpec? {
    val extraClasses = extra.flatMap { it.members }.toSet()
    val optInAnnotation = optInAnnotationSpec.mapNotNull { it?.members }.flatten()
    timer.addStep("Merging optIns, existing: $optInAnnotation, extra: $extraClasses")

    val merged = (optInAnnotation + extraClasses).distinct().filterNot { codeBlock ->
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
    vararg extraOptIns: AnnotationSpec,
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
    infix fun <A, B, C> Pair<A, B>.to(that: C): Triple<A, B, C> = Triple(first, second, that)

    timer.addStep("Extracting annotations and optIns to propagate")
    val (annotations, optInAnnotation, unresolvedSymbols) = extractAnnotationsFiltered(this)
    val (functionAnnotations, functionOptIn, symbols) = if (extractForFunction) {
        extractAnnotationsFiltered(this) { it.isAllowedForFunction() }
    } else {
        emptySet<AnnotationSpec>() to null to emptyList()
    }

    timer.addStep(
        "${symbols.size} unresolved symbols of function annotations. Found ${functionAnnotations.size} annotations, optIn: $functionOptIn",
    )
    timer.addStep(
        "${unresolvedSymbols.size} unresolved symbols of interface annotations. Found ${annotations.size} annotations, optIn: $optInAnnotation",
    )
    timer.addStep("Extracted annotations of declaration, merging optIns and options")

    val mergedAnnotations = if (optInAnnotation != null) {
        options.mergeAnnotations(annotations, optInAnnotation)
    } else {
        annotations
    }

    val mergedOptIn =
        mergeOptIns(options.optIns, mergedAnnotations.find { it.typeName == KotlinOptInClassName }, optInAnnotation)

    return options.copy(
        annotations = mergedAnnotations.filterNot { ann -> ann.typeName == KotlinOptInClassName }.toSet(),
        factoryFunctionAnnotations = (options.factoryFunctionAnnotations + functionAnnotations).let { annotationSpecs ->
            if (functionOptIn != null) {
                annotationSpecs + functionAnnotations
            } else {
                annotationSpecs
            }
        }.filterNot { ann ->
            options.optIns.any { it.typeName == ann.typeName } ||
                (optInAnnotation != null && optInAnnotation.typeName == ann.typeName)
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

// a lot of try-catch because default values is not working for KMP builds https://github.com/google/ksp/issues/2356

/**
 * This is a mapper function, handle try-catch of default values, die when [parameterName] is required, but is null.
 * @receiver the [Cookie] annotation to convert to [CookieValues]
 * @param timer the [DiagnosticSender] to use for logging
 * @param parameterName the name of the parameter, used as a fallback value
 * @return [CookieValues] cleaned
 */
context(timer: DiagnosticSender)
fun Cookie.toCookieValues(parameterName: String? = null): CookieValues {
    // clean here because don't need validation, otherwise don't do it
    var isParameter = false

    val finalValue = catchKSPExceptions(fallback = { e ->
        isParameter = true
        timer.addStep("Caught exception while parsing cookie value, assuming it's a parameter")
        timer.requireNotNull(parameterName, COOKIE_ON_FUNCTION_WITHOUT_VALUE, cause = e)
    }) {
        when (val cleanValue = value.removeWhitespace()) {
            KTORGEN_DEFAULT_VALUE -> {
                isParameter = true
                timer.requireNotNull(parameterName, COOKIE_ON_FUNCTION_WITHOUT_VALUE)
            }

            else -> {
                isParameter = false
                cleanValue
            }
        }
    }

    operator fun Cookie.PairString.component1() = first
    operator fun Cookie.PairString.component2() = second

    return CookieValues(
        name = catchKSPExceptions(fallback = { e ->
            timer.die("Cookie name is required", cause = e)
        }) { name.removeWhitespace() },
        value = finalValue,
        isValueParameter = isParameter,
        maxAge = catchKSPExceptions(fallback = { _ ->
            timer.addStep("Cookie maxAge is not set, assuming it's 0")
            0
        }) { maxAge },
        expiresTimestamp = catchKSPExceptions(fallback = { _ ->
            timer.addStep("Cookie expiresTimestamp is not set, assuming it's null")
            null
        }) {
            expiresTimestamp.takeIf { it != -1L }
        },
        domain = catchKSPExceptions(fallback = { _ ->
            timer.addStep("Cookie domain is not set, assuming it's null")
            null
        }) {
            domain.removeWhitespace().takeIf { it.isNotBlank() }
        },
        path = catchKSPExceptions(fallback = { _ ->
            timer.addStep("Cookie path is not set, assuming it's null")
            null
        }) {
            path.removeWhitespace().takeIf { it.isNotBlank() }
        },
        secure = catchKSPExceptions(fallback = {
            timer.addStep("Cookie secure is not set, assuming it's false")
            false
        }) { secure },
        httpOnly = catchKSPExceptions(fallback = {
            timer.addStep("Cookie httpOnly is not set, assuming it's false")
            false
        }) { httpOnly },
        extensions = catchKSPExceptions(fallback = {
            timer.addStep("Cookie extensions are not set, assuming it's empty")
            emptyMap()
        }) {
            extensions.associate { (key, value) ->
                key.removeWhitespace() to value.removeWhitespace().takeIf { it.isNotBlank() }
            }
        },
    )
}
