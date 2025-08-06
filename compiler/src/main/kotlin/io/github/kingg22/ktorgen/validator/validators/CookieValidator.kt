package io.github.kingg22.ktorgen.validator.validators

import io.github.kingg22.ktorgen.KtorGenLogger
import io.github.kingg22.ktorgen.model.annotations.ParameterAnnotation
import io.github.kingg22.ktorgen.validator.ValidationContext
import io.github.kingg22.ktorgen.validator.ValidationResult
import io.github.kingg22.ktorgen.validator.ValidatorStrategy

class CookieValidator : ValidatorStrategy {
    override val name: String = "Cookies"

    override fun validate(context: ValidationContext) = ValidationResult {
        for (function in context.functions) {
            for (parameter in function.parameterDataList.filter { it.hasAnnotation<ParameterAnnotation.Cookies>() }) {
                if (parameter.isVararg &&
                    parameter.ktorgenAnnotations.count { it is ParameterAnnotation.Cookies } > 1
                ) {
                    addWarning(KtorGenLogger.VARARG_PARAMETER_WITH_LOT_ANNOTATIONS, parameter)
                }
            }
        }
    }
}
