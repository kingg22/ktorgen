package io.github.kingg22.ktorgen.validator.validators

import com.squareup.kotlinpoet.ANY
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

            if (function.returnTypeData.typeName == ANY ||
                function.returnTypeData.typeName == ANY.copy(nullable = true)
            ) {
                addError(KtorGenLogger.ANY_TYPE_INVALID + addDeclaration(context, function))
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

                // mix annotations on parameter like @HeaderParam @Cookie @Body is not allowed
                if (parameter.ktorgenAnnotations.filterNot { it.isRepeatable }.size > 1) {
                    addError(
                        KtorGenLogger.PARAMETER_WITH_LOT_ANNOTATIONS + addDeclaration(context, function, parameter),
                    )
                }

                if (parameter.isVararg) {
                    addWarning(KtorGenLogger.VARARG_PARAMETER_EXPERIMENTAL)
                }

                if (parameter.typeData.typeName == ANY || parameter.typeData.typeName == ANY.copy(nullable = true)) {
                    addError(KtorGenLogger.ANY_TYPE_INVALID + addDeclaration(context, function, parameter))
                }
            }

            if (function.parameterDataList.count { it.isHttpRequestBuilder || it.isHttpRequestBuilderLambda } > 1) {
                addError(KtorGenLogger.ONLY_ONE_HTTP_REQUEST_BUILDER + addDeclaration(context, function))
            }
        }
    }
}
