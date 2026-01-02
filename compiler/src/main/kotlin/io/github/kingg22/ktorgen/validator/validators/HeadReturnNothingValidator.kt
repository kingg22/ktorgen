package io.github.kingg22.ktorgen.validator.validators

import com.squareup.kotlinpoet.UNIT
import io.github.kingg22.ktorgen.KtorGenLogger
import io.github.kingg22.ktorgen.model.annotations.HttpMethod
import io.github.kingg22.ktorgen.validator.ValidationContext
import io.github.kingg22.ktorgen.validator.ValidationResult
import io.github.kingg22.ktorgen.validator.ValidatorStrategy

internal class HeadReturnNothingValidator : ValidatorStrategy {
    override val name: String = "HTTP method 'Head'"

    override fun ValidationResult.validate(context: ValidationContext) {
        context.functions.filter { function ->
            function.goingToGenerate &&
                function.httpMethodAnnotation.httpMethod == HttpMethod.Head &&
                function.returnTypeData.typeName != UNIT
        }.forEach { function ->
            addError(KtorGenLogger.HTTP_METHOD_HEAD_NOT_RETURN_BODY, function)
        }
    }
}
