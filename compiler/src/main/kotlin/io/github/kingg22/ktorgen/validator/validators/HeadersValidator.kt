package io.github.kingg22.ktorgen.validator.validators

import io.github.kingg22.ktorgen.KtorGenLogger
import io.github.kingg22.ktorgen.model.KTORGEN_DEFAULT_VALUE
import io.github.kingg22.ktorgen.model.annotations.FunctionAnnotation
import io.github.kingg22.ktorgen.model.annotations.ParameterAnnotation
import io.github.kingg22.ktorgen.model.annotations.removeWhitespace
import io.github.kingg22.ktorgen.validator.ValidationContext
import io.github.kingg22.ktorgen.validator.ValidationResult
import io.github.kingg22.ktorgen.validator.ValidatorStrategy
import io.github.kingg22.ktorgen.http.Header.Companion.ContentType as CONTENT_TYPE_HEADER

class HeadersValidator : ValidatorStrategy {
    override val name: String = "Headers"

    override fun validate(context: ValidationContext) = ValidationResult {
        for (function in context.functions.filter { it.goingToGenerate }) {
            var haveHeadersMap = function.parameterDataList.any { it.hasAnnotation<ParameterAnnotation.HeaderMap>() }
            val headersFunction =
                function.findAnnotationOrNull<FunctionAnnotation.Headers>()?.value?.map { (name, value) ->
                    name.removeWhitespace() to value.removeWhitespace()
                }?.associate {
                    val (name, value) = it
                    if (name.isBlank() || value.isBlank()) addError(KtorGenLogger.INVALID_HEADER, function)
                    it
                }?.toMutableMap() ?: mutableMapOf()
            var haveContentType = headersFunction.any { (key, _) -> key.equals(CONTENT_TYPE_HEADER, true) }

            if (haveContentType && (function.isFormUrl || function.isMultipart)) {
                addError(KtorGenLogger.ONLY_ONE_CONTENT_TYPE_IS_ALLOWED, function)
            }

            for (parameter in function.parameterDataList.filter {
                it.hasAnnotation<ParameterAnnotation.Header>() ||
                    it.hasAnnotation<ParameterAnnotation.HeaderMap>()
            }) {
                // Validate @HeaderParam
                if (parameter.isVararg && parameter.ktorgenAnnotations.count { it is ParameterAnnotation.Header } > 1) {
                    addWarning(KtorGenLogger.VARARG_PARAMETER_WITH_LOT_ANNOTATIONS, parameter)
                }
                parameter.findAllAnnotations<ParameterAnnotation.Header>().forEach { header ->
                    val name = header.value.removeWhitespace()
                    if (name.isBlank()) {
                        addError(KtorGenLogger.INVALID_HEADER, parameter)
                    }

                    val isContentType = name.equals(CONTENT_TYPE_HEADER, ignoreCase = true)

                    // Solo valida duplicado si es Content-Type
                    if (isContentType &&
                        (headersFunction.keys.any { it.equals(CONTENT_TYPE_HEADER, true) } || parameter.isVararg)
                    ) {
                        addError(KtorGenLogger.DUPLICATE_HEADER, parameter)
                    }

                    if (!haveContentType && isContentType) haveContentType = true

                    if ((function.isFormUrl || function.isMultipart) && isContentType) {
                        addError(KtorGenLogger.ONLY_ONE_CONTENT_TYPE_IS_ALLOWED, parameter)
                    }

                    name.takeIf(String::isNotBlank)?.let {
                        // save Content-Type for next iteration
                        if (isContentType) headersFunction[it] = KTORGEN_DEFAULT_VALUE
                    }
                }

                // Validate @HeaderMap
                parameter.findAnnotationOrNull<ParameterAnnotation.HeaderMap>()?.let {
                    haveHeadersMap = true
                    validateMapParameter(
                        parameter,
                        KtorGenLogger.HEADER_MAP_PARAMETER_TYPE_MUST_BE_MAP_PAIR_STRING,
                    ) { keys, values ->
                        keys != Pair(KOTLIN_STRING, false) || values.first != KOTLIN_STRING
                    }
                }
            }

            if (function.isBody && !haveHeadersMap && !haveContentType) {
                addWarning(KtorGenLogger.CONTENT_TYPE_BODY_UNKNOWN, function)
            }
        }
    }
}
