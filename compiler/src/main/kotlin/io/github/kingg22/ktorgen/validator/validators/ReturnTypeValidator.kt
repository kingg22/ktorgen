package io.github.kingg22.ktorgen.validator.validators

import io.github.kingg22.ktorgen.KtorGenLogger
import io.github.kingg22.ktorgen.model.isFlowType
import io.github.kingg22.ktorgen.model.isHttpRequestBuilderType
import io.github.kingg22.ktorgen.model.isHttpStatementType
import io.github.kingg22.ktorgen.model.isResultType
import io.github.kingg22.ktorgen.model.unwrapResult
import io.github.kingg22.ktorgen.validator.ValidationContext
import io.github.kingg22.ktorgen.validator.ValidationResult
import io.github.kingg22.ktorgen.validator.ValidatorStrategy

internal class ReturnTypeValidator : ValidatorStrategy {
    override val name: String = "Return Type"

    override fun ValidationResult.validate(context: ValidationContext) {
        context.functions.filter { function ->
            val returnType = function.returnTypeData.typeName

            val isAllowedNonSuspend = returnType.isHttpRequestBuilderType ||
                returnType.isHttpStatementType ||
                returnType.isFlowType() || (
                    returnType.isResultType() && (
                        returnType.unwrapResult().isHttpRequestBuilderType ||
                            returnType.unwrapResult().isHttpStatementType
                        )
                    )

            function.goingToGenerate &&
                function.isSuspend.not() &&
                isAllowedNonSuspend.not()
        }.forEach { function ->
            addError(
                KtorGenLogger.SUSPEND_FUNCTION_OR_FLOW +
                    function.returnTypeData.typeName.toString(),
                function,
            )
        }
    }
}
