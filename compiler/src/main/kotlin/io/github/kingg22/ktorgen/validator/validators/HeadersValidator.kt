package io.github.kingg22.ktorgen.validator.validators

import io.github.kingg22.ktorgen.KtorGenLogger
import io.github.kingg22.ktorgen.model.FunctionData
import io.github.kingg22.ktorgen.model.KTORGEN_DEFAULT_VALUE
import io.github.kingg22.ktorgen.model.ParameterData
import io.github.kingg22.ktorgen.model.annotations.FunctionAnnotation
import io.github.kingg22.ktorgen.model.annotations.ParameterAnnotation
import io.github.kingg22.ktorgen.validator.ValidationContext
import io.github.kingg22.ktorgen.validator.ValidationResult
import io.github.kingg22.ktorgen.validator.ValidatorStrategy

class HeadersValidator : ValidatorStrategy {
    override fun validate(context: ValidationContext) = ValidationResult {
        for (function in context.functions) {
            val headersFunction = function.findAnnotationOrNull<FunctionAnnotation.Headers>()?.value?.map {
                it.replace("\\s+".toRegex(), "")
            }?.toSet()?.associate {
                val (name, value) = it.split(":", limit = 2)
                if (name.isBlank() || value.isBlank()) {
                    addError(KtorGenLogger.INVALID_HEADER_FORMAT + addDeclaration(context, function))
                }
                Pair(name.trim(), value.trim())
            }?.toMutableMap() ?: mutableMapOf()
            if (headersFunction.any { (key, _) -> key.equals("Content-Type", true) } && function.isBodyInferred) {
                addError(KtorGenLogger.ONLY_ONE_CONTENT_TYPE_IS_ALLOWED + addDeclaration(context, function))
            }

            for (parameter in function.parameterDataList) {
                // Validate @Header
                parameter.findAnnotationOrNull<ParameterAnnotation.Header>()?.let { header ->
                    val name = header.value.replace("\\s+".toRegex(), "")
                    if (name.isBlank()) {
                        addError(
                            KtorGenLogger.INVALID_HEADER_FORMAT + addDeclaration(context, function, parameter),
                        )
                    } else if (name.trim() in headersFunction.keys) {
                        addError(KtorGenLogger.DUPLICATE_HEADER + addDeclaration(context, function, parameter))
                    }
                    if (function.isBodyInferred && name.equals("Content-Type", ignoreCase = true)) {
                        addError(
                            KtorGenLogger.ONLY_ONE_CONTENT_TYPE_IS_ALLOWED +
                                addDeclaration(context, function, parameter),
                        )
                    }
                    name.trim().takeIf(String::isNotBlank)?.let {
                        // collect to next parameter verify the accumulative headers
                        headersFunction.put(it, KTORGEN_DEFAULT_VALUE)
                    }
                }

                // Validate @HeaderMap
                parameter.findAnnotationOrNull<ParameterAnnotation.HeaderMap>()?.let {
                    val decl = parameter.type.parameterType.declaration
                    val qualifiedName = decl.qualifiedName?.asString()

                    // Pair<String, String> o Map<String, String>
                    if (qualifiedName == "kotlin.collections.Map" || qualifiedName == "kotlin.Pair") {
                        validateArgs(parameter, context, function)
                    } else {
                        addError(
                            KtorGenLogger.HEADER_MAP_PARAMETER_TYPE_MUST_BE_MAP_PAIR_STRING +
                                addDeclaration(context, function, parameter),
                        )
                    }
                }
            }
        }
    }

    private fun ValidationResult.validateArgs(
        parameter: ParameterData,
        context: ValidationContext,
        function: FunctionData,
    ) {
        val (firstType, secondType) = validateArgsOf(parameter)
        if (firstType != Pair("kotlin.String", false) || secondType.first != "kotlin.String") {
            addError(
                KtorGenLogger.HEADER_MAP_PARAMETER_TYPE_MUST_BE_MAP_PAIR_STRING +
                    addDeclaration(context, function, parameter),
            )
        }
    }
}
