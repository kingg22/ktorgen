package io.github.kingg22.ktorgen.extractor

import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.getVisibility
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.ksp.toKModifier
import com.squareup.kotlinpoet.ksp.toTypeName
import io.github.kingg22.ktorgen.DiagnosticSender
import io.github.kingg22.ktorgen.KtorGenLogger
import io.github.kingg22.ktorgen.core.KtorGen
import io.github.kingg22.ktorgen.core.KtorGenCompanionExtFactory
import io.github.kingg22.ktorgen.core.KtorGenHttpClientExtFactory
import io.github.kingg22.ktorgen.core.KtorGenTopLevelFactory
import io.github.kingg22.ktorgen.model.ClassData
import io.github.kingg22.ktorgen.model.KTORGEN_DEFAULT_VALUE
import io.github.kingg22.ktorgen.model.KTORG_GENERATED_FILE_COMMENT
import io.github.kingg22.ktorgen.model.options.AnnotationsOptions
import io.github.kingg22.ktorgen.model.options.ClassGenerationOptions
import io.github.kingg22.ktorgen.model.options.Factories
import io.github.kingg22.ktorgen.model.options.Factories.CompanionExtension
import io.github.kingg22.ktorgen.model.options.Factories.HttpClientExtension
import io.github.kingg22.ktorgen.model.options.Factories.TopLevelFactory
import io.github.kingg22.ktorgen.require
import io.github.kingg22.ktorgen.requireNotNull
import io.github.kingg22.ktorgen.work

