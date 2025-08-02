package io.github.kingg22.ktorgen.validator

import io.github.kingg22.ktorgen.KtorGenLogger

class ValidationResult(
    val errors: MutableList<FatalError> = mutableListOf(),
    val warnings: MutableList<Warning> = mutableListOf(),
) {
    constructor(block: ValidationResult.() -> Unit) : this() {
        block(this)
    }

    val isOk get() = errors.isEmpty()

    fun addError(message: String) {
        if (!message.startsWith(KtorGenLogger.KTOR_GEN)) {
            errors.add(FatalError("${KtorGenLogger.KTOR_GEN} $message"))
        } else {
            errors.add(FatalError(message))
        }
    }

    fun addWarning(message: String) {
        warnings.add(Warning(message))
    }

    fun dump(logger: KtorGenLogger = KtorGenLogger.instance) {
        if (errors.isNotEmpty()) {
            errors.forEach { logger.exception(IllegalArgumentException(it.message)) }
        }
        if (warnings.isNotEmpty()) {
            warnings.forEach { logger.error(it.message) }
        }
    }

    class FatalError(val message: String)
    class Warning(val message: String)
}
