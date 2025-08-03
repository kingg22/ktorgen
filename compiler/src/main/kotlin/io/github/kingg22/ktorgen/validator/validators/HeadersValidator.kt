package io.github.kingg22.ktorgen.validator.validators

import io.github.kingg22.ktorgen.KtorGenLogger
import io.github.kingg22.ktorgen.model.KTORGEN_DEFAULT_VALUE
import io.github.kingg22.ktorgen.model.annotations.FunctionAnnotation
import io.github.kingg22.ktorgen.model.annotations.ParameterAnnotation
import io.github.kingg22.ktorgen.validator.ValidationContext
import io.github.kingg22.ktorgen.validator.ValidationResult
import io.github.kingg22.ktorgen.validator.ValidatorStrategy

class HeadersValidator : ValidatorStrategy {
    override fun validate(context: ValidationContext) = ValidationResult {
        for (function in context.functions) {
            var haveHeadersMap = function.parameterDataList.any { it.hasAnnotation<ParameterAnnotation.HeaderMap>() }
            val headersFunction = function.findAnnotationOrNull<FunctionAnnotation.Headers>()?.value?.map {
                it.replace("\\s+".toRegex(), "")
            }?.toSet()?.associate {
                val (name, value) = it.split(":", limit = 2)
                if (name.isBlank() || value.isBlank()) {
                    addError(KtorGenLogger.INVALID_HEADER_FORMAT + addDeclaration(context, function))
                }
                Pair(name.trim(), value.trim())
            }?.toMutableMap() ?: mutableMapOf()
            var haveContentType = headersFunction.any { (key, _) -> key.equals(CONTENT_TYPE_HEADER, true) }

            if (haveContentType && (function.isFormUrl || function.isMultipart)) {
                addError(KtorGenLogger.ONLY_ONE_CONTENT_TYPE_IS_ALLOWED + addDeclaration(context, function))
            }

            for (parameter in function.parameterDataList) {
                // Validate @Header
                parameter.findAnnotationOrNull<ParameterAnnotation.Header>()?.let { header ->
                    val name = header.value.trim().replace("\\s+".toRegex(), "")
                    if (name.isBlank()) {
                        addError(
                            KtorGenLogger.INVALID_HEADER_FORMAT + addDeclaration(context, function, parameter),
                        )
                    }

                    val isContentType = name.equals(CONTENT_TYPE_HEADER, ignoreCase = true)

                    // Solo valida duplicado si es Content-Type
                    if (isContentType && headersFunction.keys.any { it.equals(CONTENT_TYPE_HEADER, true) }) {
                        addError(KtorGenLogger.DUPLICATE_HEADER + addDeclaration(context, function, parameter))
                    }

                    if (!haveContentType && isContentType) haveContentType = true

                    if ((function.isFormUrl || function.isMultipart) && isContentType) {
                        addError(
                            KtorGenLogger.ONLY_ONE_CONTENT_TYPE_IS_ALLOWED +
                                addDeclaration(context, function, parameter),
                        )
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
                        context,
                        function,
                        KtorGenLogger.HEADER_MAP_PARAMETER_TYPE_MUST_BE_MAP_PAIR_STRING,
                    ) { keys, values ->
                        keys != Pair("kotlin.String", false) || values.first != "kotlin.String"
                    }
                }
            }

            if (function.isBody && !haveHeadersMap && !haveContentType) {
                addWarning(KtorGenLogger.CONTENT_TYPE_BODY_UNKNOWN + addDeclaration(context, function))
            }
        }
    }

    companion object {
        const val CONTENT_TYPE_HEADER = "Content-Type"
    }
}
