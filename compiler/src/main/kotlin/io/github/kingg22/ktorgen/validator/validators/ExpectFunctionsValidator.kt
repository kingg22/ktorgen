package io.github.kingg22.ktorgen.validator.validators

import com.google.devtools.ksp.symbol.Modifier
import io.github.kingg22.ktorgen.KtorGenWithoutCoverage
import io.github.kingg22.ktorgen.validator.ValidationContext
import io.github.kingg22.ktorgen.validator.ValidationResult
import io.github.kingg22.ktorgen.validator.ValidatorStrategy

@KtorGenWithoutCoverage // can't test KMP cases
internal class ExpectFunctionsValidator : ValidatorStrategy {
    override val name: String = "Expect Functions"

    override fun ValidationResult.validate(context: ValidationContext) {
        context.expectFunctions.filter { !it.modifiers.contains(Modifier.EXPECT) }.forEach { function ->
            addError(
                "A function annotated with @KtorGenFunctionKmp must be 'expect' in common source set",
                function,
            )
        }
    }
}
