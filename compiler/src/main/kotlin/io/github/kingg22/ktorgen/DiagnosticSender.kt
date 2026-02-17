package io.github.kingg22.ktorgen

import com.google.devtools.ksp.symbol.KSNode

/**
 * A diagnostic sender to manage and report diagnostic messages during a process execution.
 * Provides methods to track the state of the process, manage child tasks, log messages, warnings, and errors,
 * and generate diagnostic reports.
 */
@KtorGenWithoutCoverage // Kover include default values to cover, but that is not necessary, the impl class is covered
interface DiagnosticSender {
    val isStarted: Boolean
    val isFinish: Boolean
    val isInProgress get() = isStarted && !isFinish
    val isCompleted get() = isStarted && isFinish
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
    fun die(message: String, symbol: KSNode? = null, cause: Exception? = null): Nothing

    /** A concrete sender can hold and produce reports */
    interface DiagnosticHolder : DiagnosticSender {
        @Deprecated("Use createTask instead", ReplaceWith("createTask(name)"))
        fun createPhase(name: String): DiagnosticSender

        /** Generate report of all messages, this mark as finish the root timer */
        fun buildReport(): String

        /** Generate report of all errors, this not mark as finish the root timer */
        fun buildErrorsMessage(): String

        /** Generate report of all warnings, this not mark as finish the root timer */
        fun buildWarningsMessage(): String

        /** Generate report of errors and warning, this not mark as finish the root timer */
        fun buildErrorsAndWarningsMessage(): String
    }
}
