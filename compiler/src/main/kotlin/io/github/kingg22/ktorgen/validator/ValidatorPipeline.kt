package io.github.kingg22.ktorgen.validator

import io.github.kingg22.ktorgen.DiagnosticSender
import io.github.kingg22.ktorgen.KtorGenLogger
import io.github.kingg22.ktorgen.KtorGenOptions
import io.github.kingg22.ktorgen.checkImplementation
import io.github.kingg22.ktorgen.model.ClassData
import io.github.kingg22.ktorgen.work

internal class ValidatorPipeline(private val validators: Set<ValidatorStrategy>) : Validator {
    constructor(vararg validators: ValidatorStrategy) : this(validators.toSet())
    init {
        checkImplementation(validators.isNotEmpty()) {
            "${KtorGenLogger.KTOR_GEN} ValidatorPipeline must have at least one validator. This is an implementation errors. "
        }
    }

    override fun validate(
        classData: ClassData,
        ktorGenOptions: KtorGenOptions,
        diagnosticSender: (String) -> DiagnosticSender,
        onFatalError: () -> Unit,
    ): ClassData? {
        // if we don't go to generate it, skip
        if (!classData.goingToGenerate) return null

        val context = ValidationContext(classData)

        val totalErrors = validators.sumOf { validator ->
            val sender = diagnosticSender(validator.name)
            sender.work {
                val result = ValidationResultImpl(sender)
                with (validator) {
                    result.validate(context)
                }
                result.errorCount
            }
        }

        return if (totalErrors == 0) {
            classData
        } else {
            onFatalError()
            null
        }
    }
}
