package io.github.kingg22.ktorgen.validator.validators

import io.github.kingg22.ktorgen.KtorGenLogger
import io.github.kingg22.ktorgen.model.annotations.ParameterAnnotation
import io.github.kingg22.ktorgen.validator.ValidationContext
import io.github.kingg22.ktorgen.validator.ValidationResult
import io.github.kingg22.ktorgen.validator.ValidatorStrategy

class UrlSyntaxValidator : ValidatorStrategy {
    override fun validate(context: ValidationContext) = ValidationResult {
        for (function in context.functions) {
            val pathValue = function.httpMethodAnnotation.path

            if (pathValue.isNotBlank() &&
                function.parameterDataList.any { it.hasAnnotation<ParameterAnnotation.Url>() }
            ) {
                addError(KtorGenLogger.URL_WITH_PATH_VALUE + addDeclaration(context, function))
            }

            if (function.parameterDataList.count { it.hasAnnotation<ParameterAnnotation.Url>() } > 1 ||
                function.parameterDataList
                    .firstOrNull { it.hasAnnotation<ParameterAnnotation.Url>() }?.isVararg == true
            ) {
                addError(KtorGenLogger.MULTIPLE_URL_FOUND + addDeclaration(context, function))
            }

            val urlParameter = function.parameterDataList.firstOrNull {
                it.hasAnnotation<ParameterAnnotation.Url>()
            }

            if (urlParameter != null) {
                if (urlParameter.typeData.parameterType.isMarkedNullable) {
                    addError(
                        KtorGenLogger.URL_PARAMETER_TYPE_MAY_NOT_BE_NULLABLE +
                            addDeclaration(context, function, urlParameter),
                    )
                }

                if (function.parameterDataList.any { it.hasAnnotation<ParameterAnnotation.Path>() }) {
                    addError(KtorGenLogger.URL_WITH_PATH_PARAMETER + addDeclaration(context, function))
                }
            }
        }
    }
}
