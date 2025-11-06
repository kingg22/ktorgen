package io.github.kingg22.ktorgen

import com.google.devtools.ksp.symbol.FileLocation
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSValueParameter
import kotlin.math.pow
import kotlin.math.roundToInt

// Inspired on Paris Processor timer https://github.com/airbnb/paris/blob/d8b5edbc56253bcdd0d0c57930d2e91113dd0f37/paris-processor/src/main/java/com/airbnb/paris/processor/Timer.kt
@Suppress("kotlin:S6514") // Can't use delegation because Step is inner class
internal class DiagnosticTimer(name: String, private val onDebugLog: (String) -> Unit) : DiagnosticSender {
    private val root = Step(name, StepType.ROOT)
    private val rootStarted
        inline get() = root.isStarted()

    override fun start() {
        checkImplementation(!rootStarted) { "DiagnosticTimer on root already started" }
        root.start()
    }

    override fun isStarted(): Boolean = root.isStarted()

    /** Finish the root timer */
    override fun finish() = root.finish()
    override fun createTask(name: String): DiagnosticSender = root.createTask(name)
    override fun addStep(message: String, symbol: KSNode?) = root.addStep(message, symbol)
    override fun addWarning(message: String, symbol: KSNode?) = root.addWarning(message, symbol)
    override fun addError(message: String, symbol: KSNode?) = root.addError(message, symbol)
    override fun die(message: String, symbol: KSNode?, exception: Exception?) = root.die(message, symbol, exception)
    override fun isFinish() = root.isFinish()
    override fun hasErrors(): Boolean = root.hasErrors()
    override fun hasWarnings(): Boolean = root.hasWarnings()
    override fun toString(): String = root.toString()

    override fun equals(other: Any?): Boolean =
        other is DiagnosticTimer && other.root == root && other.onDebugLog == onDebugLog

    override fun hashCode(): Int {
        var result = root.hashCode()
        result = 31 * result + onDebugLog.hashCode()
        return result
    }

    /** Factory for inner phases */
    internal fun createPhase(name: String): DiagnosticSender = Step(name, StepType.PHASE, root)

    /** Generate all the report in mode verbose */
    internal fun buildReport(): String = buildString { printStep(root, "") }

    /** Generate report of all errors, this not mark as finish the root timer */
    internal fun buildErrorsMessage() = buildString {
        appendLine("❌ Errors found during \"${root.name}\" execution:")
        appendFilteredSteps(root, 0) { it.type == MessageType.ERROR }
    }

    /** Generate report of all warnings, this not mark as finish the root timer */
    internal fun buildWarningsMessage() = buildString {
        appendLine("⚠️ Warnings found during \"${root.name}\" execution:")
        appendFilteredSteps(root, 0) { it.type == MessageType.WARNING }
    }

    /** Generate report of errors and warning, this not mark as finish the root timer */
    internal fun buildErrorsAndWarningsMessage() = buildString {
        appendLine("❌ Errors and ⚠️ Warnings found during \"${root.name}\" execution:")
        appendFilteredSteps(root, 0) { it.type == MessageType.WARNING || it.type == MessageType.ERROR }
    }

    private fun StringBuilder.printStep(step: Step, indent: String) {
        val icon = iconFor(step)
        val label = step.type.label

        appendLine("$indent$icon $label: ${step.name} completed in (${step.formattedDuration()})")
        for (msg in step.messages) {
            this.appendLine("$indent    $msg")
        }

        for (child in step.children) {
            printStep(child, "$indent    ")
        }
    }

    private fun iconFor(step: Step): String = when (step.type) {
        StepType.PHASE, StepType.TASK -> if (step.hasErrors()) "❌" else "✔️"
        else -> step.type.icon
    }

    private fun StringBuilder.appendFilteredSteps(
        step: Step,
        indentLevel: Int,
        filter: (DiagnosticMessage) -> Boolean,
    ) {
        val indent = "  ".repeat(indentLevel)
        val messages = step.messages.filter(filter)
        val hasRelevantChildren = step.children.any { it.containsMessagesRecursively(filter) }

        if (messages.isNotEmpty() || hasRelevantChildren) {
            appendLine("$indent${iconFor(step)} ${step.name} (${step.formattedDuration()})")
            messages.forEach { msg ->
                appendLine("$indent  ${msg.type}: ${msg.message}")
                msg.generateSymbolInfo.takeIf { it.isNotEmpty() }?.let { symbolInfo ->
                    appendLine("$indent    -> $symbolInfo")
                }
            }
            step.children.forEach { child -> this.appendFilteredSteps(child, indentLevel + 1, filter) }
        }
    }

