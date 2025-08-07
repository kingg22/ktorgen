package io.github.kingg22.ktorgen.extractor

import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.getVisibility
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.AnnotationSpec
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
import kotlin.reflect.KClass

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
            timer.addStep("Have companion object for: $companionObject")

            val properties = declaration.getDeclaredProperties()

            timer.addStep("Retrieved all properties")
            val options = extractKtorGen(declaration)
                ?: DefaultOptions(
                    generatedName = "_${interfaceName}Impl",
                    visibilityModifier = declaration.getVisibility().name,
                )
            timer.addStep("Retrieved @KtorGen options. BasePath: '${options.basePath}'")

            val functions = declaration.getDeclaredFunctions().map { func ->
                DeclarationFunctionMapper.DEFAULT.mapToModel(func, imports::add, options.basePath) {
                    timer.createTask(it)
                }.also {
                    timer.addStep("Processed function: ${it.name}")
                }
            }.toList().also {
                if (it.isNotEmpty()) {
                    imports.addAll(
                        arrayOf(
                            KTOR_CLIENT_CALL_BODY,
                            KTOR_CLIENT_REQUEST,
                            KTOR_URL_TAKE_FROM,
                            KTOR_DECODE_URL_QUERY,
                        ),
                    )
                }
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
                ),
                annotationSet = declaration.annotations
                    .filterNot { it.shortName.getShortName() == KtorGen::class.simpleName!! }.toSet(),
                haveCompanionObject = companionObject,
                options = options,
            ).also {
                timer.addStep("Mapper complete of ${it.interfaceName} to ${it.generatedName}")
            }
        }
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
                annotationsToPropagate =
                it.getArgumentValueByName<Array<KClass<out Annotation>>>("annotations")
                    ?.map { a -> AnnotationSpec.builder(a) }
                    ?.toSet()
                    ?: emptySet(),
                optIns = it.getArgumentValueByName<Array<KClass<out Annotation>>>("optInAnnotations")
                    ?.map { a -> AnnotationSpec.builder(a) }
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
                annotationsToPropagate = it.annotations.map { a -> AnnotationSpec.builder(a) }.toSet(),
                optIns = it.optInAnnotations.map { a -> AnnotationSpec.builder(a) }.toSet(),

                visibilityModifier = it.visibilityModifier,
                customFileHeader = it.customFileHeader,
                customClassHeader = it.customClassHeader,
            )
        }
}
