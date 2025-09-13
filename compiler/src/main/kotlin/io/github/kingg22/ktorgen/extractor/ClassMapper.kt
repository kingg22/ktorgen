package io.github.kingg22.ktorgen.extractor

import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.getVisibility
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ksp.toKModifier
import com.squareup.kotlinpoet.ksp.toTypeName
import io.github.kingg22.ktorgen.DiagnosticSender
import io.github.kingg22.ktorgen.KtorGenLogger
import io.github.kingg22.ktorgen.core.KtorGen
import io.github.kingg22.ktorgen.core.KtorGenExperimental
import io.github.kingg22.ktorgen.model.ClassData
import io.github.kingg22.ktorgen.model.ClassGenerationOptions
import io.github.kingg22.ktorgen.model.KTORGEN_DEFAULT_VALUE
import io.github.kingg22.ktorgen.model.KTORG_GENERATED_FILE_COMMENT
import io.github.kingg22.ktorgen.model.KTOR_CLIENT_CALL_BODY
import io.github.kingg22.ktorgen.model.KTOR_CLIENT_REQUEST
import io.github.kingg22.ktorgen.model.KTOR_DECODE_URL_QUERY
import io.github.kingg22.ktorgen.model.KTOR_REQUEST_TAKE_FROM
import io.github.kingg22.ktorgen.model.KTOR_URL_TAKE_FROM
import io.github.kingg22.ktorgen.requireNotNull
import io.github.kingg22.ktorgen.work
import kotlin.collections.filterNot
import kotlin.collections.plus
import kotlin.collections.plusAssign
import kotlin.collections.toSet

class ClassMapper : DeclarationMapper {
    override fun mapToModel(
        declaration: KSClassDeclaration,
        timer: (String) -> DiagnosticSender,
    ): Pair<ClassData?, List<KSAnnotated>> {
        val interfaceName = declaration.simpleName.getShortName()
        val deferredSymbols = mutableListOf<KSAnnotated>()
        val imports = mutableSetOf<String>()
        return timer("Class Mapper for [$interfaceName]").work { timer ->
            val companionObject = declaration.declarations
                .filterIsInstance<KSClassDeclaration>()
                .any { it.isCompanionObject }

            var options = extractKtorGen(declaration, interfaceName)
                ?: run {
                    if (companionObject) {
                        extractKtorGen(
                            declaration.declarations.filterIsInstance<KSClassDeclaration>()
                                .first { it.isCompanionObject },
                            interfaceName,
                        )?.let { return@run it }
                    }
                    ClassGenerationOptions.default(
                        generatedName = "_${interfaceName}Impl",
                        visibilityModifier = declaration.getVisibility().name,
                    )
                }

            timer.addStep(
                "Retrieved @KtorGen options, going to propagate annotations? ${options.propagateAnnotations}. Options: $options",
            )

            if (options.propagateAnnotations) {
                options = updateClassGenerationOptions(declaration, timer, deferredSymbols, options)
            }

            if (!options.goingToGenerate) {
                timer.addStep("Early finish processing, not going to generate this interface.")
                return@work null to emptyList()
            }

            val packageName = declaration.packageName.asString()

            val filteredSupertypes = filterSupertypes(declaration.superTypes, deferredSymbols, timer, interfaceName)
            timer.addStep("Retrieved all supertypes")

            timer.addStep("Have companion object: $companionObject")

            val properties = declaration.getDeclaredProperties()

            properties.forEach {
                val type = it.type.resolve()
                if (type.isError) {
                    timer.addStep("Found error type reference of property: $type")
                    deferredSymbols += type.declaration
                }
            }

            timer.addStep("Retrieved all properties")

            val functions = declaration.getDeclaredFunctions().mapNotNull { func ->
                val (functionData, symbols) = DeclarationFunctionMapper.DEFAULT.mapToModel(
                    func,
                    imports::add,
                    options.basePath,
                    timer::createTask,
                )
                functionData?.let {
                    timer.addStep("Processed function: ${it.name}")
                    return@mapNotNull it
                }
                timer.addStep("${symbols.size} unresolved symbols of function: ${func.simpleName.getShortName()}")
                deferredSymbols += symbols
                return@mapNotNull null
            }.toList()

            if (functions.isNotEmpty()) {
                // basic imports
                imports.addAll(
                    arrayOf(
                        KTOR_CLIENT_CALL_BODY,
                        KTOR_CLIENT_REQUEST,
                        KTOR_URL_TAKE_FROM,
                        KTOR_REQUEST_TAKE_FROM,
                        KTOR_DECODE_URL_QUERY,
                    ),
                )
            }

            timer.addStep("Processed all functions")

            if (deferredSymbols.isNotEmpty()) {
                timer.addStep("Found deferred symbols, skipping to next round of processing")
                return@work null to deferredSymbols
            }

            // an operation terminal of sequences must be in one site
            ClassData(
                ksClassDeclaration = declaration,
                interfaceName = interfaceName,
                packageNameString = packageName,
                functions = functions,
                imports = imports,
                superClasses = filteredSupertypes.toList(),
                properties = properties.toList(),
                modifierSet = declaration.modifiers.mapNotNull { it.toKModifier() }.toSet(),
                ksFile = timer.requireNotNull(
                    declaration.containingFile,
                    KtorGenLogger.INTERFACE_NOT_HAVE_FILE + interfaceName,
                    declaration,
                ),
                haveCompanionObject = companionObject,
                options = options,
            ).also {
                timer.addStep("Mapper complete of ${it.interfaceName} to ${it.generatedName}")
            } to emptyList()
        }
    }

