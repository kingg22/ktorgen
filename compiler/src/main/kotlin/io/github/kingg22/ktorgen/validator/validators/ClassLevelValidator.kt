package io.github.kingg22.ktorgen.validator.validators

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.KModifier
import io.github.kingg22.ktorgen.KtorGenLogger
import io.github.kingg22.ktorgen.core.KtorGen
import io.github.kingg22.ktorgen.model.annotations.HttpMethod
import io.github.kingg22.ktorgen.validator.ValidationContext
import io.github.kingg22.ktorgen.validator.ValidationResult
import io.github.kingg22.ktorgen.validator.ValidatorStrategy

internal class ClassLevelValidator : ValidatorStrategy {
    override val name: String = "Class Level"

    override fun validate(context: ValidationContext) = ValidationResult {
        val interfaceDeclaration = context.classData.ksClassDeclaration

        val classVisibility = context.classData.classVisibilityModifier
        if (classVisibility.isBlank() ||
            classVisibility.equals("public", true).not() &&
            classVisibility.equals("internal", true).not() &&
            classVisibility.equals("private", true).not()
        ) {
            addError(
                KtorGenLogger.ONLY_PUBLIC_INTERNAL_CLASS + "Current '$classVisibility'",
                interfaceDeclaration,
            )
        }
        validateKModifier(classVisibility, interfaceDeclaration)

        val constructorVisibility = context.classData.constructorVisibilityModifier
        if (constructorVisibility.isBlank() ||
            constructorVisibility.equals("private", true) ||
            constructorVisibility.equals("protected", true)
        ) {
            addError(
                KtorGenLogger.PRIVATE_CONSTRUCTOR + "Current '$constructorVisibility'",
                interfaceDeclaration,
            )
        }
        validateKModifier(constructorVisibility, interfaceDeclaration)

        if (context.classData.classVisibilityModifier.equals("private", true) &&
            context.classData.generateTopLevelFunction.not() &&
            context.classData.generateCompanionExtFunction.not() &&
            context.classData.generateHttpClientExtension.not()
        ) {
            addError(KtorGenLogger.PRIVATE_CLASS_NO_ACCESS, interfaceDeclaration)
        }

        val functionVisibility = context.classData.functionVisibilityModifier
        if (functionVisibility.isBlank() ||
            functionVisibility.equals("private", true) ||
            functionVisibility.equals("protected", true)
        ) {
            addError(
                KtorGenLogger.PRIVATE_FUNCTION + "Current '$functionVisibility'",
                interfaceDeclaration,
            )
        }
        validateKModifier(functionVisibility, interfaceDeclaration)

        if (context.classData.modifierSet.any { it == KModifier.PRIVATE }) {
            addError(
                KtorGenLogger.PRIVATE_INTERFACE_CANT_GENERATE + "Current ${context.classData.modifierSet}",
                interfaceDeclaration,
            )
        }

        if (context.classData.haveCompanionObject.not() && context.classData.generateCompanionExtFunction) {
            addError(KtorGenLogger.MISSING_COMPANION_TO_GENERATE, interfaceDeclaration)
        }

        if (context.classData.haveCompanionObject) {
            val companionDeclaration = interfaceDeclaration.declarations
                .filterIsInstance<KSClassDeclaration>()
                .firstOrNull { it.isCompanionObject } ?: error("Unexpected not found companion object declaration")

            if (companionDeclaration.annotations
                    .any { it.shortName.getShortName() == KtorGen::class.simpleName!! } &&
                interfaceDeclaration.annotations
                    .any { it.shortName.getShortName() == KtorGen::class.simpleName!! }
            ) {
                addError(KtorGenLogger.TWO_KTORGEN_ANNOTATIONS, companionDeclaration)
            }
        }

        context.functions
            .filter { it.isImplemented.not() && it.goingToGenerate.not() }
            .forEach { addError(KtorGenLogger.ABSTRACT_FUNCTION_IGNORED, it) }

        validateFunctions(context)

        val expectedAreNotExpect = context.classData.expectFunctions.filter { !it.modifiers.contains(Modifier.EXPECT) }
        if (expectedAreNotExpect.isNotEmpty()) {
            addError(
                "A function annotated with @KtorGenFunctionKmp must be 'expect' in common source set",
                expectedAreNotExpect.first(),
            )
        }
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
                if (parameter.ktorgenAnnotations.isEmpty() &&
                    parameter.isValidTakeFrom.not() &&
                    parameter.isHttpRequestBuilderLambda.not()
                ) {
                    addError(KtorGenLogger.PARAMETER_WITHOUT_ANNOTATION, parameter)
                }

                // mix annotations on parameter like @HeaderParam @Cookie @Body is not allowed
                if (parameter.ktorgenAnnotations.filterNot { it.isRepeatable }.size > 1) {
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
