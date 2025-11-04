package io.github.kingg22.ktorgen.generator

import com.google.devtools.ksp.symbol.KSClassDeclaration
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
import io.github.kingg22.ktorgen.requireNotNull

/** **NOTE**: This class is NOT stateless! */
class FactoryFunctionGenerator {
    private val generatedFactories = linkedSetOf<FactoryFunctionKey>()

    fun generateFactoryFunctions(
        classData: ClassData,
        timer: DiagnosticSender,
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
                timer,
            )?.let { functions.add(it) }
        }

        if (classData.generateCompanionExtFunction && classData.haveCompanionObject) {
            generateCompanionFunction(
                classData,
                constructorParams,
                functionAnnotation,
                functionVisibilityModifier,
                timer,
            )?.let { functions.add(it) }
        }

        if (classData.generateHttpClientExtension) {
            generateHttpClientFunction(
                classData,
                constructorParams,
                functionAnnotation,
                functionVisibilityModifier,
                timer,
            )?.let { functions.add(it) }
        }

        return functions
    }

    private fun generateTopLevelFunction(
        classData: ClassData,
        constructorParams: List<ParameterSpec>,
        functionAnnotation: Set<AnnotationSpec>,
        functionVisibilityModifier: KModifier,
        timer: DiagnosticSender,
    ): FunSpec? {
        val function = generateTopLevelFactoryFunction(
            classNameImpl = ClassName(classData.packageNameString, classData.generatedName),
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
            timer,
            "top level function factory",
        )?.build()
    }

    private fun generateCompanionFunction(
        classData: ClassData,
        constructorParams: List<ParameterSpec>,
        functionAnnotation: Set<AnnotationSpec>,
        functionVisibilityModifier: KModifier,
        timer: DiagnosticSender,
    ): FunSpec? {
        val companionName = timer.requireNotNull(
            classData.ksClassDeclaration.declarations
                .filterIsInstance<KSClassDeclaration>()
                .firstOrNull { it.isCompanionObject },
            "${classData.interfaceName} don't have companion object",
            classData.ksClassDeclaration,
        ).toClassName()

        val function = generateCompanionExtensionFunction(
            companionClassName = companionName,
            classNameImpl = ClassName(classData.packageNameString, classData.generatedName),
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
            timer,
            "companion extension function factory",
        )?.build()
    }

    private fun generateHttpClientFunction(
        classData: ClassData,
        constructorParams: List<ParameterSpec>,
        functionAnnotation: Set<AnnotationSpec>,
        functionVisibilityModifier: KModifier,
        timer: DiagnosticSender,
    ): FunSpec? {
        val function = generateHttpClientExtensionFunction(
            httpClientClassName = HttpClientClassName,
            classNameImpl = ClassName(classData.packageNameString, classData.generatedName),
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
            timer,
            "http client extension function factory",
        )?.build()
    }

    private fun registerFactoryFunction(
        key: FactoryFunctionKey,
        timer: DiagnosticSender,
        functionType: String,
    ): FunSpec.Builder? = if (generatedFactories.add(key)) {
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

    companion object {
        private const val RETURN_TYPE_LITERAL = "return %T(%L)"
    }
}
