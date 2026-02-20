package io.github.kingg22.ktorgen.generator

import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.KModifier.SUSPEND
import com.squareup.kotlinpoet.KModifier.VARARG
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.toClassName
import io.github.kingg22.ktorgen.DiagnosticSender
import io.github.kingg22.ktorgen.applyIf
import io.github.kingg22.ktorgen.applyIfNotNull
import io.github.kingg22.ktorgen.model.ClassData
import io.github.kingg22.ktorgen.model.FunctionData
import io.github.kingg22.ktorgen.model.ParameterData
import io.github.kingg22.ktorgen.model.options.AnnotationsOptions
import io.github.kingg22.ktorgen.work

internal class KotlinpoetGenerator : KtorGenGenerator {
    private val constructorGenerator = ConstructorGenerator()
    private val factoryFunctionGenerator = FactoryFunctionGenerator()
    private lateinit var functionBodyGenerator: FunctionBodyGenerator
    private val expectFunctionProcessor = ExpectFunctionProcessor()

    context(timer: DiagnosticSender)
    override fun generate(
        classData: ClassData,
        partDataKtor: KSType?,
        listType: KSType,
        arrayType: KSType,
    ): List<FileSpec> = timer.work {
        factoryFunctionGenerator.clean()
        // Initialize builders
        val classBuilder = createClassBuilder(classData)
        val fileBuilder = createFileBuilder(classData)

        timer.addStep("Creating class for ${classData.interfaceName} to ${classData.options.generatedName}")

        // Generate constructor and properties
        val (constructor, properties, httpClient) =
            constructorGenerator.generatePrimaryConstructorAndProperties(
                classData,
                KModifier.valueOf(classData.visibilityOptions.constructorVisibilityModifier.uppercase()),
            )
        // Needs to refresh the reference of Ktor PartData
        functionBodyGenerator = FunctionBodyGenerator(
            httpClient,
            ParameterBodyGenerator(partDataKtor, listType, arrayType),
        )

        // Generate functions
        val functions = classData.functions.filter { it.options.goingToGenerate }.toList()
        timer.addStep("Generated primary constructor and properties, going to generate ${functions.size} functions")

        // Build class structure
        classBuilder.buildClassStructure(classData, constructor, properties, functions)

        // Generate factory functions
        val factoryFunctions = generateFactoryFunctions(classData, constructor.build().parameters)
        factoryFunctionGenerator.clean()
        fileBuilder.addFunctions(factoryFunctions)

        // Process expects functions if any
        val extraFiles = processExpectFunctions(classData, onAddFunction = { expectFunction ->
            factoryFunctions.firstOrNull { funSpec ->
                // compare using build. Builders don't have equals implemented
                val factoryFunction = funSpec.toBuilder()
                    .addModifiers(KModifier.ACTUAL)
                    .apply { annotations.clear() }.build()
                val expectFunctionWithoutAnnotations = expectFunction.toBuilder()
                    .apply { annotations.clear() }.build()

                factoryFunction == expectFunctionWithoutAnnotations
            }?.let { factoryFunction ->
                // Remove normal factory function and add expect one
                fileBuilder.members.remove(factoryFunction)
                fileBuilder.addFunction(expectFunction)
            } ?: fileBuilder.addFunction(expectFunction)
        })

        // Build final result
        timer.addStep("Finished file generation with ${extraFiles.size + 1} files")
        listOf(fileBuilder.addType(classBuilder.build()).build()) + extraFiles
    }

    private fun FileSpec.Builder.addDefaultConfig() = apply {
        addAnnotation(
            AnnotationSpec.builder(Suppress::class)
                .useSiteTarget(AnnotationSpec.UseSiteTarget.FILE)
                .addMember("%S", "REDUNDANT_VISIBILITY_MODIFIER")
                .addMember("%S", "unused")
                .addMember("%S", "UNUSED_IMPORT")
                .addMember("%S", "warnings")
                .addMember("%S", "RemoveSingleExpressionStringTemplate")
                .addMember("%S", "ktlint")
                .addMember("%S", "detekt:all")
                .addMember("%S", "LocalVariableName")
                .addMember("%S", "PropertyName")
                .addMember("%S", "ClassName")
                .build(),
        ).addAnnotation(GeneratedAnnotation)
            .indent("    ") // use 4 spaces https://pinterest.github.io/ktlint/latest/rules/standard/#indentation
    }

