package io.github.kingg22.ktorgen.validator.validators

import io.github.kingg22.ktorgen.KtorGenLogger
import io.github.kingg22.ktorgen.model.annotations.ParameterAnnotation
import io.github.kingg22.ktorgen.validator.ValidationContext
import io.github.kingg22.ktorgen.validator.ValidationResult
import io.github.kingg22.ktorgen.validator.ValidatorStrategy

class PathParameterValidator : ValidatorStrategy {
    override fun validate(context: ValidationContext) = ValidationResult {
        for (function in context.functions) {
            val (template, placeholders) = function.urlTemplate
            if (function.parameterDataList.any { it.hasAnnotation<ParameterAnnotation.Path>() } &&
                placeholders.isEmpty()
            ) {
                addError(
                    KtorGenLogger.PATH_CAN_ONLY_BE_USED_WITH_RELATIVE_URL_ON +
                        ", current URL without path placeholders: '$template'. " +
                        addDeclaration(context, function),
                )
            }
            if (placeholders.isEmpty()) continue
            if (function.parameterDataList.none { it.hasAnnotation<ParameterAnnotation.Path>() }) {
                addError(
                    KtorGenLogger.MISSING_PATH_VALUE + function.httpMethodAnnotation.path + ". " +
                        addDeclaration(context, function),
                )
            }
            if (placeholders.distinct().size != placeholders.size) {
                addError(
                    KtorGenLogger.DUPLICATE_PATH_PLACEHOLDER + function.httpMethodAnnotation.path + ". " +
                        addDeclaration(context, function),
                )
            }
            for (parameter in function.parameterDataList) {
                val pathValue = parameter.findAnnotationOrNull<ParameterAnnotation.Path>()
                if (pathValue != null) {
                    if (parameter.typeData.parameterType.isMarkedNullable) {
                        addError(
                            KtorGenLogger.PATH_PARAMETER_TYPE_MAY_NOT_BE_NULLABLE +
                                addDeclaration(context, function, parameter),
                        )
                    }
                    if (pathValue.value !in placeholders) {
                        addError(
                            KtorGenLogger.MISSING_PATH_VALUE + function.httpMethodAnnotation.path + ". " +
                                addDeclaration(context, function),
                        )
                    }
                }
            }
        }
    }
}
