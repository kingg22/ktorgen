package io.github.kingg22.ktorgen.generator

import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.toAnnotationSpec
import com.squareup.kotlinpoet.ksp.toKModifier
import com.squareup.kotlinpoet.ksp.toTypeName
import io.github.kingg22.ktorgen.DiagnosticSender
import io.github.kingg22.ktorgen.model.ClassData
import io.github.kingg22.ktorgen.model.annotations.KTORGEN_KMP_FACTORY
import io.github.kingg22.ktorgen.requireNotNull

internal class ExpectFunctionProcessor {
    /** Process all expect functions and generate actual implementations */
    context(_: DiagnosticSender)
    fun processExpectFunctions(
        classData: ClassData,
        constructorSignature: List<TypeName>,
        onAddFunction: (FunSpec) -> Unit,
    ): List<FileSpec.Builder> {
        return classData.expectFunctions.groupBy {
            // Group expect functions by package to generate one file per package
            it.packageName.asString()
        }.flatMap { (_, functions) ->
            functions.mapNotNull { func ->
                // Validate signature matches constructor
                val expectParamTypes = func.parameters.map { it.type.resolve().toTypeName() }
                if (!validateFunctionSignature(expectParamTypes, constructorSignature, func, classData)) {
                    return@mapNotNull null
                }

                // Generate actual function
                generateActualFunctionForKmp(
                    classData = classData,
                    func = func,
                    onAddFunction = onAddFunction,
                )
            }
        }
    }

    /** Validates that the expect function signature matches the constructor */
    context(logger: DiagnosticSender)
    private fun validateFunctionSignature(
        expectParamTypes: List<TypeName>,
        constructorSignature: List<TypeName>,
        func: KSFunctionDeclaration,
        classData: ClassData,
    ): Boolean {
        if (constructorSignature.size != expectParamTypes.size) {
            logger.addError(
                "Constructor mismatch for ${classData.options.generatedName}. Expect function '${func.simpleName.asString()}' has ${expectParamTypes.size} parameter(s) but generated constructor expects ${constructorSignature.size} parameter(s)",
                func,
            )
            return false
        }

        // Validate parameter types match
        constructorSignature.zip(expectParamTypes).forEachIndexed { index, (expected, actual) ->
            if (expected != actual) {
                logger.addError(
                    "Parameter type mismatch at position $index for expect function '${func.simpleName.asString()}'. Expected: $expected, Found: $actual",
                    func,
                )
                return false
            }
        }

        return true
    }

    /** Generates an actual function implementation for KMP expect function */
    context(logger: DiagnosticSender)
    private fun generateActualFunctionForKmp(
        classData: ClassData,
        func: KSFunctionDeclaration,
        onAddFunction: (FunSpec) -> Unit,
    ): FileSpec.Builder? {
        val name = func.simpleName.asString()
        val pkg = func.packageName.asString()

        // Generate actual function
        val implClassName = ClassName(classData.packageNameString, classData.options.generatedName)
        val returnClassName = ClassName(classData.packageNameString, classData.interfaceName)

        val funBuilder = FunSpec.builder(name)
            .returns(returnClassName)
            .addModifiers(KModifier.ACTUAL)
            .addOriginatingKSFile(
                logger.requireNotNull(func.containingFile, "Expect function has no containing file: $name", func),
            )

        // Add all modifiers except EXPECT, preserving order
        val modifiers = func.modifiers
            .mapNotNull { it.toKModifier() }
            .filter { it != KModifier.EXPECT }
        modifiers.forEach { funBuilder.addModifiers(it) }

        // Add all annotations in the exact order they appear
        for (annotation in func.annotations) {
            // Skip the kmp factory annotation because is only a common source annotation
            if (annotation.shortName.asString() == KTORGEN_KMP_FACTORY) continue
            funBuilder.addAnnotation(annotation.toAnnotationSpec(true))
        }

        // Add parameters with exact names, types, and order
        func.parameters.forEach { param ->
            val paramName = param.name?.asString() ?: run {
                logger.addError("Parameter without name in expect function '$name'", param)
                return null
            }
            val paramType = param.type.resolve().toTypeName()

            val paramBuilder = ParameterSpec.builder(paramName, paramType)

            // Copy parameter annotations
            param.annotations.forEach { annotation ->
                paramBuilder.addAnnotation(annotation.toAnnotationSpec(true))
            }

            funBuilder.addParameter(paramBuilder.build())
        }

        // Generate function body - call implementation constructor
        val argsJoin = func.parameters.joinToString { it.name?.asString() ?: "p" }
        funBuilder.addStatement("return %T(%L)", implClassName, argsJoin)

        val function = funBuilder.build()
        // Determine file name and location
        val fileName = if (pkg == classData.packageNameString) {
            onAddFunction(function)
            return null
        } else {
            // Different package - must create separate file
            "_${name}KmpActual"
        }

        // Build file
        return FileSpec.builder(pkg, fileName).addFunction(function)
    }
}
