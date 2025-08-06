package io.github.kingg22.ktorgen.validator.validators

import com.google.devtools.ksp.symbol.Visibility
import com.squareup.kotlinpoet.ANY
import io.github.kingg22.ktorgen.KtorGenLogger
import io.github.kingg22.ktorgen.model.annotations.HttpMethod
import io.github.kingg22.ktorgen.validator.ValidationContext
import io.github.kingg22.ktorgen.validator.ValidationResult
import io.github.kingg22.ktorgen.validator.ValidatorStrategy

class ClassLevelValidator : ValidatorStrategy {
    override val name: String = "Class Level"

    override fun validate(context: ValidationContext) = ValidationResult {
        if (context.visibility == Visibility.PRIVATE.name) {
            addError(
                buildString {
                    append(KtorGenLogger.PRIVATE_INTERFACE_CANT_GENERATE)
                    appendLine()
                    append("Declaration: ")
                    append(context.visibility)
                    append(" ")
                    append(context.className)
                },
            )
        }

        for (function in context.functions) {
            if (function.isImplemented.not() && function.goingToGenerate.not()) {
                addError(KtorGenLogger.ABSTRACT_FUNCTION_IGNORED, function)
            }

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

            for (parameter in function.parameterDataList) {
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
