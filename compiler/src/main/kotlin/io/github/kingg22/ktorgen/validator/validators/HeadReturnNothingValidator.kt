package io.github.kingg22.ktorgen.validator.validators

import com.squareup.kotlinpoet.UNIT
import io.github.kingg22.ktorgen.KtorGenLogger
import io.github.kingg22.ktorgen.model.annotations.HttpMethod
import io.github.kingg22.ktorgen.validator.ValidationContext
import io.github.kingg22.ktorgen.validator.ValidationResult
import io.github.kingg22.ktorgen.validator.ValidatorStrategy

class HeadReturnNothingValidator : ValidatorStrategy {
    override fun validate(context: ValidationContext) = ValidationResult {
        for (function in context.functions) {
            if (function.httpMethodAnnotation.httpMethod != HttpMethod.Head) continue
            if (function.returnTypeData.typeName != UNIT) {
                addError(KtorGenLogger.HTTP_METHOD_HEAD_NOT_RETURN_BODY + addDeclaration(context, function))
            }
        }
    }
}