    private fun updateClassGenerationOptions(
        declaration: KSClassDeclaration,
        timer: DiagnosticSender,
        deferredSymbols: MutableList<KSAnnotated>,
        options: ClassGenerationOptions,
    ): ClassGenerationOptions {
        val (annotations, optIns, unresolvedSymbols) = extractAnnotationsFiltered(declaration)
        val (functionAnnotations, functionOptIn, symbols) = extractFunctionAnnotationsFiltered(declaration)

        timer.addStep("Retrieved the rest of annotations and optIns")
        timer.addStep("${symbols.size} unresolved symbols of function annotations")
        timer.addStep("${unresolvedSymbols.size} unresolved symbols of interface annotations")
        deferredSymbols += symbols
        deferredSymbols += unresolvedSymbols

        val mergedOptIn = mergeOptIns(optIns, options.optIns)

        val mergedAnnotations = options.mergeAnnotations(annotations, optIns)

        return options.copy(
            annotationsToPropagate = mergedAnnotations,
            extensionFunctionAnnotation =
            (options.extensionFunctionAnnotation + functionAnnotations + functionOptIn)
                .filterNot { ann ->
                    options.optIns.any { it.typeName == ann.typeName } ||
                        (optIns.any { it.typeName == ann.typeName })
                }.toSet(),
            optInAnnotation = mergedOptIn,
            optIns = if (mergedOptIn != null) emptySet() else options.optIns,
        ).also {
            timer.addStep("Updated options with annotations and optIns propagated: $it")
        }
    }

