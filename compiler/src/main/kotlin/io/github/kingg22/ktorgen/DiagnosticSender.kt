package io.github.kingg22.ktorgen

import com.google.devtools.ksp.symbol.KSNode

interface DiagnosticSender {
    fun isStarted(): Boolean
    fun isFinish(): Boolean
    fun isInProgress() = isStarted() && !isFinish()
    fun isCompleted() = isStarted() && isFinish()
    fun hasErrors(): Boolean
    fun hasWarnings(): Boolean
    fun hasErrorsOrWarnings() = hasErrors() || hasWarnings()

    /** Start the step. Only called on start and after root is started */
    fun start()

    /** Finish the step. Only called on finish and after root is started */
    fun finish()

    /** Crea a child task, **need** start it */
    fun createTask(name: String): DiagnosticSender

    /** Informational message of the process */
    fun addStep(message: String, symbol: KSNode? = null)

    /** Notice a warn message, can be related to a symbol */
    fun addWarning(message: String, symbol: KSNode? = null)

    /** No fatal error report, can be related to a symbol */
    fun addError(message: String, symbol: KSNode? = null)

    /** Fatal error, can be related to a symbol. This is a controlled exception. */
    fun die(message: String, symbol: KSNode? = null, exception: Exception?): Nothing
}

/** Run all the work in try-catch-finally, this handle start finish and die when throw exceptions */
inline fun <R> DiagnosticSender.work(block: (DiagnosticSender) -> R): R = try {
    start()
    val result = block(this)
    finish()
    result
} catch (e: Exception) {
    die(e.message ?: "", null, e)
}

/** If the condition is false, die */
@Suppress("NOTHING_TO_INLINE")
inline fun DiagnosticSender.require(condition: Boolean, message: String, symbol: KSNode? = null) {
    if (!condition) die(message, symbol, null)
}

/** If the value is null, die */
@Suppress("NOTHING_TO_INLINE")
inline fun <T> DiagnosticSender.requireNotNull(value: T?, message: String, symbol: KSNode? = null): T {
    if (value == null) die(message, symbol, null)
    return value
}