internal class ClassMapper : DeclarationMapper {
    context(timer: DiagnosticSender)
    override fun mapToModel(
        declaration: KSClassDeclaration,
        expectFunctions: List<KSFunctionDeclaration>,
    ): DeclarationMapper.ClassDataOrDeferredSymbols = timer.work {
        timer.require(
            !declaration.modifiers.contains(Modifier.EXTERNAL),
            KtorGenLogger.EXTERNAL_DECLARATION_NOT_ALLOWED,
            declaration,
        )

        timer.require(
            !declaration.modifiers.contains(Modifier.EXPECT),
            KtorGenLogger.INTERFACE_IS_EXPECTED,
            declaration,
        )

        val companionObject = declaration.declarations
            .filterIsInstance<KSClassDeclaration>()
            .firstOrNull { it.isCompanionObject }

        val interfaceName = declaration.simpleName.getShortName()

        val options = extractKtorGen(declaration, interfaceName)
            ?: run {
                if (companionObject != null) {
                    extractKtorGen(companionObject, interfaceName)?.let {
                        return@run it
                    }
                }
                ClassGenerationOptions.default(
                    generatedName = "${interfaceName}Impl",
                    isDeclaredAtCompanionObject = false,
                    isDeclaredAtInterface = false,
                )
            }
        timer.addStep("Retrieved @KtorGen options: $options")

        if (!options.goingToGenerate) {
            timer.addStep("Skipping, not going to generate this interface.")
            return@work null to emptyList()
        }
        val deferredSymbols = mutableListOf<KSAnnotated>()

        // TODO add annotation options flag when is repeated on interface + companion object
        var (annotationsOptions, symbols) = declaration.extractAnnotationOptions()
        deferredSymbols += symbols
        if (companionObject != null && annotationsOptions == AnnotationsOptions.NO_ANNOTATIONS) {
            companionObject.extractAnnotationOptions().let { (options, symbols) ->
                annotationsOptions = options
                deferredSymbols += symbols
            }
        }

        val packageName = declaration.packageName.asString()

        val filteredSupertypes = filterSupertypes(declaration.superTypes, deferredSymbols, interfaceName).toList()
        timer.addStep("Retrieved all supertypes")

        timer.addStep("Have companion object: $companionObject")

        val properties = declaration.getDeclaredProperties().toList()

        properties.asSequence().map { it.type.resolve() }.filter { it.isError }.forEach { type ->
            timer.addStep("Found error type reference of property: $type")
            // Investigate the correct approach type.resolve.declaration or type or type.declaration
            deferredSymbols += type.declaration
        }

        timer.addStep("Retrieved all properties")

        val functions = declaration.getDeclaredFunctions().mapNotNull { func ->
            val (functionData, symbols) = context(
                timer.createTask(DeclarationFunctionMapper.DEFAULT.getLoggerNameFor(func)),
            ) {
                DeclarationFunctionMapper.DEFAULT.mapToModel(func, options.basePath)
            }
            functionData?.let {
                timer.addStep("Processed function: ${it.name}")
                return@mapNotNull it
            }
            timer.addStep("${symbols.size} unresolved symbols of function: ${func.simpleName.getShortName()}")
            deferredSymbols += symbols
            return@mapNotNull null
        }.toList()

        timer.addStep("Processed all functions")

        if (deferredSymbols.isNotEmpty()) {
            timer.addStep("Found deferred symbols, skipping to next round of processing")
            return@work null to deferredSymbols
        }

        ClassData(
            packageNameString = packageName,
            interfaceName = interfaceName,
            qualifiedName = declaration.qualifiedName?.asString() ?: run {
                timer.addStep(
                    "No qualified name found, building qualified name manually using package name and interface name. This can lead errors in deferred symbols processing.",
                )
                "$packageName.$interfaceName"
            },
            functions = functions.asSequence(),
            ksFile = timer.requireNotNull(
                declaration.containingFile,
                KtorGenLogger.INTERFACE_NOT_HAVE_FILE + interfaceName,
                declaration,
            ),
            ksInterface = declaration,
            superClasses = filteredSupertypes.asSequence(),
            properties = properties.asSequence(),
            modifierSet = declaration.modifiers.mapNotNull { it.toKModifier() }.toSet(),
            ksCompanionObject = companionObject,
            options = options,
            factories = buildSet {
                if (expectFunctions.isNotEmpty()) {
                    add(Factories.KmpExpectActual(expectFunctions))
                }
                extractTopLevelFactory(declaration, interfaceName)?.let { add(it) }
                extractCompanionExtFactory(declaration, interfaceName)?.let { add(it) }
                extractHttpClientExtFactory(declaration, interfaceName)?.let { add(it) }
                if (companionObject != null) {
                    extractTopLevelFactory(companionObject, interfaceName)?.let { add(it) }
                    extractCompanionExtFactory(companionObject, interfaceName)?.let { add(it) }
                    extractHttpClientExtFactory(companionObject, interfaceName)?.let { add(it) }
                }
            },
            annotationsOptions = annotationsOptions,
            visibilityOptions = declaration.extractVisibilityOptions(declaration.getVisibility().name),
        ).also { classData ->
            timer.addStep("Mapper complete of ${classData.interfaceName} to ${classData.options.generatedName}")
        } to emptyList()
    }

    private fun extractTopLevelFactory(declaration: KSClassDeclaration, interfaceName: String): TopLevelFactory? {
        val isInInterface = !declaration.isCompanionObject
        val isInCompanionObject = declaration.isCompanionObject

        return declaration.getAnnotation<KtorGenTopLevelFactory, TopLevelFactory>(manualExtraction = {
            TopLevelFactory(
                it.getArgumentValueByName<String>("name").replaceExact(KTORGEN_DEFAULT_VALUE, interfaceName),
                isInInterface,
                isInCompanionObject,
            )
        }) {
            TopLevelFactory(
                it.name.replaceExact(KTORGEN_DEFAULT_VALUE, interfaceName),
                isInInterface,
                isInCompanionObject,
            )
        }
    }

    private fun extractCompanionExtFactory(
        declaration: KSClassDeclaration,
        interfaceName: String,
    ): CompanionExtension? {
        val isInInterface = !declaration.isCompanionObject
        val isInCompanionObject = declaration.isCompanionObject

        return declaration.getAnnotation<KtorGenCompanionExtFactory, CompanionExtension>(manualExtraction = {
            CompanionExtension(
                it.getArgumentValueByName<String>("name").replaceExact(KTORGEN_DEFAULT_VALUE, interfaceName),
                isInInterface,
                isInCompanionObject,
            )
        }) {
            CompanionExtension(
                it.name.replaceExact(KTORGEN_DEFAULT_VALUE, interfaceName),
                isInInterface,
                isInCompanionObject,
            )
        }
    }

