package io.github.kingg22.ktorgen.validator.validators

import com.squareup.kotlinpoet.UNIT
import io.github.kingg22.ktorgen.KtorGenLogger
import io.github.kingg22.ktorgen.model.annotations.HttpMethod
import io.github.kingg22.ktorgen.validator.ValidationContext
import io.github.kingg22.ktorgen.validator.ValidationResult
import io.github.kingg22.ktorgen.validator.ValidatorStrategy

class HeadReturnNothingValidator : ValidatorStrategy {
    override val name: String = "HTTP method 'Head'"

    override fun validate(context: ValidationContext) = ValidationResult {
        context.functions.filter {
            it.goingToGenerate &&
                it.httpMethodAnnotation.httpMethod == HttpMethod.Head &&
                it.returnTypeData.typeName != UNIT
        }.forEach { function ->
            addError(KtorGenLogger.HTTP_METHOD_HEAD_NOT_RETURN_BODY, function)
        }
    }
}
