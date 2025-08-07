package io.github.kingg22.ktorgen.validator.validators

import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.KModifier
import io.github.kingg22.ktorgen.KtorGenLogger
import io.github.kingg22.ktorgen.model.annotations.HttpMethod
import io.github.kingg22.ktorgen.validator.ValidationContext
import io.github.kingg22.ktorgen.validator.ValidationResult
import io.github.kingg22.ktorgen.validator.ValidatorStrategy

class ClassLevelValidator : ValidatorStrategy {
    override val name: String = "Class Level"

    override fun validate(context: ValidationContext) = ValidationResult {
        if (context.visibilityGeneratedClass.equals("public", true).not() &&
            context.visibilityGeneratedClass.equals("internal", false).not()
        ) {
            addError(
                KtorGenLogger.ONLY_PUBLIC_INTERNAL_CLASS + "Current ${context.visibilityGeneratedClass}",
                context.classData.ksClassDeclaration,
            )
        }

        if (context.classData.modifierSet.any { it == KModifier.PRIVATE }) {
            addError(
                KtorGenLogger.PRIVATE_INTERFACE_CANT_GENERATE + "Current ${context.classData.modifierSet}",
                context.classData.ksClassDeclaration,
            )
        }

        if (context.classData.haveCompanionObject.not() && context.classData.generateCompanionExtFunction) {
            addError(KtorGenLogger.MISSING_COMPANION_TO_GENERATE, context.classData.ksClassDeclaration)
        }

        context.functions
            .filter { it.isImplemented.not() && it.goingToGenerate.not() }
            .forEach { addError(KtorGenLogger.ABSTRACT_FUNCTION_IGNORED, it) }

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
}
