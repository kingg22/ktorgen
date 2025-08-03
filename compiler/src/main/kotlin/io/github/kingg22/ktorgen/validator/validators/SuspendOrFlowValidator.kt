package io.github.kingg22.ktorgen.validator.validators

import io.github.kingg22.ktorgen.KtorGenLogger
import io.github.kingg22.ktorgen.validator.ValidationContext
import io.github.kingg22.ktorgen.validator.ValidationResult
import io.github.kingg22.ktorgen.validator.ValidatorStrategy

class SuspendOrFlowValidator : ValidatorStrategy {
    override fun validate(context: ValidationContext) = ValidationResult {
        for (function in context.functions) {
            val returnTypeName = requireNotNull(function.returnTypeData.parameterType.declaration.qualifiedName) {
                "${KtorGenLogger.KTOR_GEN} Return type is not defined. ${addDeclaration(context, function)}"
            }
            val isFlowName = returnTypeName.asString() == "kotlinx.coroutines.flow.Flow"
            if (!function.isSuspend && !isFlowName) {
                addError(
                    KtorGenLogger.SUSPEND_FUNCTION_OR_FLOW + returnTypeName.getShortName() + ". " +
                        addDeclaration(context, function),
                )
            }
        }
    }
}
