package io.github.kingg22.ktorgen.validator.validators

import io.github.kingg22.ktorgen.KtorGenLogger
import io.github.kingg22.ktorgen.model.annotations.ParameterAnnotation
import io.github.kingg22.ktorgen.validator.ValidationContext
import io.github.kingg22.ktorgen.validator.ValidationResult
import io.github.kingg22.ktorgen.validator.ValidatorStrategy

internal class QueryValidator : ValidatorStrategy {
    override val name: String = "URL Query"

    override fun validate(context: ValidationContext) = ValidationResult {
        for (function in context.functions.filter { it.goingToGenerate }) {
            function.parameterDataList.filter {
                it.hasAnnotation<ParameterAnnotation.QueryMap>()
            }.forEach { parameter ->
                parameter.findAnnotationOrNull<ParameterAnnotation.QueryMap>()?.let {
                    validateMapParameter(
                        parameter,
                        KtorGenLogger.QUERY_MAP_PARAMETER_TYPE_MUST_BE_MAP_PAIR_STRING,
                    ) { keys, values ->
                        // <String, String>
                        keys != Pair(KOTLIN_STRING, false) || values != Pair(KOTLIN_STRING, false)
                    }
                }
            }
        }
    }
}
