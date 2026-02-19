package io.github.kingg22.ktorgen.validator

import com.google.devtools.ksp.symbol.KSNode
import io.github.kingg22.ktorgen.DiagnosticSender
import io.github.kingg22.ktorgen.KtorGenLogger
import io.github.kingg22.ktorgen.model.ClassData
import io.github.kingg22.ktorgen.model.FunctionData
import io.github.kingg22.ktorgen.model.ParameterData

interface ValidationResult {
    fun addError(message: String, classData: ClassData) = addError(message, classData.ksInterface)
    fun addError(message: String, functionData: FunctionData) = addError(message, functionData.ksFunctionDeclaration)
    fun addError(message: String, parameterData: ParameterData) = addError(message, parameterData.ksValueParameter)
    fun addError(message: String, symbol: KSNode? = null)

    fun addWarning(message: String, classData: ClassData) = addWarning(message, classData.ksInterface)
    fun addWarning(message: String, functionData: FunctionData) =
        addWarning(message, functionData.ksFunctionDeclaration)
    fun addWarning(message: String, parameterData: ParameterData) = addWarning(message, parameterData.ksValueParameter)
    fun addWarning(message: String, symbol: KSNode? = null)
}

/** This a container of errors and warnings, not couple the validation strategies with diagnostic sender */
class ValidationResultImpl(private val sender: DiagnosticSender) : ValidationResult {
    /*
    @get:VisibleForTesting
    val errors: MutableList<FatalError> = mutableListOf()

    @get:VisibleForTesting
    val warnings: MutableList<Warning> = mutableListOf()
     */
    var errorCount: Int = 0
        private set

    override fun addError(message: String, symbol: KSNode?) {
        errorCount++
        sender.addError(message.removePrefix(KtorGenLogger.KTOR_GEN), symbol)
    }

    override fun addWarning(message: String, symbol: KSNode?) {
        sender.addWarning(message.removePrefix(KtorGenLogger.KTOR_GEN), symbol)
    }
/*
    @VisibleForTesting
    class FatalError(val message: String, val symbol: KSNode?)

    @VisibleForTesting
    class Warning(val message: String, val symbol: KSNode?)
 */
}
