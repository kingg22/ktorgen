package io.github.kingg22.ktorgen

import com.google.devtools.ksp.symbol.FileLocation
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSValueParameter
import kotlin.math.pow
import kotlin.math.roundToInt

// Inspired on Paris Processor timer https://github.com/airbnb/paris/blob/d8b5edbc56253bcdd0d0c57930d2e91113dd0f37/paris-processor/src/main/java/com/airbnb/paris/processor/Timer.kt
class DiagnosticTimer(name: String, private val onDebugLog: (String) -> Unit) : DiagnosticSender {
    private val root = Step(name, StepType.ROOT)
    private val rootStarted get() = root.isStarted()

    override fun start() {
        check(!rootStarted) { "DiagnosticTimer on root already started" }
        root.start()
    }

    /** Factory for inner phases */
    fun createPhase(name: String): DiagnosticSender = Step(name, StepType.PHASE).apply {
        root.addChild(this)
    }

    /** Add step on root phase */
    fun addStep(message: String) {
        root.addStep(message)
    }

    override fun isStarted(): Boolean = root.isStarted()

    override fun isFinish() = root.isFinish()
    override fun hasErrors(): Boolean = root.hasErrors()
    override fun hasWarnings(): Boolean = root.hasWarnings()
    override fun toString(): String = root.toString()

    /** Generate all the report in mode verbose */
    fun buildReport(): String = buildString { printStep(root, "") }

    /** Generate report of all errors, this not mark as finish the root timer */
    fun buildErrorsMessage() = buildString {
        appendLine("❌ Errors found during \"${root.name}\" execution:")
        appendFilteredSteps(root, 0) { it.type == MessageType.ERROR }
    }

    /** Generate report of all warnings, this not mark as finish the root timer */
    fun buildWarningsMessage() = buildString {
        appendLine("⚠️ Warnings found during \"${root.name}\" execution:")
        appendFilteredSteps(root, 0) { it.type == MessageType.WARNING }
    }

    /** Generate report of errors and warning, this not mark as finish the root timer */
    fun buildErrorsAndWarningsMessage() = buildString {
        appendLine("❌ Errors and ⚠️ Warnings found during \"${root.name}\" execution:")
        appendFilteredSteps(root, 0) { it.type == MessageType.WARNING || it.type == MessageType.ERROR }
    }

    /** Finish the root timer */
    override fun finish() = root.finish()
    override fun createTask(name: String): DiagnosticSender = root.createTask(name)
    override fun addStep(message: String, symbol: KSNode?) = root.addStep(message, symbol)
    override fun addWarning(message: String, symbol: KSNode?) = root.addWarning(message, symbol)
    override fun addError(message: String, symbol: KSNode?) = root.addError(message, symbol)
    override fun die(message: String, symbol: KSNode?, exception: Exception?): Nothing =
        root.die(message, symbol, exception)

    private fun StringBuilder.printStep(step: Step, indent: String) {
        val icon = iconFor(step)
        val label = step.type.label

        appendLine("$indent$icon $label: ${step.name} completed in (${step.formattedDuration()})")
        step.printDiagnostic(this, indent)

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
                appendLine("$indent  ${msg.type.icon}${msg.type}: ${msg.message}")
                msg.generateSymbolInfo.takeIf(String::isNotEmpty)?.let { symbolInfo ->
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

        override fun toString() = (
            "${type.label} ${type.icon} $name " +
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
            check((this == root || rootStarted) && !isStarted()) { "Step $name already started" }
            startNanos = System.nanoTime()
        }

        override fun finish() {
            check(rootStarted && isStarted()) { "Step $name not started yet" }
            check(!isFinish()) { "Step $name already finish" }
            if (!children.all(DiagnosticSender::isCompleted)) {
                onDebugLog(
                    "A children step of $name is not completed. " +
                        "Children status: " +
                        "${children.count(DiagnosticSender::isCompleted)}/${children.size} completed." +
                        "This is an implementation errors.",
                )
            }
            endNanos = System.nanoTime()
        }

        fun addChild(step: Step) {
            children.add(step)
        }

        override fun createTask(name: String): DiagnosticSender = Step(name, StepType.TASK).also { task ->
            addChild(task)
        }

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
                    .filter(DiagnosticSender::isStarted)
                    .filter(DiagnosticSender::isInProgress)
                    .forEach(DiagnosticSender::finish)
            } catch (e: Throwable) {
                suppressException = e
            }
            val exception = Throwable("Fatal error occurred. \n${buildErrorsAndWarningsMessage()}", exception)
            suppressException?.let { exception.addSuppressed(suppressException) }
            throw exception
        }

        fun printDiagnostic(builder: StringBuilder, indent: String) {
            for (msg in messages) {
                val emoji = msg.type.icon
                val prefix = "    "
                builder.appendLine("$indent$prefix$emoji ${msg.type.name}: ${msg.message}${msg.generateSymbolInfo}")
            }
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
    }

    private class DiagnosticMessage(val type: MessageType, val message: String, private val symbol: KSNode?) {
        val generateSymbolInfo: String by lazy {
            if (symbol == null) return@lazy ""
            if (symbol.location is FileLocation) {
                val fileLocation = symbol.location as? FileLocation
                val fileName = fileLocation?.filePath?.substringAfterLast('/')
                val line = fileLocation?.lineNumber

                if (symbol is KSDeclaration) {
                    val symbolName = symbol.qualifiedName?.asString() ?: "<unknown>"
                    return@lazy "at $symbolName($fileName:$line)"
                } else if (symbol is KSValueParameter) {
                    val type = symbol.type.resolve()
                    val vararg = if (symbol.isVararg) "vararg" else ""
                    val function = symbol.parent as? KSDeclaration
                    val functionName = function?.qualifiedName?.asString() ?: ""
                    return@lazy "Parameter: $vararg ${symbol.name?.asString() ?: "<unknown>"}: " +
                        "${type.declaration.simpleName.asString()}${if (type.isMarkedNullable) "?" else ""} " +
                        "at $functionName($fileName:$line)"
                }
            }
            return@lazy ""
        }

        init {
            // invoke to cache the info of location during initialization
            generateSymbolInfo
        }

        override fun toString() = "$type ${type.icon}: $message $generateSymbolInfo".trim()
    }

    private enum class MessageType(val icon: String) {
        WARNING("⚠️"),
        ERROR("❌"),
        STEP("🟢"),
    }

    private enum class StepType(val label: String, val icon: String) {
        ROOT("Processor", "⚙️"),
        PHASE("Phase", "ℹ️"),
        TASK("Task", "🛠️"),
    }
}
