package io.github.kingg22.ktorgen.validator

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
        ktorGenLogger: KtorGenLogger,
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
        return run(context, ktorGenLogger).let {
            if (it.isOk) {
                classData
            } else {
                onFatalError()
                null
            }
        }
    }

    private fun run(context: ValidationContext, logger: KtorGenLogger): ValidationResult {
        val finalResult = ValidationResult()
        for (validator in validators) {
            val result = validator.validate(context)
            finalResult.errors.addAll(result.errors)
            finalResult.warnings.addAll(result.warnings)
        }
        finalResult.dump(logger)
        return finalResult
    }
}
