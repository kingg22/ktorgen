package io.github.kingg22.ktorgen.validator.validators

import io.github.kingg22.ktorgen.KtorGenLogger
import io.github.kingg22.ktorgen.validator.ValidationContext
import io.github.kingg22.ktorgen.validator.ValidationResult
import io.github.kingg22.ktorgen.validator.ValidatorStrategy

internal class SuspendOrFlowValidator : ValidatorStrategy {
    override val name: String = "Return Type"

    override fun ValidationResult.validate(context: ValidationContext) {
        for (function in context.functions.filter { it.goingToGenerate }) {
            val returnTypeName = function.returnTypeData.parameterType.declaration.qualifiedName ?: run {
                addError("Return type is not defined", function)
                continue
            }
            val isFlowName = returnTypeName.asString() == "kotlinx.coroutines.flow.Flow"
            if (!function.isSuspend && !isFlowName) {
                addError(KtorGenLogger.SUSPEND_FUNCTION_OR_FLOW + returnTypeName.getShortName(), function)
            }
        }
    }
}
