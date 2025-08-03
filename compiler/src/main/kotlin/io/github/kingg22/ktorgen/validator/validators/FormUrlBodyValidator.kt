package io.github.kingg22.ktorgen.validator.validators

import io.github.kingg22.ktorgen.KtorGenLogger
import io.github.kingg22.ktorgen.model.annotations.FunctionAnnotation
import io.github.kingg22.ktorgen.model.annotations.HttpMethod
import io.github.kingg22.ktorgen.model.annotations.ParameterAnnotation
import io.github.kingg22.ktorgen.validator.ValidationContext
import io.github.kingg22.ktorgen.validator.ValidationResult
import io.github.kingg22.ktorgen.validator.ValidatorStrategy

class FormUrlBodyValidator : ValidatorStrategy {
    override fun validate(context: ValidationContext) = ValidationResult {
        for (function in context.functions) {
            var isFormUrlEncoded = function.hasAnnotation<FunctionAnnotation.FormUrlEncoded>()
            if (
                !isFormUrlEncoded &&
                function.parameterDataList.any {
                    it.hasAnnotation<ParameterAnnotation.Field>() || it.hasAnnotation<ParameterAnnotation.FieldMap>()
                }
            ) {
                addWarning(
                    KtorGenLogger.FORM_ENCODED_ANNOTATION_MISSING_FOUND_FIELD + addDeclaration(context, function),
                )
                isFormUrlEncoded = true
            }
            if (isFormUrlEncoded &&
                function.parameterDataList.none {
                    it.hasAnnotation<ParameterAnnotation.Field>() || it.hasAnnotation<ParameterAnnotation.FieldMap>()
                }
            ) {
                addError(KtorGenLogger.FORM_ENCODED_MUST_CONTAIN_AT_LEAST_ONE_FIELD + addDeclaration(context, function))
            }
            if (isFormUrlEncoded &&
                function.httpMethodAnnotation.httpMethod !in listOf(HttpMethod.Post, HttpMethod.Put, HttpMethod.Patch)
            ) {
                addWarning(
                    KtorGenLogger.FORM_ENCODED_ANNOTATION_MISMATCH_HTTP_METHOD + addDeclaration(context, function),
                )
            }
            function.parameterDataList.forEach { parameter ->
                parameter.findAnnotationOrNull<ParameterAnnotation.FieldMap>()?.let {
                    validateMapParameter(
                        parameter,
                        context,
                        function,
                        KtorGenLogger.FIELD_MAP_PARAMETER_TYPE_MUST_BE_MAP_PAIR_STRING,
                    )
                }
            }
        }
    }
}
