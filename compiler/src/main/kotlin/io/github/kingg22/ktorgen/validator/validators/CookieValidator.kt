package io.github.kingg22.ktorgen.validator.validators

import io.github.kingg22.ktorgen.KtorGenLogger
import io.github.kingg22.ktorgen.model.annotations.ParameterAnnotation
import io.github.kingg22.ktorgen.validator.ValidationContext
import io.github.kingg22.ktorgen.validator.ValidationResult
import io.github.kingg22.ktorgen.validator.ValidatorStrategy

internal class CookieValidator : ValidatorStrategy {
    override val name: String = "Cookies"

    override fun validate(context: ValidationContext) = ValidationResult {
        context.functions
            .filter { it.goingToGenerate }
            .flatMap { func -> func.parameterDataList.filter { it.hasAnnotation<ParameterAnnotation.Cookies>() } }
            .filter { parameter ->
                // check if the parameter is vararg and has more than one annotation
                parameter.isVararg && parameter.findAnnotation<ParameterAnnotation.Cookies>().value.size > 1
            }.forEach { parameter ->
                addWarning(KtorGenLogger.VARARG_PARAMETER_WITH_LOT_ANNOTATIONS, parameter)
            }
    }
}
