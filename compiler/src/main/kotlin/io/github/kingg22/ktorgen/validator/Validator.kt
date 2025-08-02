package io.github.kingg22.ktorgen.validator

import io.github.kingg22.ktorgen.KtorGenLogger
import io.github.kingg22.ktorgen.KtorGenOptions
import io.github.kingg22.ktorgen.model.ClassData
import io.github.kingg22.ktorgen.validator.validators.*

fun interface Validator {
    fun validate(classData: ClassData, ktorGenOptions: KtorGenOptions, ktorGenLogger: KtorGenLogger): ClassData?

    companion object {
        val DEFAULT by lazy {
            ValidatorPipeline(
                ClassLevelValidator(),
                WildcardParameterValidator(),
                SuspendOrFlowValidator(),
                UrlSyntaxValidator(),
                PathParameterValidator(),
                HeadReturnNothingValidator(),
                HeadersValidator(),
                BodyUsageValidator(),
                FormUrlBodyValidator(),
                MultipartValidator(),
            )
        }

        val NO_OP by lazy { Validator { _, _, _ -> null } }

        val NO_VALIDATION by lazy { Validator { classData, _, _ -> classData } }

        /** Shortcut to get the default validator impl */
        fun validate(classData: ClassData, ktorGenOptions: KtorGenOptions, ktorGenLogger: KtorGenLogger) =
            DEFAULT.validate(classData, ktorGenOptions, ktorGenLogger)
    }
}