    private fun Step.containsMessagesRecursively(filter: (DiagnosticMessage) -> Boolean): Boolean =
        messages.any(filter) ||
            children.any { it.containsMessagesRecursively(filter) }

    private inner class Step(val name: String, val type: StepType) : DiagnosticSender {
        private var startNanos: Long = 0
        private var endNanos: Long = 0
        val messages = mutableListOf<DiagnosticMessage>()
        val children = mutableListOf<Step>()

        constructor(name: String, type: StepType, parent: Step) : this(name, type) {
            parent.children.add(this)
        }

        override fun toString() = (
            "$type $name " +
                "(${if (isCompleted()) formattedDuration() else "Start at: $startNanos, not finished yet"}). " +
                "${messages.size} messages, ${children.size} children.\n" +
                "Messages: [${messages.joinToString().trim()}]\n" +
                "Children: [${children.joinToString().trim()}]"
            ).trim()

        override fun isStarted() = startNanos != 0L
        override fun isFinish() = endNanos != 0L
        override fun hasErrors(): Boolean =
            messages.any { it.type == MessageType.ERROR } || children.any { it.hasErrors() }
        override fun hasWarnings(): Boolean =
            messages.any { it.type == MessageType.WARNING } || children.any { it.hasWarnings() }

        override fun start() {
            checkImplementation((this == root || rootStarted) && !isStarted()) { "Step $name already started" }
            startNanos = System.nanoTime()
        }

        override fun finish() {
            checkImplementation(rootStarted && isStarted()) { "Step $name not started yet" }
            checkImplementation(!isFinish()) { "Step $name already finish" }
            endNanos = System.nanoTime()
            if (!children.all { it.isCompleted() }) {
                onDebugLog(
                    "A children step of $name is not completed. " +
                        "Children status: " +
                        "${children.count { it.isCompleted() }}/${children.size} completed." +
                        "This is an implementation errors.",
                )
            }
        }

        override fun createTask(name: String): DiagnosticSender = Step(name, StepType.TASK, this)

        override fun addStep(message: String, symbol: KSNode?) {
            messages.add(DiagnosticMessage(MessageType.STEP, message.trim(), symbol))
        }

        override fun addWarning(message: String, symbol: KSNode?) {
            messages.add(DiagnosticMessage(MessageType.WARNING, message.trim(), symbol))
        }

        override fun addError(message: String, symbol: KSNode?) {
            messages.add(DiagnosticMessage(MessageType.ERROR, message.trim(), symbol))
        }

        override fun die(message: String, symbol: KSNode?, exception: Exception?): Nothing {
            messages.add(DiagnosticMessage(MessageType.ERROR, message.trim(), symbol))
            var suppressException: Throwable? = null

            try {
                // cancel in order of hierarchy
                (root.retrieveAllChildrenStep() + root)
                    .filter { it.isStarted() }
                    .filter { it.isInProgress() }
                    .forEach { it.finish() }
            } catch (e: KtorGenFatalError) {
                // rethrow if is caught here
                throw e
            } catch (e: Throwable) {
                suppressException = e
            }
            val exception = KtorGenFatalError(buildErrorsAndWarningsMessage(), exception)
            suppressException?.let { exception.addSuppressed(suppressException) }
            throw exception
        }

        fun formattedDuration(): String {
            if (endNanos == 0L || startNanos == 0L) return "--"
            val ms = (endNanos - startNanos).div(1_000_000.0).roundTo(3)
            return "$ms ms"
        }

        private fun retrieveAllChildrenStep(): List<DiagnosticSender> =
            this.children.flatMap { it.retrieveAllChildrenStep() } + this.children

        private fun Double.roundTo(numFractionDigits: Int): Double {
            val factor = 10.0.pow(numFractionDigits.toDouble())
            return (this * factor).roundToInt() / factor
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Step

            if (startNanos != other.startNanos) return false
            if (endNanos != other.endNanos) return false
            if (name != other.name) return false
            if (type != other.type) return false
            if (messages != other.messages) return false
            if (children != other.children) return false

            return true
        }

        override fun hashCode(): Int {
            var result = startNanos.hashCode()
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

    private enum class MessageType(private val icon: String) {
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