    private fun AnnotationsOptions.buildAnnotations(): Set<AnnotationSpec> {
        val annotations = annotations.toMutableSet()

        // si tengo optIns pendientes y no hay optInAnnotation unificado → generar uno
        if (optIns.isNotEmpty() && optInAnnotation == null) {
            annotations += AnnotationSpec.builder(ClassName("kotlin", "OptIn"))
                .addMember(
                    optIns.joinToString { "%T::class" },
                    *optIns.map { it.typeName }.toTypedArray(),
                ).build()
        } else if (optInAnnotation != null) {
            annotations += optInAnnotation
        }
        return annotations + GeneratedAnnotation
    }

    /** This fills the primary constructor and super interfaces */
    private fun TypeSpec.Builder.addSuperInterfaces(classData: ClassData, primaryConstructor: FunSpec.Builder) = apply {
        classData.superClasses
            .forEach { ref ->
                val ksType = ref.resolve()
                val decl = ksType.declaration
                val className = ksType.toClassName()
                val name = decl.simpleName.asString()
                val parameterName = name.replaceFirstChar { it.lowercase() }

                primaryConstructor.addParameter(parameterName, className)
                addSuperinterface(className, CodeBlock.of(parameterName))
            }
    }

    private fun createClassBuilder(classData: ClassData): TypeSpec.Builder = TypeSpec
        .classBuilder(classData.options.generatedName)
        .addModifiers(KModifier.valueOf(classData.visibilityOptions.classVisibilityModifier.uppercase()))
        .addSuperinterface(ClassName(classData.packageNameString, classData.interfaceName))
        .addKdoc(classData.options.customClassHeader)
        .addAnnotations(classData.annotationsOptions.buildAnnotations())
        .addOriginatingKSFile(classData.ksFile)

    private fun createFileBuilder(classData: ClassData): FileSpec.Builder = FileSpec
        .builder(classData.packageNameString, classData.options.customFileName)
        .addDefaultConfig()
        .addFileComment(classData.options.customFileHeader)

    private fun TypeSpec.Builder.buildClassStructure(
        classData: ClassData,
        constructor: FunSpec.Builder,
        properties: List<PropertySpec>,
        functions: List<FunctionData>,
    ) = apply {
        addSuperInterfaces(classData, constructor)
            .primaryConstructor(constructor.build())
            .addProperties(properties)
            .addFunctions(functions.map { generateFunction(it) })
    }

    private fun generateFunction(func: FunctionData): FunSpec = FunSpec.builder(func.name)
        .addModifiers(func.modifierSet)
        .returns(func.returnTypeData.typeName)
        .addAnnotations(func.annotationsOptions.buildAnnotations())
        .applyIfNotNull(func.options.customHeader) { addKdoc(it) }
        .applyIf(func.isSuspend) { addModifiers(SUSPEND) }
        .apply {
            func.parameterDataList.forEach { param ->
                addParameter(createParameterSpec(param))
            }
        }
        .addCode(functionBodyGenerator.generateFunctionBody(func))
        .build()

    private fun createParameterSpec(param: ParameterData): ParameterSpec = ParameterSpec.builder(
        name = param.nameString,
        type = param.typeData.typeName,
        modifiers = buildList { if (param.isVararg) add(VARARG) },
    ).addAnnotations(param.nonKtorgenAnnotations.toList())
        .applyIfNotNull(param.optInAnnotation) { addAnnotation(it) }
        .build()

    context(_: DiagnosticSender)
    private fun generateFactoryFunctions(classData: ClassData, constructorParams: List<ParameterSpec>): List<FunSpec> {
        val classAnnotations = classData.annotationsOptions.buildAnnotations()
        val optInAnnotation = classAnnotations.firstOrNull { it.typeName == ClassName("kotlin", "OptIn") }
        val functionAnnotation =
            setOfNotNull(GeneratedAnnotation, optInAnnotation) + classData.annotationsOptions.factoryFunctionAnnotations
        val functionVisibilityModifier = KModifier.valueOf(
            classData.visibilityOptions.factoryFunctionVisibilityModifier.uppercase(),
        )

        return factoryFunctionGenerator.generateFactoryFunctions(
            classData,
            constructorParams,
            functionAnnotation,
            functionVisibilityModifier,
        )
    }

    context(_: DiagnosticSender)
    private fun processExpectFunctions(classData: ClassData, onAddFunction: (FunSpec) -> Unit): List<FileSpec> =
        expectFunctionProcessor.processExpectFunctions(
            classData,
            constructorGenerator.computeConstructorSignature(classData),
            onAddFunction,
        ).map { it.addDefaultConfig().build() }

    private companion object {
        private val GeneratedAnnotation = AnnotationSpec.builder(
            ClassName("io.github.kingg22.ktorgen.core", "Generated"),
        ).build()
    }
}
