package io.github.kingg22.ktorgen.validator.validators

import io.github.kingg22.ktorgen.KtorGenLogger
import io.github.kingg22.ktorgen.validator.ValidationContext
import io.github.kingg22.ktorgen.validator.ValidationResult
import io.github.kingg22.ktorgen.validator.ValidatorStrategy

class ClassLevelValidator : ValidatorStrategy {
    override fun validate(context: ValidationContext) = ValidationResult {
        for (function in context.functions) {
            if (function.isImplemented.not() && function.goingToGenerate.not()) {
                addError(KtorGenLogger.ABSTRACT_FUNCTION_IGNORED + addDeclaration(context, function))
            }

            for (parameter in function.parameterDataList) {
                if (parameter.ktorgenAnnotations.isEmpty() &&
                    parameter.isHttpRequestBuilder.not() &&
                    parameter.isHttpRequestBuilderLambda.not()
                ) {
                    addError(
                        KtorGenLogger.PARAMETER_WITHOUT_ANNOTATION + addDeclaration(context, function, parameter),
                    )
                }
                if (parameter.ktorgenAnnotations.size > 1) {
                    addError(
                        KtorGenLogger.PARAMETER_WITH_LOT_ANNOTATIONS + addDeclaration(context, function, parameter),
                    )
                }
                if (parameter.isVararg) {
                    addWarning(KtorGenLogger.VARARG_PARAMETER_EXPERIMENTAL)
                }
            }

            if (function.parameterDataList.count { it.isHttpRequestBuilder || it.isHttpRequestBuilderLambda } > 1) {
                addError(KtorGenLogger.ONLY_ONE_HTTP_REQUEST_BUILDER + addDeclaration(context, function))
            }
        }
    }
}
