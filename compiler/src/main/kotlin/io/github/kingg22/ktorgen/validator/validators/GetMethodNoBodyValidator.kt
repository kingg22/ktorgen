package io.github.kingg22.ktorgen.validator.validators

import io.github.kingg22.ktorgen.KtorGenLogger
import io.github.kingg22.ktorgen.model.annotations.ParameterAnnotation
import io.github.kingg22.ktorgen.validator.ValidationContext
import io.github.kingg22.ktorgen.validator.ValidationResult
import io.github.kingg22.ktorgen.validator.ValidatorStrategy

class GetMethodNoBodyValidator : ValidatorStrategy {
    override fun validate(context: ValidationContext): ValidationResult {
        val result = ValidationResult()
        for (function in context.functions) {
            if (function.goingToGenerate &&
                function.httpMethodAnnotation.httpMethod.supportsRequestBody.not() &&
                function.parameterDataList.any { it.annotations.contains(ParameterAnnotation.Body) }
            ) {
                result.addWarning(
                    function.httpMethodAnnotation.httpMethod.value + KtorGenLogger.GET_METHOD_MUST_NOT_INCLUDE_BODY,
                )
            }
        }
        return result
    }
}
