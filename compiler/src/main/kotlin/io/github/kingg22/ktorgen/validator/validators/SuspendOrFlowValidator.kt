package io.github.kingg22.ktorgen.validator.validators

import com.google.devtools.ksp.symbol.FileLocation
import io.github.kingg22.ktorgen.KtorGenLogger
import io.github.kingg22.ktorgen.validator.ValidationContext
import io.github.kingg22.ktorgen.validator.ValidationResult
import io.github.kingg22.ktorgen.validator.ValidatorStrategy

class SuspendOrFlowValidator : ValidatorStrategy {
    override val name: String = "Return Type"

    override fun validate(context: ValidationContext) = ValidationResult {
        for (function in context.functions.filter { it.goingToGenerate }) {
            val returnTypeName = requireNotNull(function.returnTypeData.parameterType.declaration.qualifiedName) {
                val funcLine = function.ksFunctionDeclaration.location as? FileLocation
                val position = if (funcLine != null) "${funcLine.lineNumber}:${funcLine.filePath}" else function.name
                "${KtorGenLogger.KTOR_GEN} Return type is not defined. $position"
            }
            val isFlowName = returnTypeName.asString() == "kotlinx.coroutines.flow.Flow"
            if (!function.isSuspend && !isFlowName) {
                addError(KtorGenLogger.SUSPEND_FUNCTION_OR_FLOW + returnTypeName.getShortName(), function)
            }
        }
    }
}