    private fun extractHttpClientExtFactory(
        declaration: KSClassDeclaration,
        interfaceName: String,
    ): HttpClientExtension? {
        val isInInterface = !declaration.isCompanionObject
        val isInCompanionObject = declaration.isCompanionObject

        return declaration.getAnnotation<KtorGenHttpClientExtFactory, HttpClientExtension>(manualExtraction = {
            HttpClientExtension(
                it.getArgumentValueByName<String>("name").replaceExact(KTORGEN_DEFAULT_VALUE, interfaceName),
                isInInterface,
                isInCompanionObject,
            )
        }) {
            HttpClientExtension(
                it.name.replaceExact(KTORGEN_DEFAULT_VALUE, interfaceName),
                isInInterface,
                isInCompanionObject,
            )
        }
    }

    context(timer: DiagnosticSender)
    private fun filterSupertypes(
        supertypes: Sequence<KSTypeReference>,
        deferredSymbols: MutableList<KSAnnotated>,
        interfaceName: String,
    ) = supertypes.filterNot {
        val type = it.resolve()
        if (type.isError) {
            // Verificar si la superinterfaz tiene @KtorGen con generate=false
            val superDeclaration = type.declaration as? KSClassDeclaration
            if (superDeclaration != null) {
                val superOptions = extractKtorGen(superDeclaration, interfaceName)
                if (superOptions?.goingToGenerate == false) {
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

    private fun extractKtorGen(
        kSClassDeclaration: KSClassDeclaration,
        interfaceName: String,
    ): ClassGenerationOptions? {
        val defaultGeneratedName = "${interfaceName}Impl"
        val isDeclaredAtInterface = !kSClassDeclaration.isCompanionObject
        val isDeclaredAtCompanionObject = kSClassDeclaration.isCompanionObject

        return kSClassDeclaration.getAnnotation<KtorGen, ClassGenerationOptions>(manualExtraction = {
            val generatedName = it.getArgumentValueByName<String>("name")
                .replaceExact(KTORGEN_DEFAULT_VALUE, defaultGeneratedName)

            ClassGenerationOptions(
                generatedName = generatedName,
                goingToGenerate = it.getArgumentValueByName("generate") ?: true,
                basePath = it.getArgumentValueByName("basePath") ?: "",

                customFileHeader = it.getArgumentValueByName<String>("customFileHeader")?.replaceExact(
                    KTORGEN_DEFAULT_VALUE,
                    kSClassDeclaration.getVisibility().name,
                ) ?: KTORG_GENERATED_FILE_COMMENT,
                customClassHeader = it.getArgumentValueByName("customClassHeader") ?: "",
                customFileName = it.getArgumentValueByName<String>("customFileName")
                    .replaceExact(KTORGEN_DEFAULT_VALUE, generatedName),
                isDeclaredAtInterface = isDeclaredAtInterface,
                isDeclaredAtCompanionObject = isDeclaredAtCompanionObject,
            )
        }) {
            val generatedName = it.name.replaceExact(KTORGEN_DEFAULT_VALUE, defaultGeneratedName)

            ClassGenerationOptions(
                generatedName = generatedName,
                goingToGenerate = it.generate,
                basePath = it.basePath,
                customFileHeader = it.customFileHeader.replaceExact(
                    KTORGEN_DEFAULT_VALUE,
                    KTORG_GENERATED_FILE_COMMENT,
                ),
                customClassHeader = it.customClassHeader,
                customFileName = it.customFileName.replaceExact(KTORGEN_DEFAULT_VALUE, generatedName),
                isDeclaredAtInterface = isDeclaredAtInterface,
                isDeclaredAtCompanionObject = isDeclaredAtCompanionObject,
            )
        }
    }
}
