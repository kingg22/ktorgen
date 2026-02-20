package io.github.kingg22.ktorgen.generator

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.toClassName
import io.github.kingg22.ktorgen.DiagnosticSender
import io.github.kingg22.ktorgen.model.ClassData
import io.github.kingg22.ktorgen.model.HttpClientClassName

/** **NOTE**: This class is NOT stateless! */
internal class FactoryFunctionGenerator {
    private val generatedFactories = linkedSetOf<FactoryFunctionKey>()

    fun clean() = generatedFactories.clear()

    context(_: DiagnosticSender)
    fun generateFactoryFunctions(
        classData: ClassData,
        constructorParams: List<ParameterSpec>,
        functionAnnotation: Set<AnnotationSpec>,
        functionVisibilityModifier: KModifier,
    ): List<FunSpec> {
        val functions = mutableListOf<FunSpec>()

        if (classData.generateTopLevelFunction) {
            generateTopLevelFunction(
                classData,
                constructorParams,
                functionAnnotation,
                functionVisibilityModifier,
            )?.let { functions.add(it) }
        }

        if (classData.generateCompanionExtFunction && classData.ksCompanionObject != null) {
            generateCompanionFunction(
                classData = classData,
                companionObjectClassName = classData.ksCompanionObject.toClassName(),
                constructorParams = constructorParams,
                functionAnnotation = functionAnnotation,
                functionVisibilityModifier = functionVisibilityModifier,
            )?.let { functions.add(it) }
        }

        if (classData.generateHttpClientExtension) {
            generateHttpClientFunction(
                classData,
                constructorParams,
                functionAnnotation,
                functionVisibilityModifier,
            )?.let { functions.add(it) }
        }

        return functions
    }

    context(timer: DiagnosticSender)
    private fun generateTopLevelFunction(
        classData: ClassData,
        constructorParams: List<ParameterSpec>,
        functionAnnotation: Set<AnnotationSpec>,
        functionVisibilityModifier: KModifier,
    ): FunSpec? {
        val function = generateTopLevelFactoryFunction(
            classNameImpl = ClassName(classData.packageNameString, classData.options.generatedName),
            interfaceClassName = ClassName(classData.packageNameString, classData.interfaceName),
            constructorParams = constructorParams,
        ).addModifiers(functionVisibilityModifier)
            .addAnnotations(functionAnnotation)
            .addOriginatingKSFile(classData.ksFile)

        return registerFactoryFunction(
            FactoryFunctionKey(
                name = classData.interfaceName,
                packageName = classData.packageNameString,
                paramTypes = constructorParams.map { it.type },
                function,
            ),
            "top level function factory",
        )?.build()
    }

    context(_: DiagnosticSender)
    private fun generateCompanionFunction(
        classData: ClassData,
        companionObjectClassName: ClassName,
        constructorParams: List<ParameterSpec>,
        functionAnnotation: Set<AnnotationSpec>,
        functionVisibilityModifier: KModifier,
    ): FunSpec? = registerFactoryFunction(
        FactoryFunctionKey(
            name = classData.interfaceName,
            packageName = classData.packageNameString,
            paramTypes = constructorParams.map { it.type },
            generateCompanionExtensionFunction(
                companionClassName = companionObjectClassName,
                classNameImpl = ClassName(classData.packageNameString, classData.options.generatedName),
                interfaceClassName = ClassName(classData.packageNameString, classData.interfaceName),
                constructorParams = constructorParams,
            ).addModifiers(functionVisibilityModifier)
                .addAnnotations(functionAnnotation)
                .addOriginatingKSFile(classData.ksFile),
        ),
        "companion extension function factory",
    )?.build()

    context(_: DiagnosticSender)
    private fun generateHttpClientFunction(
        classData: ClassData,
        constructorParams: List<ParameterSpec>,
        functionAnnotation: Set<AnnotationSpec>,
        functionVisibilityModifier: KModifier,
    ): FunSpec? {
        val function = generateHttpClientExtensionFunction(
            httpClientClassName = HttpClientClassName,
            classNameImpl = ClassName(classData.packageNameString, classData.options.generatedName),
            interfaceClassName = ClassName(classData.packageNameString, classData.interfaceName),
            constructorParams = constructorParams,
        ).addModifiers(functionVisibilityModifier)
            .addAnnotations(functionAnnotation)
            .addOriginatingKSFile(classData.ksFile)

        return registerFactoryFunction(
            FactoryFunctionKey(
                name = classData.interfaceName,
                packageName = classData.packageNameString,
                paramTypes = constructorParams.map { it.type },
                function,
            ),
            "http client extension function factory",
        )?.build()
    }

    context(timer: DiagnosticSender)
    private fun registerFactoryFunction(key: FactoryFunctionKey, functionType: String): FunSpec.Builder? =
        if (generatedFactories.add(key)) {
            timer.addStep("Added $functionType")
            key.functionSpecBuilder
        } else {
            timer.addWarning("Duplicate $functionType detected")
            null
        }

    private fun generateTopLevelFactoryFunction(
        classNameImpl: ClassName,
        interfaceClassName: ClassName,
        constructorParams: List<ParameterSpec>,
    ) = FunSpec.builder(interfaceClassName.simpleName)
        .returns(interfaceClassName)
        .addParameters(constructorParams.map { it.toBuilder(it.name.removePrefix("_")).build() })
        .addStatement(
            RETURN_TYPE_LITERAL,
            classNameImpl,
            constructorParams.joinToString { it.name.removePrefix("_") },
        )

    private fun generateCompanionExtensionFunction(
        companionClassName: ClassName,
        classNameImpl: ClassName,
        interfaceClassName: ClassName,
        constructorParams: List<ParameterSpec>,
    ) = FunSpec.builder("create")
        .receiver(companionClassName)
        .returns(interfaceClassName)
        .addParameters(constructorParams.map { it.toBuilder(it.name.removePrefix("_")).build() })
        .addStatement(
            RETURN_TYPE_LITERAL,
            classNameImpl,
            constructorParams.joinToString { it.name.removePrefix("_") },
        )

    private fun generateHttpClientExtensionFunction(
        httpClientClassName: ClassName,
        classNameImpl: ClassName,
        interfaceClassName: ClassName,
        constructorParams: List<ParameterSpec>,
    ): FunSpec.Builder {
        val paramsExclHttpClient = constructorParams.filter { it.type != httpClientClassName }
        return FunSpec.builder("create${interfaceClassName.simpleName}")
            .receiver(httpClientClassName)
            .returns(interfaceClassName)
            .addParameters(paramsExclHttpClient)
            .addStatement(
                RETURN_TYPE_LITERAL,
                classNameImpl,
                buildString {
                    append("this")
                    if (paramsExclHttpClient.isNotEmpty()) {
                        append(", ")
                        append(paramsExclHttpClient.joinToString { it.name })
                    }
                },
            )
    }

    /** Key to identify unique factory functions */
    private data class FactoryFunctionKey(
        val name: String,
        val packageName: String,
        val paramTypes: List<TypeName>,
        val functionSpecBuilder: FunSpec.Builder,
    )

    private companion object {
        private const val RETURN_TYPE_LITERAL = "return %T(%L)"
    }
}
