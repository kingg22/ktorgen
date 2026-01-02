package io.github.kingg22.ktorgen.validator.validators

import io.github.kingg22.ktorgen.KtorGenLogger
import io.github.kingg22.ktorgen.model.UrlPathRegex
import io.github.kingg22.ktorgen.model.annotations.FunctionAnnotation
import io.github.kingg22.ktorgen.model.annotations.ParameterAnnotation
import io.github.kingg22.ktorgen.validator.ValidationContext
import io.github.kingg22.ktorgen.validator.ValidationResult
import io.github.kingg22.ktorgen.validator.ValidatorStrategy

internal class UrlSyntaxValidator : ValidatorStrategy {
    override val name: String = "Url Syntax"

    override fun ValidationResult.validate(context: ValidationContext) {
        for (function in context.functions.filter { it.goingToGenerate }) {
            val pathValue = function.httpMethodAnnotation.path.replace(UrlPathRegex, "valid")
            // prevent false positive when have path template, but in runtime can throw exception Url function

            if (pathValue.isNotBlank() &&
                function.parameterDataList.any { it.hasAnnotation<ParameterAnnotation.Url>() }
            ) {
                addError(KtorGenLogger.URL_WITH_PATH_VALUE, function)
            }

            if (!isValidUrl(pathValue)) {
                addWarning(
                    KtorGenLogger.URL_SYNTAX_ERROR +
                        "Current path ('valid' is a placeholder for runtime values): $pathValue",
                    function,
                )
            }

            val basePath = context.classData.basePath
            val methodPath = function.httpMethodAnnotation.path

            if (hasSuspiciousDoubleSlash(basePath, methodPath)) {
                addWarning(
                    KtorGenLogger.DOUBLE_SLASH_IN_URL_PATH +
                        "Base path '$basePath' and endpoint path '$methodPath' " +
                        "will generate a double slash ('//'). " +
                        "Consider removing the trailing or leading '/'.",
                    function,
                )
            }

            if (function.parameterDataList.count { it.hasAnnotation<ParameterAnnotation.Url>() } > 1 ||
                function.parameterDataList
                    .firstOrNull { it.hasAnnotation<ParameterAnnotation.Url>() }?.isVararg == true
            ) {
                addError(KtorGenLogger.MULTIPLE_URL_FOUND, function)
            }

            val urlParameter = function.parameterDataList.firstOrNull {
                it.hasAnnotation<ParameterAnnotation.Url>()
            }

            if (urlParameter != null) {
                if (urlParameter.typeData.parameterType.isMarkedNullable) {
                    addError(KtorGenLogger.URL_PARAMETER_TYPE_MAY_NOT_BE_NULLABLE, urlParameter)
                }

                if (function.parameterDataList.any { it.hasAnnotation<ParameterAnnotation.Path>() }) {
                    addError(KtorGenLogger.URL_WITH_PATH_PARAMETER, function)
                }
            }

            function.findAnnotationOrNull<FunctionAnnotation.Fragment>()?.let { fragment ->
                if (fragment.value.isBlank()) addError(KtorGenLogger.URL_FRAGMENT_IN_FUNCTION_IS_BLANK, function)
                if (function.parameterDataList.any { it.hasAnnotation<ParameterAnnotation.Fragment>() }) {
                    addError(KtorGenLogger.MULTIPLE_URL_FRAGMENT, function)
                }
            } ?: run {
                if (function.parameterDataList.count { it.hasAnnotation<ParameterAnnotation.Fragment>() } > 1) {
                    addError(KtorGenLogger.MULTIPLE_URL_FRAGMENT, function)
                }
            }
        }
    }

    private fun isValidUrl(url: String): Boolean = try {
        io.ktor.http.Url(url)
        true
    } catch (_: Throwable) {
        false
    }

    private fun hasSuspiciousDoubleSlash(basePath: String?, fullPath: String): Boolean {
        if (basePath.isNullOrBlank()) return false
        if (fullPath.isBlank()) return false
        if (basePath == fullPath) return false

        return basePath.endsWith("/") &&
            fullPath.removePrefix(basePath).startsWith("/") &&
            basePath != "/" &&
            fullPath != "/"
    }
}
