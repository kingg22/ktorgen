package io.github.kingg22.ktorgen.validator.validators

import io.github.kingg22.ktorgen.KtorGenLogger
import io.github.kingg22.ktorgen.model.annotations.ParameterAnnotation
import io.github.kingg22.ktorgen.validator.ValidationContext
import io.github.kingg22.ktorgen.validator.ValidationResult
import io.github.kingg22.ktorgen.validator.ValidatorStrategy

class QueryValidator : ValidatorStrategy {
    override fun validate(context: ValidationContext) = ValidationResult {
        for (function in context.functions) {
            function.parameterDataList.forEach { parameter ->
                parameter.findAnnotationOrNull<ParameterAnnotation.QueryMap>()?.let {
                    validateMapParameter(
                        parameter,
                        context,
                        function,
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
