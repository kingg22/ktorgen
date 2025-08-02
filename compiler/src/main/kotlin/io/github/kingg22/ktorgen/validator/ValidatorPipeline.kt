package io.github.kingg22.ktorgen.validator

import io.github.kingg22.ktorgen.KtorGenLogger
import io.github.kingg22.ktorgen.KtorGenOptions
import io.github.kingg22.ktorgen.model.ClassData

class ValidatorPipeline(private val validators: List<ValidatorStrategy>) : Validator {
    init {
        require(validators.isNotEmpty()) { "ValidatorPipeline must have at least one validator" }
    }

    override fun validate(
        classData: ClassData,
        ktorGenOptions: KtorGenOptions,
        ktorGenLogger: KtorGenLogger,
    ): ClassData? {
        val context = ValidationContext(
            className = classData.interfaceName,
            packageName = classData.packageName,
            functions = classData.functions,
            superInterfaces = listOf(), // TODO
            baseUrl = null, // TODO
        )
        return run(context, ktorGenLogger).let { if (it.isOk) classData else null }
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
