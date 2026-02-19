package io.github.kingg22.ktorgen.validator.validators

import com.google.devtools.ksp.symbol.KSNode
import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.KModifier
import io.github.kingg22.ktorgen.KtorGenLogger
import io.github.kingg22.ktorgen.model.annotations.HttpMethod
import io.github.kingg22.ktorgen.model.options.Factories
import io.github.kingg22.ktorgen.validator.ValidationContext
import io.github.kingg22.ktorgen.validator.ValidationResult
import io.github.kingg22.ktorgen.validator.ValidatorStrategy

internal class ClassLevelValidator : ValidatorStrategy {
    override val name: String = "Class Level"

    override fun ValidationResult.validate(context: ValidationContext) {
        val interfaceDeclaration = context.classData
        val visibilityOptions = context.classData.visibilityOptions
        val ksInterfaceSymbol = interfaceDeclaration.ksInterface
        val ksCompanionSymbol = interfaceDeclaration.ksCompanionObject

        val classVisibility = visibilityOptions.classVisibilityModifier
        if (classVisibility.isBlank() || (
                classVisibility.equals("public", true).not() &&
                    classVisibility.equals("internal", true).not() &&
                    classVisibility.equals("private", true).not()
                )
        ) {
            addError(
                KtorGenLogger.ONLY_PUBLIC_INTERNAL_CLASS + "Current '$classVisibility'",
                ksInterfaceSymbol,
            )
        }
        validateKModifier(classVisibility, ksInterfaceSymbol)

        val constructorVisibility = visibilityOptions.constructorVisibilityModifier
        if (constructorVisibility.isBlank() ||
            constructorVisibility.equals("private", true) ||
            constructorVisibility.equals("protected", true)
        ) {
            addError(
                KtorGenLogger.PRIVATE_CONSTRUCTOR + "Current '$constructorVisibility'",
                interfaceDeclaration.ksInterface,
            )
        }
        validateKModifier(constructorVisibility, ksInterfaceSymbol)

        if (classVisibility.equals("private", true) &&
            interfaceDeclaration.generateTopLevelFunction.not() &&
            interfaceDeclaration.generateCompanionExtFunction.not() &&
            interfaceDeclaration.generateHttpClientExtension.not()
        ) {
            if (context.expectFunctions.none()) {
                addError(KtorGenLogger.PRIVATE_CLASS_NO_ACCESS, ksInterfaceSymbol)
            } else {
                addWarning(KtorGenLogger.PRIVATE_CLASS_NO_ACCESS, ksInterfaceSymbol)
            }
        }

        val functionVisibility = visibilityOptions.factoryFunctionVisibilityModifier
        if (functionVisibility.isBlank() ||
            functionVisibility.equals("private", true) ||
            functionVisibility.equals("protected", true)
        ) {
            addError(
                KtorGenLogger.PRIVATE_FUNCTION + "Current '$functionVisibility'",
                ksInterfaceSymbol,
            )
        }
        validateKModifier(functionVisibility, ksInterfaceSymbol)

        if (context.classData.modifierSet.contains(KModifier.PRIVATE)) {
            addError(
                KtorGenLogger.PRIVATE_INTERFACE_CANT_GENERATE + "Current ${context.classData.modifierSet}",
                ksInterfaceSymbol,
            )
        }

        if (ksCompanionSymbol == null &&
            interfaceDeclaration.generateCompanionExtFunction
        ) {
            addError(KtorGenLogger.MISSING_COMPANION_TO_GENERATE, ksInterfaceSymbol)
        }

        val ktorgenOptions = interfaceDeclaration.options

        if (ktorgenOptions.isDeclaredAtCompanionObject && ktorgenOptions.isDeclaredAtInterface) {
            addError(KtorGenLogger.DOUBLE_ANNOTATIONS + "@KtorGen()", ksCompanionSymbol)
        }

        if (interfaceDeclaration.factories.count { it is Factories.TopLevelFactory } > 1) {
            addError(KtorGenLogger.DOUBLE_ANNOTATIONS + "@KtorGenTopLevelFactory()", ksCompanionSymbol)
        }

        if (interfaceDeclaration.factories.count { it is Factories.CompanionExtension } > 1) {
            addError(KtorGenLogger.DOUBLE_ANNOTATIONS + "@KtorGenCompanionExtFactory()", ksCompanionSymbol)
        }

        if (interfaceDeclaration.factories.count { it is Factories.HttpClientExtension } > 1) {
            addError(KtorGenLogger.DOUBLE_ANNOTATIONS + "@KtorGenHttpClientExtFactory()", ksCompanionSymbol)
        }

        validateFunctions(context)
    }

    private fun ValidationResult.validateFunctions(context: ValidationContext) {
        for (function in context.functions.filter { it.goingToGenerate }) {
            if (function.returnTypeData.typeName == ANY ||
                function.returnTypeData.typeName == ANY.copy(nullable = true)
            ) {
                addError(KtorGenLogger.ANY_TYPE_INVALID, function)
            }

            if (function.httpMethodAnnotation.httpMethod == HttpMethod.Absent &&
                function.parameterDataList.none { it.isHttpRequestBuilderLambda || it.isValidTakeFrom }
            ) {
                addError(KtorGenLogger.NO_HTTP_ANNOTATION, function)
            }

            function.parameterDataList.forEach { parameter ->
                if (parameter.ktorgenAnnotations.none() &&
                    parameter.isValidTakeFrom.not() &&
                    parameter.isHttpRequestBuilderLambda.not()
                ) {
                    addError(KtorGenLogger.PARAMETER_WITHOUT_ANNOTATION, parameter)
                }

                // mix annotations on parameter like @HeaderParam @Cookie @Body is not allowed
                if (parameter.ktorgenAnnotations.filterNot { it.isRepeatable }.count() > 1) {
                    addError(KtorGenLogger.PARAMETER_WITH_LOT_ANNOTATIONS, parameter)
                }

                if (parameter.isVararg) {
                    addWarning(KtorGenLogger.VARARG_PARAMETER_EXPERIMENTAL, parameter)
                }

                if (parameter.typeData.typeName == ANY ||
                    parameter.typeData.typeName == ANY.copy(nullable = true)
                ) {
                    addError(KtorGenLogger.ANY_TYPE_INVALID, parameter)
                }
            }

            if (function.parameterDataList.count { it.isValidTakeFrom || it.isHttpRequestBuilderLambda } > 1) {
                addError(KtorGenLogger.ONLY_ONE_HTTP_REQUEST_BUILDER, function)
            }
        }
    }

    private fun ValidationResult.validateKModifier(modifier: String, symbol: KSNode?) {
        try {
            KModifier.valueOf(modifier.uppercase())
        } catch (_: IllegalArgumentException) {
            addError(
                KtorGenLogger.INVALID_VISIBILITY_MODIFIER + "Current '$modifier'",
                symbol,
            )
        }
    }
}
