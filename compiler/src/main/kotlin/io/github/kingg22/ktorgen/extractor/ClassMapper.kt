package io.github.kingg22.ktorgen.extractor

import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.getVisibility
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ksp.toAnnotationSpec
import com.squareup.kotlinpoet.ksp.toKModifier
import com.squareup.kotlinpoet.ksp.toTypeName
import io.github.kingg22.ktorgen.DiagnosticTimer
import io.github.kingg22.ktorgen.KtorGenLogger
import io.github.kingg22.ktorgen.core.KtorGen
import io.github.kingg22.ktorgen.core.KtorGenExperimental
import io.github.kingg22.ktorgen.model.ClassData
import io.github.kingg22.ktorgen.model.DefaultOptions
import io.github.kingg22.ktorgen.model.GenOptions
import io.github.kingg22.ktorgen.model.KTORGEN_DEFAULT_VALUE
import io.github.kingg22.ktorgen.model.KTOR_CLIENT_CALL_BODY
import io.github.kingg22.ktorgen.model.KTOR_CLIENT_REQUEST
import io.github.kingg22.ktorgen.model.KTOR_DECODE_URL_QUERY
import io.github.kingg22.ktorgen.model.KTOR_URL_TAKE_FROM

class ClassMapper : DeclarationMapper {
    override fun mapToModel(
        declaration: KSClassDeclaration,
        timer: (String) -> DiagnosticTimer.DiagnosticSender,
    ): ClassData {
        val interfaceName = declaration.simpleName.getShortName()
        val timer = timer("Class Mapper for [$interfaceName]")
        return timer.work { _ ->
            val imports = mutableSetOf<String>()

            val packageName = declaration.packageName.asString()

            val filteredSupertypes = declaration.superTypes.filterNot { it.toTypeName() == ANY }
            timer.addStep("Retrieved all supertypes")

            val companionObject = declaration.declarations
                .filterIsInstance<KSClassDeclaration>()
                .any { it.isCompanionObject }
            timer.addStep("Have companion object: $companionObject")

            val properties = declaration.getDeclaredProperties()
            timer.addStep("Retrieved all properties")

            var options = extractKtorGen(declaration)
                ?: DefaultOptions(
                    generatedName = "_${interfaceName}Impl",
                    visibilityModifier = declaration.getVisibility().name,
                )
            timer.addStep("Retrieved @KtorGen options. BasePath: '${options.basePath}'")

            var (annotations, optIn) = extractAnnotationsFiltered(declaration)
            timer.addStep("Retrieved the rest of annotations and optIns")

            if (optIn != null && options.optIns.isNotEmpty()) {
                optIn = optIn.toBuilder()
                    .addMember(
                        (1..options.optIns.size).joinToString { "%T::class" },
                        *options.optIns.map { it.typeName }.toTypedArray(),
                    ).build()
            } else if (optIn == null && options.optIns.isNotEmpty()) {
                optIn = AnnotationSpec.builder(ClassName("kotlin", "OptIn"))
                    .addMember(
                        (1..options.optIns.size).joinToString { "%T::class" },
                        *options.optIns.map { it.typeName }.toTypedArray(),
                    ).build()
            }
            annotations = (options.annotationsToPropagate + annotations).filterNot { it in options.optIns }.toSet()
            options =
                options.copy(annotationsToPropagate = annotations, optInAnnotation = optIn) as GenOptions.GenTypeOption

            val functions = declaration.getDeclaredFunctions().map { func ->
                DeclarationFunctionMapper.DEFAULT.mapToModel(func, imports::add, options.basePath) {
                    timer.createTask(it)
                }.also {
                    timer.addStep("Processed function: ${it.name}")
                }
            }.toList()

            if (functions.isNotEmpty()) {
                // basic imports
                imports.addAll(
                    arrayOf(
                        KTOR_CLIENT_CALL_BODY,
                        KTOR_CLIENT_REQUEST,
                        KTOR_URL_TAKE_FROM,
                        KTOR_DECODE_URL_QUERY,
                    ),
                )
            }

            timer.addStep("Processed all functions")

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
            }
        }
    }

    private fun extractAnnotationsFiltered(
        declaration: KSClassDeclaration,
    ): Pair<Set<AnnotationSpec>, AnnotationSpec?> {
        val optIn = declaration.annotations
            .filterNot { it.shortName.getShortName() == KtorGen::class.simpleName!! }
            .filter { it.shortName.getShortName() == "OptIn" }
            .map(KSAnnotation::toAnnotationSpec)
            .toSet()

        val propagateAnnotations = declaration.annotations
            .filterNot { it.shortName.getShortName() == KtorGen::class.simpleName!! }
            .map(KSAnnotation::toAnnotationSpec)
            .filterNot { it in optIn }
            .toSet()

        return propagateAnnotations to optIn.singleOrNull()
    }

    @OptIn(KtorGenExperimental::class)
    private fun extractKtorGen(interfaceDeclaration: KSClassDeclaration): GenOptions.GenTypeOption? =
        interfaceDeclaration.getAnnotation<KtorGen, GenOptions.GenTypeOption>(manualExtraction = {
            DefaultOptions(
                generatedName =
                it.getArgumentValueByName<String>("name")?.takeUnless { n -> n == KTORGEN_DEFAULT_VALUE }
                    ?: "_${interfaceDeclaration.simpleName.getShortName()}Impl",

                goingToGenerate = it.getArgumentValueByName("generate") ?: true,
                basePath = it.getArgumentValueByName("basePath") ?: "",
                generateTopLevelFunction = it.getArgumentValueByName("generateTopLevelFunction") ?: true,
                generateCompanionExtFunction = it.getArgumentValueByName("generateCompanionExtFunction") ?: false,
                generateHttpClientExtension = it.getArgumentValueByName("generateHttpClientExtension") ?: false,

                propagateAnnotations = it.getArgumentValueByName("propagateAnnotations") ?: true,
                annotationsToPropagate = it.getArgumentValueByName<List<KSType>>("annotations")
                    ?.mapNotNull { a -> a.declaration.qualifiedName?.asString() }
                    ?.map { n -> ClassName.bestGuess(n) }
                    ?.map { a -> AnnotationSpec.builder(a).build() }
                    ?.toSet()
                    ?: emptySet(),
                optIns = it.getArgumentValueByName<List<KSType>>("optInAnnotations")
                    ?.mapNotNull { a -> a.declaration.qualifiedName?.asString() }
                    ?.map { n -> ClassName.bestGuess(n) }
                    ?.map { a -> AnnotationSpec.builder(a).build() }
                    ?.toSet()
                    ?: emptySet(),

                visibilityModifier = it.getArgumentValueByName("visibilityModifier") ?: "public",
                customFileHeader = it.getArgumentValueByName("customFileHeader") ?: "",
                customClassHeader = it.getArgumentValueByName("customClassHeader") ?: "",
            )
        }) {
            DefaultOptions(
                generatedName = it.name.takeUnless { n -> n == KTORGEN_DEFAULT_VALUE }
                    ?: "_${interfaceDeclaration.simpleName.getShortName()}Impl",
                goingToGenerate = it.generate,
                basePath = it.basePath,
                generateTopLevelFunction = it.generateTopLevelFunction,
                generateCompanionExtFunction = it.generateCompanionExtFunction,
                generateHttpClientExtension = it.generateHttpClientExtension,

                propagateAnnotations = it.propagateAnnotations,
                annotationsToPropagate = it.annotations.map { a -> AnnotationSpec.builder(a).build() }.toSet(),
                optIns = it.optInAnnotations.map { a -> AnnotationSpec.builder(a).build() }.toSet(),

                visibilityModifier = it.visibilityModifier,
                customFileHeader = it.customFileHeader,
                customClassHeader = it.customClassHeader,
            )
        }
}
