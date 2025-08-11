package io.github.kingg22.ktorgen.validator.validators

import io.github.kingg22.ktorgen.KtorGenLogger
import io.github.kingg22.ktorgen.model.annotations.FunctionAnnotation
import io.github.kingg22.ktorgen.model.annotations.HttpMethod
import io.github.kingg22.ktorgen.model.annotations.ParameterAnnotation
import io.github.kingg22.ktorgen.validator.ValidationContext
import io.github.kingg22.ktorgen.validator.ValidationResult
import io.github.kingg22.ktorgen.validator.ValidatorStrategy

class MultipartValidator : ValidatorStrategy {
    override val name: String = "Multipart Body"

    override fun validate(context: ValidationContext) = ValidationResult {
        for (function in context.functions.filter { it.goingToGenerate }) {
            var isMultiPart = function.hasAnnotation<FunctionAnnotation.Multipart>()
            if (!isMultiPart &&
                function.parameterDataList.any {
                    it.hasAnnotation<ParameterAnnotation.Part>() || it.hasAnnotation<ParameterAnnotation.PartMap>()
                }
            ) {
                isMultiPart = true
                addWarning(KtorGenLogger.MULTIPART_ANNOTATION_MISSING_FOUND_PART, function)
            }
            if (isMultiPart &&
                function.parameterDataList.none {
                    it.hasAnnotation<ParameterAnnotation.Part>() || it.hasAnnotation<ParameterAnnotation.PartMap>()
                }
            ) {
                addError(KtorGenLogger.MULTIPART_MUST_CONTAIN_AT_LEAST_ONE_PART, function)
            }
            if (isMultiPart &&
                function.httpMethodAnnotation.httpMethod !in listOf(HttpMethod.Post, HttpMethod.Put, HttpMethod.Patch)
            ) {
                addWarning(KtorGenLogger.FORM_ENCODED_ANNOTATION_MISMATCH_HTTP_METHOD, function)
            }
            function.parameterDataList.forEach { parameter ->
                parameter.findAnnotationOrNull<ParameterAnnotation.PartMap>()?.let {
                    validateMapParameter(
                        parameter,
                        KtorGenLogger.PART_MAP_PARAMETER_TYPE_MUST_BE_MAP_PAIR_STRING,
                    )
                }
            }
        }
    }
}
