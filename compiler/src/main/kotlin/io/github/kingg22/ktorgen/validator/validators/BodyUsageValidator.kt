package io.github.kingg22.ktorgen.validator.validators

import io.github.kingg22.ktorgen.KtorGenLogger
import io.github.kingg22.ktorgen.model.annotations.ParameterAnnotation
import io.github.kingg22.ktorgen.validator.ValidationContext
import io.github.kingg22.ktorgen.validator.ValidationResult
import io.github.kingg22.ktorgen.validator.ValidatorStrategy

class BodyUsageValidator : ValidatorStrategy {
    override fun validate(context: ValidationContext) = ValidationResult {
        for (function in context.functions) {
            val isBody = function.isBody
            val isFormUrl = function.isFormUrl
            val isMultipart = function.isMultipart

            if ((isBody || isFormUrl || isMultipart) && !function.httpMethodAnnotation.httpMethod.supportsRequestBody) {
                addWarning(KtorGenLogger.BODY_USAGE_INVALID_HTTP_METHOD + addDeclaration(context, function))
            }

            if (listOf(isBody, isFormUrl, isMultipart).count { it } > 1) {
                addError(KtorGenLogger.CONFLICT_BODY_TYPE + addDeclaration(context, function))
            }

            if (function.parameterDataList.count { it.hasAnnotation<ParameterAnnotation.Body>() } > 1) {
                addError(KtorGenLogger.INVALID_BODY_PARAMETER + addDeclaration(context, function))
            }
        }
    }
}
