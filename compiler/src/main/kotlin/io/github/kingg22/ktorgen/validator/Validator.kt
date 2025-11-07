package io.github.kingg22.ktorgen.validator

import io.github.kingg22.ktorgen.DiagnosticSender
import io.github.kingg22.ktorgen.KtorGenOptions
import io.github.kingg22.ktorgen.model.ClassData
import io.github.kingg22.ktorgen.validator.validators.*

fun interface Validator {
    fun validate(
        classData: ClassData,
        ktorGenOptions: KtorGenOptions,
        diagnosticSender: (String) -> DiagnosticSender,
        onFatalError: () -> Unit,
    ): ClassData?

    companion object {
        @JvmStatic
        val DEFAULT: Validator by lazy {
            ValidatorPipeline(
                ClassLevelValidator(),
                ExpectFunctionsValidator(),
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
                CookieValidator(),
            )
        }

        @JvmStatic
        val NO_OP by lazy { Validator { _, _, _, _ -> null } }

        @JvmStatic
        val NO_VALIDATION by lazy { Validator { classData, _, _, _ -> classData } }
    }
}