    private fun filterSupertypes(
        supertypes: Sequence<KSTypeReference>,
        deferredSymbols: MutableList<KSAnnotated>,
        timer: DiagnosticSender,
        interfaceName: String,
    ) = supertypes.filterNot {
        val type = it.resolve()
        if (type.isError) {
            // Verificar si la superinterfaz tiene @KtorGen con generate=false
            val superDeclaration = type.declaration as? KSClassDeclaration
            if (superDeclaration != null) {
                val superOptions = extractKtorGen(superDeclaration, interfaceName)
                if (superOptions?.generate == false) {
                    true // Ignorar completamente
                } else {
                    timer.addStep("Found error type reference of superinterface: $type")
                    deferredSymbols += type.declaration
                    true
                }
            } else {
                timer.addStep("Found error type reference of super interface: $type")
                deferredSymbols += type.declaration
                true
            }
        } else {
            it.toTypeName() == ANY
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun extractFunctionAnnotationsFiltered(declaration: KSClassDeclaration) =
        extractAnnotationsFiltered(declaration) { it.isAllowedForFunction() }

    private fun KSAnnotation.isAllowedForFunction(): Boolean {
        val type = annotationType.resolve().declaration as? KSClassDeclaration ?: return false
        val targetAnnotation = type.annotations.firstOrNull {
            it.shortName.getShortName() == Target::class.simpleName!!
        } ?: return true // si no declara @Target, asumimos que es usable en cualquier sitio

        val allowedTargets = targetAnnotation.arguments
            .flatMap { it.value as? List<*> ?: emptyList<Any>() }
            .mapNotNull { it?.toString()?.substringAfterLast('.') } // e.g., "CLASS", "FUNCTION"

        return allowedTargets.any { it.equals(AnnotationTarget.FUNCTION.name, true) }
    }

    @OptIn(KtorGenExperimental::class)
    private fun extractKtorGen(kSClassDeclaration: KSClassDeclaration, interfaceName: String) =
        kSClassDeclaration.getAnnotation<KtorGen, ClassGenerationOptions>(manualExtraction = {
            val visibilityModifier = it.getArgumentValueByName<String>("visibilityModifier")?.replace(
                KTORGEN_DEFAULT_VALUE,
                kSClassDeclaration.getVisibility().name,
            )?.uppercase() ?: kSClassDeclaration.getVisibility().name

            ClassGenerationOptions(
                generatedName =
                it.getArgumentValueByName<String>("name")?.takeUnless { n -> n == KTORGEN_DEFAULT_VALUE }
                    ?: "_${interfaceName}Impl",

                goingToGenerate = it.getArgumentValueByName("generate") ?: true,
                basePath = it.getArgumentValueByName("basePath") ?: "",
                generateTopLevelFunction = it.getArgumentValueByName("generateTopLevelFunction") ?: true,
                generateCompanionExtFunction = it.getArgumentValueByName("generateCompanionExtFunction") ?: false,
                generateHttpClientExtension = it.getArgumentValueByName("generateHttpClientExtension") ?: false,

                propagateAnnotations = it.getArgumentValueByName("propagateAnnotations") ?: true,
                annotations = it.getArgumentValueByName<List<KSType>>("annotations")
                    ?.mapNotNull { a -> a.declaration.qualifiedName?.asString() }
                    ?.map { n -> AnnotationSpec.builder(ClassName.bestGuess(n)).build() }
                    ?.toSet()
                    ?: emptySet(),
                optIns = it.getArgumentValueByName<List<KSType>>("optInAnnotations")
                    ?.mapNotNull { a -> a.declaration.qualifiedName?.asString() }
                    ?.map { n -> AnnotationSpec.builder(ClassName.bestGuess(n)).build() }
                    ?.toSet()
                    ?: emptySet(),
                extensionFunctionAnnotation = it.getArgumentValueByName<List<KSType>>("functionAnnotations")
                    ?.mapNotNull { a -> a.declaration.qualifiedName?.asString() }
                    ?.map { n -> AnnotationSpec.builder(ClassName.bestGuess(n)).build() }
                    ?.toSet()
                    ?: emptySet(),

                classVisibilityModifier = it.getArgumentValueByName<String>("classVisibilityModifier")?.replace(
                    KTORGEN_DEFAULT_VALUE,
                    visibilityModifier,
                )?.uppercase() ?: visibilityModifier,
                constructorVisibilityModifier =
                it.getArgumentValueByName<String>("constructorVisibilityModifier")?.replace(
                    KTORGEN_DEFAULT_VALUE,
                    visibilityModifier,
                )?.uppercase() ?: visibilityModifier,
                functionVisibilityModifier = it.getArgumentValueByName<String>("functionVisibilityModifier")?.replace(
                    KTORGEN_DEFAULT_VALUE,
                    visibilityModifier,
                )?.uppercase() ?: visibilityModifier,

                customFileHeader = it.getArgumentValueByName<String>("customFileHeader")?.replace(
                    KTORGEN_DEFAULT_VALUE,
                    kSClassDeclaration.getVisibility().name,
                ) ?: KTORG_GENERATED_FILE_COMMENT,
                customClassHeader = it.getArgumentValueByName("customClassHeader") ?: "",
            )
        }) {
            val visibilityModifier = it.visibilityModifier.replace(
                KTORGEN_DEFAULT_VALUE,
                kSClassDeclaration.getVisibility().name,
            ).uppercase()

            ClassGenerationOptions(
                generatedName = it.name.takeUnless { n -> n == KTORGEN_DEFAULT_VALUE } ?: "_${interfaceName}Impl",
                goingToGenerate = it.generate,
                basePath = it.basePath,
                generateTopLevelFunction = it.generateTopLevelFunction,
                generateCompanionExtFunction = it.generateCompanionExtFunction,
                generateHttpClientExtension = it.generateHttpClientExtension,

                propagateAnnotations = it.propagateAnnotations,
                annotations = it.annotations.map { a -> AnnotationSpec.builder(a).build() }.toSet(),
                optIns = it.optInAnnotations.map { a -> AnnotationSpec.builder(a).build() }.toSet(),
                extensionFunctionAnnotation = it.functionAnnotations.map { a ->
                    AnnotationSpec.builder(a).build()
                }.toSet(),

                classVisibilityModifier = it.classVisibilityModifier.replace(
                    KTORGEN_DEFAULT_VALUE,
                    visibilityModifier,
                ).uppercase(),
                constructorVisibilityModifier = it.constructorVisibilityModifier.replace(
                    KTORGEN_DEFAULT_VALUE,
                    visibilityModifier,
                ).uppercase(),
                functionVisibilityModifier = it.functionVisibilityModifier.replace(
                    KTORGEN_DEFAULT_VALUE,
                    visibilityModifier,
                ).uppercase(),

                customFileHeader = it.customFileHeader.replace(KTORGEN_DEFAULT_VALUE, KTORG_GENERATED_FILE_COMMENT),
                customClassHeader = it.customClassHeader,
            )
        }
}
