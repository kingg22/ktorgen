package io.github.kingg22.ktorgen.validator.validators

import io.github.kingg22.ktorgen.KtorGenLogger
import io.github.kingg22.ktorgen.validator.ValidationContext
import io.github.kingg22.ktorgen.validator.ValidationResult
import io.github.kingg22.ktorgen.validator.ValidatorStrategy

internal class WildcardParameterValidator : ValidatorStrategy {
    override val name: String = "Wildcard Parameter"

    override fun validate(context: ValidationContext) = ValidationResult {
        context.functions
            .filter {
                it.goingToGenerate && it.ksFunctionDeclaration.typeParameters.isNotEmpty()
            }.forEach { function ->
                addError(
                    KtorGenLogger.FUNCTION_OR_PARAMETERS_TYPES_MUST_NOT_INCLUDE_TYPE_VARIABLE_OR_WILDCARD,
                    function,
                )
            }
    }
}
