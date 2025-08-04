package io.github.kingg22.ktorgen.validator.validators

import io.github.kingg22.ktorgen.KtorGenLogger
import io.github.kingg22.ktorgen.validator.ValidationContext
import io.github.kingg22.ktorgen.validator.ValidationResult
import io.github.kingg22.ktorgen.validator.ValidatorStrategy

class WildcardParameterValidator : ValidatorStrategy {
    override fun validate(context: ValidationContext) = ValidationResult {
        for (function in context.functions) {
            if (function.ksFunctionDeclaration.typeParameters.isNotEmpty()) {
                addError(
                    KtorGenLogger.FUNCTION_OR_PARAMETERS_TYPES_MUST_NOT_INCLUDE_TYPE_VARIABLE_OR_WILDCARD +
                        function.name,
                )
            }
        }
    }
}
