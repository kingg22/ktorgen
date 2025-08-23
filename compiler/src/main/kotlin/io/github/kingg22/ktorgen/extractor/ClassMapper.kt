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
import io.github.kingg22.ktorgen.model.KTOR_URL_TAKE_FROM

class ClassMapper : DeclarationMapper {
    override fun mapToModel(declaration: KSClassDeclaration, timer: (String) -> DiagnosticSender): ClassData {
        val interfaceName = declaration.simpleName.getShortName()
        return timer("Class Mapper for [$interfaceName]").work { timer ->
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
                ?: run {
                    if (companionObject) {
                        extractKtorGen(
                            declaration.declarations.filterIsInstance<KSClassDeclaration>()
                                .first { it.isCompanionObject },
                        )?.let { return@run it }
                    }
                    ClassGenerationOptions.default(
                        generatedName = "_${interfaceName}Impl",
                        visibilityModifier = declaration.getVisibility().name,
                    )
                }
            timer.addStep(
                "Retrieved @KtorGen options. BasePath: '${options.basePath}', propagate annotations: ${options.propagateAnnotations}",
            )

            if (options.propagateAnnotations) {
                var (annotations, optIn, functionAnnotation) = extractAnnotationsOfClassFiltered(declaration)
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
                annotations = (options.annotations + annotations).filterNot { it in options.optIns }.toSet()
                options = options.copy(
                    annotationsToPropagate = annotations,
                    extensionFunctionAnnotation = options.extensionFunctionAnnotation + functionAnnotation,
                    optInAnnotation = optIn,
                )
            }

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

    private fun extractAnnotationsOfClassFiltered(
        declaration: KSClassDeclaration,
    ): Triple<Set<AnnotationSpec>, AnnotationSpec?, Set<AnnotationSpec>> {
        val optIn = declaration.annotations
            .filter { it.shortName.getShortName() == "OptIn" }
            .map { it.toAnnotationSpec() }
            .toSet()

        val propagateAnnotations = declaration.annotations
            .filterNot { it.shortName.getShortName() == KtorGen::class.simpleName!! }
            .map { it.toAnnotationSpec() }
            .toSet()

        val functionAnnotation = declaration.annotations
            .filterNot { it.shortName.getShortName() == "OptIn" }
            .filter { it.isAllowedForFunction() }
            .map { it.toAnnotationSpec() }
            .toSet()

        return Triple(propagateAnnotations, optIn.singleOrNull(), functionAnnotation)
    }

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
    private fun extractKtorGen(interfaceDeclaration: KSClassDeclaration) =
        interfaceDeclaration.getAnnotation<KtorGen, ClassGenerationOptions>(manualExtraction = {
            ClassGenerationOptions(
                generatedName =
                it.getArgumentValueByName<String>("name")?.takeUnless { n -> n == KTORGEN_DEFAULT_VALUE }
                    ?: "_${interfaceDeclaration.simpleName.getShortName()}Impl",

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

                visibilityModifier = it.getArgumentValueByName<String>("visibilityModifier")?.replace(
                    KTORGEN_DEFAULT_VALUE,
                    interfaceDeclaration.getVisibility().name,
                )?.uppercase() ?: interfaceDeclaration.getVisibility().name,
                classVisibilityModifier = it.getArgumentValueByName<String>("classVisibilityModifier")?.replace(
                    KTORGEN_DEFAULT_VALUE,
                    interfaceDeclaration.getVisibility().name,
                )?.uppercase() ?: KTORGEN_DEFAULT_VALUE,
                constructorVisibilityModifier =
                it.getArgumentValueByName<String>("constructorVisibilityModifier")?.replace(
                    KTORGEN_DEFAULT_VALUE,
                    interfaceDeclaration.getVisibility().name,
                )?.uppercase() ?: KTORGEN_DEFAULT_VALUE,
                functionVisibilityModifier = it.getArgumentValueByName<String>("functionVisibilityModifier")?.replace(
                    KTORGEN_DEFAULT_VALUE,
                    interfaceDeclaration.getVisibility().name,
                )?.uppercase() ?: KTORGEN_DEFAULT_VALUE,

                customFileHeader = it.getArgumentValueByName<String>("customFileHeader")?.replace(
                    KTORGEN_DEFAULT_VALUE,
                    interfaceDeclaration.getVisibility().name,
                ) ?: KTORG_GENERATED_FILE_COMMENT,
                customClassHeader = it.getArgumentValueByName("customClassHeader") ?: "",
            ).copy { options ->
                options.copy(
                    classVisibilityModifier = options.classVisibilityModifier.replace(
                        KTORGEN_DEFAULT_VALUE,
                        options.visibilityModifier,
                    ).uppercase(),
                    constructorVisibilityModifier = options.constructorVisibilityModifier.replace(
                        KTORGEN_DEFAULT_VALUE,
                        options.visibilityModifier,
                    ).uppercase(),
                    functionVisibilityModifier = options.functionVisibilityModifier.replace(
                        KTORGEN_DEFAULT_VALUE,
                        options.visibilityModifier,
                    ).uppercase(),
                )
            }
        }) {
            ClassGenerationOptions(
                generatedName = it.name.takeUnless { n -> n == KTORGEN_DEFAULT_VALUE }
                    ?: "_${interfaceDeclaration.simpleName.getShortName()}Impl",
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

                visibilityModifier = it.visibilityModifier.replace(
                    KTORGEN_DEFAULT_VALUE,
                    interfaceDeclaration.getVisibility().name,
                ).uppercase(),
                classVisibilityModifier = it.classVisibilityModifier,
                constructorVisibilityModifier = it.constructorVisibilityModifier,
                functionVisibilityModifier = it.functionVisibilityModifier,

                customFileHeader = it.customFileHeader.replace(KTORGEN_DEFAULT_VALUE, KTORG_GENERATED_FILE_COMMENT),
                customClassHeader = it.customClassHeader,
            ).copy { options ->
                options.copy(
                    classVisibilityModifier = options.classVisibilityModifier.replace(
                        KTORGEN_DEFAULT_VALUE,
                        options.visibilityModifier,
                    ).uppercase(),
                    constructorVisibilityModifier = options.constructorVisibilityModifier.replace(
                        KTORGEN_DEFAULT_VALUE,
                        options.visibilityModifier,
                    ).uppercase(),
                    functionVisibilityModifier = options.functionVisibilityModifier.replace(
                        KTORGEN_DEFAULT_VALUE,
                        options.visibilityModifier,
                    ).uppercase(),
                )
            }
        }
}
