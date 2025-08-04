package io.github.kingg22.ktorgen.validator

import io.github.kingg22.ktorgen.KtorGenLogger
import io.github.kingg22.ktorgen.KtorGenOptions
import io.github.kingg22.ktorgen.model.ClassData
import io.github.kingg22.ktorgen.validator.validators.*

fun interface Validator {
    fun validate(
        classData: ClassData,
        ktorGenOptions: KtorGenOptions,
        ktorGenLogger: KtorGenLogger,
        onFatalError: () -> Unit,
    ): ClassData?

    companion object {
        val DEFAULT: Validator by lazy {
            ValidatorPipeline(
                ClassLevelValidator(),
                WildcardParameterValidator(),
                SuspendOrFlowValidator(),
                PathParameterValidator(),
                QueryValidator(),
                UrlSyntaxValidator(),
                HeadReturnNothingValidator(),
                HeadersValidator(),
                BodyUsageValidator(),
                FormUrlBodyValidator(),
                MultipartValidator(),
            )
        }

        val NO_OP by lazy { Validator { _, _, _, _ -> null } }

        val NO_VALIDATION by lazy { Validator { classData, _, _, _ -> classData } }
    }
}
