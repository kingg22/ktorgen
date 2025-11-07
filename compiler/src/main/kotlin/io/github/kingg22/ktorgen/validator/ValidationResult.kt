package io.github.kingg22.ktorgen.validator

import com.google.devtools.ksp.symbol.KSNode
import io.github.kingg22.ktorgen.DiagnosticSender
import io.github.kingg22.ktorgen.KtorGenLogger
import io.github.kingg22.ktorgen.model.ClassData
import io.github.kingg22.ktorgen.model.FunctionData
import io.github.kingg22.ktorgen.model.ParameterData

/** This a container of errors and warnings, not couple the validation strategies with diagnostic sender */
class ValidationResult() {
    private val errors: MutableList<FatalError> = mutableListOf()
    private val warnings: MutableList<Warning> = mutableListOf()

    val errorCount get() = errors.size

    constructor(block: ValidationResult.() -> Unit) : this() {
        block(this)
    }

    fun addError(message: String, classData: ClassData) = addError(message, classData.ksClassDeclaration)
    fun addError(message: String, functionData: FunctionData) = addError(message, functionData.ksFunctionDeclaration)
    fun addError(message: String, parameterData: ParameterData) = addError(message, parameterData.ksValueParameter)
    fun addError(message: String, symbol: KSNode? = null) {
        errors.add(FatalError(message.removePrefix(KtorGenLogger.KTOR_GEN), symbol))
    }

    fun addWarning(message: String, classData: ClassData) = addWarning(message, classData.ksClassDeclaration)
    fun addWarning(message: String, functionData: FunctionData) =
        addWarning(message, functionData.ksFunctionDeclaration)
    fun addWarning(message: String, parameterData: ParameterData) = addWarning(message, parameterData.ksValueParameter)
    fun addWarning(message: String, symbol: KSNode? = null) {
        warnings.add(Warning(message.removePrefix(KtorGenLogger.KTOR_GEN), symbol))
    }

    fun dump(logger: DiagnosticSender) {
        errors.forEach { logger.addError(it.message, it.symbol) }
        warnings.forEach { logger.addWarning(it.message, it.symbol) }
    }

    private class FatalError(val message: String, val symbol: KSNode?)
    private class Warning(val message: String, val symbol: KSNode?)
}
