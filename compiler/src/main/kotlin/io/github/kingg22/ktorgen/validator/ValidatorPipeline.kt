package io.github.kingg22.ktorgen.validator

import io.github.kingg22.ktorgen.DiagnosticTimer
import io.github.kingg22.ktorgen.KtorGenLogger
import io.github.kingg22.ktorgen.KtorGenOptions
import io.github.kingg22.ktorgen.model.ClassData

class ValidatorPipeline(private val validators: Set<ValidatorStrategy>) : Validator {
    constructor(vararg validators: ValidatorStrategy) : this(validators.toSet())
    init {
        require(validators.isNotEmpty()) {
            "${KtorGenLogger.KTOR_GEN} ValidatorPipeline must have at least one validator. This is an implementation errors. "
        }
    }

    override fun validate(
        classData: ClassData,
        ktorGenOptions: KtorGenOptions,
        diagnosticSender: (String) -> DiagnosticTimer.DiagnosticSender,
        onFatalError: () -> Unit,
    ): ClassData? {
        // if we don't go to generate it, skip
        if (classData.goingToGenerate.not()) return null

        val context = ValidationContext(
            className = classData.interfaceName,
            packageName = classData.packageNameString,
            functions = classData.functions.filter { it.goingToGenerate },
            visibility = classData.visibilityModifier,
            baseUrl = null, // TODO
        )
        var errorCount = 0

        for (validator in validators) {
            val sender = diagnosticSender(validator.name)
            sender.start()
            val result = validator.validate(context)
            sender.finish()
            result.dump(sender)
            errorCount += result.errorCount
        }

        return if (errorCount == 0) {
            classData
        } else {
            onFatalError()
            null
        }
    }
}
