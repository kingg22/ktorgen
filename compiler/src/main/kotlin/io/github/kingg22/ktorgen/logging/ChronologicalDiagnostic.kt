package io.github.kingg22.ktorgen.logging

import com.google.devtools.ksp.symbol.FileLocation
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSValueParameter
import io.github.kingg22.ktorgen.DiagnosticSender
import io.github.kingg22.ktorgen.KtorGenFatalError
import io.github.kingg22.ktorgen.KtorGenWithoutCoverage
import io.github.kingg22.ktorgen.checkImplementation
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.sequences.forEach

// Inspired on Paris Processor timer https://github.com/airbnb/paris/blob/d8b5edbc56253bcdd0d0c57930d2e91113dd0f37/paris-processor/src/main/java/com/airbnb/paris/processor/Timer.kt
@ConsistentCopyVisibility // no copyable
internal data class ChronologicalDiagnostic private constructor(private val root: Step) :
    DiagnosticSender by root,
    DiagnosticSender.DiagnosticHolder {
    constructor(name: String) : this(Step(name, ROOT, null, 0, mutableListOf()))

    /** Generate all the report in mode verbose */
    override fun buildReport(): String = buildString {
        appendFilteredMessagesChronologically { true }
    }

    /** Generate report of all errors, this not mark as finish the root timer */
    override fun buildErrorsMessage(): String = buildString {
        appendLine("❌ Errors found during \"${root.name}\" execution:")
        appendFilteredMessagesChronologically { it.message.type == ERROR }
    }

    /** Generate report of all warnings, this not mark as finish the root timer */
    override fun buildWarningsMessage(): String = buildString {
        appendLine("⚠️ Warnings found during \"${root.name}\" execution:")
        appendFilteredMessagesChronologically { it.message.type == WARNING }
    }

    /** Generate report of errors and warning, this not mark as finish the root timer */
    override fun buildErrorsAndWarningsMessage(): String = buildString {
        appendLine("❌ Errors and ⚠️ Warnings found during \"${root.name}\" execution:")
        appendFilteredMessagesChronologically { it.message.type == WARNING || it.message.type == ERROR }
    }

    private fun iconFor(step: Step): String = when (step.type) {
        PHASE, TASK -> if (step.hasErrors()) "❌" else "✔️"
        else -> step.type.icon
    }

    private fun StringBuilder.appendFilteredMessagesChronologically(filter: (TimestampedMessage) -> Boolean) {
        val filteredMessages = root.globalMessages.filter(filter)

        if (filteredMessages.isEmpty()) {
            appendLine("  No messages found.")
            return
        }

        for (timestampedMsg in filteredMessages) {
            val msg = timestampedMsg.message
            val step = timestampedMsg.step
            val prefix = "|" + "-".repeat(step.depth * 2)

            append("$prefix ${iconFor(step)} ${step.name}: ${msg.type.icon} ${msg.message}")
            msg.generateSymbolInfo.takeIf { it.isNotEmpty() }?.let { symbolInfo ->
                appendLine("    -> $symbolInfo")
            } ?: appendLine()
        }
    }

    private data class TimestampedMessage(
        val message: DiagnosticMessage,
        val step: Step,
        val timestamp: Long = System.nanoTime(),
    )

    private class Step(
        val name: String,
        val type: StepType,
        val parent: Step?,
        val depth: Int,
        val globalMessages: MutableList<TimestampedMessage>,
    ) : DiagnosticSender {
        private var startNanos: Long = 0
        private var endNanos: Long = 0
        val messages = mutableListOf<DiagnosticMessage>()
        val children = mutableListOf<Step>()

        init {
            checkImplementation(parent == null || this != parent) { "Invalid parent for step '$name." }
            if (parent != null) {
                checkImplementation(parent.isInProgress) {
                    "Parent step '${parent.name}' of '$name' not in progress and try to add a child step"
                }
                parent.children.add(this)
            }
        }

        @KtorGenWithoutCoverage // This method is useful while debugging, is not necessary in runtime, I guess
        override fun toString() = (
            "$type $name (${if (isCompleted) formattedDuration() else "Start at: $startNanos, not finished yet"}). \n" +
                "Parent: ${parent?.name ?: "null"}, ${messages.size} messages, ${children.size} children.\n" +
                "Messages: [${messages.joinToString().trim()}]\n" +
                "Children: [${children.joinToString().trim()}]"
            ).trim()

        override val isStarted get() = startNanos != 0L
        override val isFinish get() = endNanos != 0L
        override fun hasErrors(): Boolean = messages.any { it.type == ERROR } || children.any { it.hasErrors() }
        override fun hasWarnings(): Boolean = messages.any { it.type == WARNING } || children.any { it.hasWarnings() }

        override fun start() {
            checkImplementation(!isStarted) { "Step '$name' already started" }
            if (parent != null) {
                checkImplementation(parent.isInProgress) {
                    "Parent step '${parent.name}' of '$name' not in progress and try to start a child step"
                }
            }
            startNanos = System.nanoTime()
        }

        override fun finish() {
            checkImplementation(isStarted) { "Step '$name' not started yet and try to finish" }
            checkImplementation(!isFinish) { "Step '$name' already finish" }
            if (parent != null) {
                checkImplementation(parent.isInProgress) {
                    "Parent step '${parent.name}' of '$name' not in progress and try to finish a child step"
                }
            }
            endNanos = System.nanoTime()
        }

        fun createPhase(name: String): DiagnosticSender = Step(name, PHASE, this, depth + 1, globalMessages)

        override fun createTask(name: String): DiagnosticSender = Step(name, TASK, this, depth + 1, globalMessages)

        override fun addStep(message: String, symbol: KSNode?) {
            val diagnosticMsg = DiagnosticMessage(STEP, message.trim(), symbol)
            messages.add(diagnosticMsg)
            globalMessages.add(TimestampedMessage(diagnosticMsg, this))
        }

        override fun addWarning(message: String, symbol: KSNode?) {
            val diagnosticMsg = DiagnosticMessage(WARNING, message.trim(), symbol)
            messages.add(diagnosticMsg)
            globalMessages.add(TimestampedMessage(diagnosticMsg, this))
        }

        override fun addError(message: String, symbol: KSNode?) {
            val diagnosticMsg = DiagnosticMessage(ERROR, message.trim(), symbol)
            messages.add(diagnosticMsg)
            globalMessages.add(TimestampedMessage(diagnosticMsg, this))
        }

        override fun die(message: String, symbol: KSNode?, cause: Exception?): Nothing {
            var suppressException: Throwable? = null

            try {
                val diagnosticMsg = DiagnosticMessage(ERROR, message.trim(), symbol)
                messages.add(diagnosticMsg)
                globalMessages.add(TimestampedMessage(diagnosticMsg, this))

                // cancel in order of hierarchy
                if (parent == null) {
                    this.retrieveAllChildrenStep() + this
                } else {
                    parent.retrieveAllChildrenStep() + parent
                }
                    .filter { it.isInProgress }
                    .forEach { it.finish() }
            } catch (e: KtorGenFatalError) {
                // rethrow if is caught here
                throw e
            } catch (e: Throwable) {
                suppressException = e
            }
            val exception = KtorGenFatalError(message, cause)
            suppressException?.let { exception.addSuppressed(suppressException) }
            throw exception
        }

        fun formattedDuration(): String {
            if (endNanos == 0L || startNanos == 0L) return "--"
            val ms = (endNanos - startNanos).div(1_000_000.0).roundTo(3)
            return "$ms ms"
        }

        private fun retrieveAllChildrenStep(): Sequence<DiagnosticSender> = sequence {
            yieldAll(children.flatMap { it.retrieveAllChildrenStep() })
            yieldAll(children)
        }

        private fun Double.roundTo(numFractionDigits: Int): Double {
            val factor = 10.0.pow(numFractionDigits.toDouble())
            return (this * factor).roundToInt() / factor
        }

        @KtorGenWithoutCoverage
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Step

            if (parent != other.parent) return false
            if (startNanos != other.startNanos) return false
            if (endNanos != other.endNanos) return false
            if (name != other.name) return false
            if (type != other.type) return false
            if (messages != other.messages) return false
            if (children != other.children) return false

            return true
        }

        @KtorGenWithoutCoverage
        override fun hashCode(): Int {
            var result = startNanos.hashCode()
            result = 31 * result + parent.hashCode()
            result = 31 * result + endNanos.hashCode()
            result = 31 * result + name.hashCode()
            result = 31 * result + type.hashCode()
            result = 31 * result + messages.hashCode()
            result = 31 * result + children.hashCode()
            return result
        }
    }

    private data class DiagnosticMessage(val type: MessageType, val message: String, private val symbol: KSNode?) {
        // invoke to get the info of location during initialization
        val generateSymbolInfo: String = run {
            if (symbol == null) return@run ""
            if (symbol.location is FileLocation) {
                val fileLocation = symbol.location as? FileLocation
                val fileName = fileLocation?.filePath?.substringAfterLast('/')
                val line = fileLocation?.lineNumber

                if (symbol is KSDeclaration) {
                    val symbolName = symbol.qualifiedName?.asString() ?: UNKNOWN_SYMBOL
                    return@run "at $symbolName($fileName:$line)"
                } else if (symbol is KSValueParameter) {
                    val type = symbol.type.resolve()
                    val vararg = if (symbol.isVararg) "vararg" else ""
                    val function = symbol.parent as? KSDeclaration
                    val functionName = function?.qualifiedName?.asString() ?: UNKNOWN_SYMBOL
                    return@run "on parameter '$vararg ${symbol.name?.asString() ?: UNKNOWN_SYMBOL}: " +
                        "${type.declaration.simpleName.asString()}${if (type.isMarkedNullable) "?" else ""}' " +
                        "at $functionName($fileName:$line)"
                }
            }
            return@run ""
        }

        override fun toString() = "$type: $message $generateSymbolInfo".trim()

        private companion object {
            private const val UNKNOWN_SYMBOL = "<unknown>"
        }
    }

    private enum class MessageType(val icon: String) {
        WARNING("⚠️"),
        ERROR("❌"),
        STEP("🟢"),
        ;

        override fun toString(): String = "$icon $name"
    }

    private enum class StepType(val label: String, val icon: String) {
        ROOT("Processor", "⚙️"),
        PHASE("Phase", "ℹ️"),
        TASK("Task", "🛠️"),
        ;

        override fun toString(): String = "$icon $label"
    }
}
