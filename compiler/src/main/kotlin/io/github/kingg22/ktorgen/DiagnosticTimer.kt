package io.github.kingg22.ktorgen

import com.google.devtools.ksp.symbol.FileLocation
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSNode
import kotlin.math.pow
import kotlin.math.roundToInt

// Inspired on Paris Processor timer https://github.com/airbnb/paris/blob/d8b5edbc56253bcdd0d0c57930d2e91113dd0f37/paris-processor/src/main/java/com/airbnb/paris/processor/Timer.kt
class DiagnosticTimer(name: String, private val logger: KtorGenLogger) {
    private val root = Step(name, StepType.ROOT)
    private var started = false

    fun start(): DiagnosticTimer {
        check(!started) { "DiagnosticTimer on root already started" }
        started = true
        root.start()
        return this
    }

    /** Factory for inner phases */
    fun createPhase(name: String): DiagnosticSender {
        val phase = Step(name, StepType.PHASE)
        root.addChild(phase)
        return phase
    }

    /** Add step on root phase */
    fun addStep(message: String) {
        root.addStep(message)
    }

    fun isFinish() = root.isFinish()

    /** Print all the report in mode verbose to logger in level INFO */
    fun dumpReport() {
        logger.info(buildString { printStep(this, root, "") })
    }

    /** Print all errors to logger in level ERROR, this not mark as finish the root timer */
    fun dumpErrors() {
        val message = buildString {
            appendLine("❌ Errors found during \"${root.name}\" execution:")
            appendFilteredSteps(root, this, 0) { it.type == StepType.ERROR }
        }
        logger.error(message, null)
    }

    /** Print all warnings to logger in level ERROR (options modify it as warn), this not mark as finish the root timer */
    fun dumpWarnings() {
        val message = buildString {
            appendLine("⚠️ Warnings found during \"${root.name}\" execution:")
            appendFilteredSteps(root, this, 0) { it.type == StepType.WARNING }
        }
        logger.error(message, null)
    }

    /** Print errors and warning to logger in level EXCEPTION, this not mark as finish the root timer */
    fun dumpErrorsAndWarnings() {
        val header = buildString {
            appendLine("❌ Errors and Warnings found during \"${root.name}\" execution:")
            appendFilteredSteps(root, this, 0) { it.type == StepType.WARNING || it.type == StepType.ERROR }
        }
        logger.exception(Throwable(header))
    }

    /** Finish the root timer */
    fun finish() {
        root.finish()
    }

    private fun printStep(builder: StringBuilder, step: Step, indent: String) {
        val icon = iconFor(step)
        val label = when (step.type) {
            StepType.ROOT -> "Processor"
            StepType.PHASE -> "Phase"
            StepType.TASK -> "Task"
            StepType.STEP -> "Step"
            StepType.WARNING -> "Warning"
            StepType.ERROR -> "Error"
        }

        builder.appendLine("$indent$icon $label: ${step.name} completed in (${step.formattedDuration()})")
        step.printDiagnostic(builder, indent)

        for (child in step.children) {
            printStep(builder, child, "$indent    ")
        }
    }

    private fun iconFor(step: Step): String = when (step.type) {
        StepType.ROOT -> "⚙️"
        StepType.PHASE, StepType.TASK -> if (step.haveErrors()) "❌" else "✔️"
        StepType.STEP -> "✔️"
        StepType.WARNING -> "⚠️"
        StepType.ERROR -> "❌"
    }

    @Suppress("ktlint:standard:max-line-length")
    private fun appendFilteredSteps(
        step: Step,
        builder: StringBuilder,
        indentLevel: Int,
        filter: (DiagnosticMessage) -> Boolean,
    ) {
        val indent = "  ".repeat(indentLevel)
        val messages = step.messages.filter(filter)
        val hasRelevantChildren = step.children.any { it.containsMessagesRecursively(filter) }

        if (messages.isNotEmpty() || hasRelevantChildren) {
            builder.appendLine("$indent${iconFor(step)} ${step.name} (${step.formattedDuration()})")
            messages.forEach { msg ->
                builder.appendLine("$indent  ${msg.type}: ${msg.message}")
                msg.generateSymbolInfo().takeIf(String::isNotEmpty)?.let { symbolInfo ->
                    builder.appendLine("$indent    -> $symbolInfo")
                }
            }
            step.children.forEach { child -> appendFilteredSteps(child, builder, indentLevel + 1, filter) }
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

        fun isStarted() = startNanos != 0L
        fun isFinish() = endNanos != 0L
        fun isInProgress() = isStarted() && !isFinish()
        fun isCompleted() = isStarted() && isFinish()
        fun haveErrors() = children.any { it.type == StepType.ERROR }

        override fun start() {
            check(started && !isStarted()) { "Step $name already started" }
            startNanos = System.nanoTime()
        }

        override fun finish() {
            check(started && isStarted()) { "Step $name not started" }
            check(!isFinish()) { "Step $name already finish" }
            endNanos = System.nanoTime()
        }

        fun addChild(step: Step) {
            children.add(step)
        }

        override fun createTask(name: String): DiagnosticSender {
            val task = Step(name, StepType.TASK)
            addChild(task)
            return task
        }

        override fun addStep(message: String, symbol: KSNode?) {
            messages.add(DiagnosticMessage(StepType.STEP, message.trim(), symbol))
        }

        override fun addWarning(message: String, symbol: KSNode?) {
            messages.add(DiagnosticMessage(StepType.WARNING, message.trim(), symbol))
        }

        override fun addError(message: String, symbol: KSNode?) {
            messages.add(DiagnosticMessage(StepType.ERROR, message.trim(), symbol))
        }

        override fun die(message: String, symbol: KSNode?): Nothing {
            messages.add(DiagnosticMessage(StepType.ERROR, message.trim(), symbol))
            this@DiagnosticTimer.finish()
            dumpErrorsAndWarnings()
            var suppressException: Throwable? = null
            try {
                finish()
            } catch (e: Exception) {
                suppressException = e
            }
            val exception = IllegalStateException()
            suppressException?.let { exception.addSuppressed(suppressException) }
            throw exception
        }

        fun printDiagnostic(builder: StringBuilder, indent: String) {
            check(isCompleted()) { "Step $name not completed yet" }
            for (msg in messages) {
                val emoji = when (msg.type) {
                    StepType.STEP -> "🟢"
                    StepType.WARNING -> "⚠️"
                    StepType.ERROR -> "❌"
                    else -> "ℹ️"
                }
                val prefix = "    "
                builder.appendLine("$indent$prefix$emoji ${msg.type.name}: ${msg.message}${msg.generateSymbolInfo()}")
            }
        }

        fun formattedDuration(): String {
            if (endNanos == 0L || startNanos == 0L) return "--"
            val ms = (endNanos - startNanos).div(1_000_000.0).roundTo(3)
            return "$ms ms"
        }

        private fun Double.roundTo(numFractionDigits: Int): Double {
            val factor = 10.0.pow(numFractionDigits.toDouble())
            return (this * factor).roundToInt() / factor
        }
    }

    private class DiagnosticMessage(val type: StepType, val message: String, private val symbol: KSNode?) {
        fun generateSymbolInfo(): String {
            if (symbol == null) return ""
            if (symbol.location is FileLocation && symbol is KSDeclaration) {
                val symbolName = symbol.qualifiedName?.asString() ?: "<unknown>"
                val fileLocation = symbol.location as? FileLocation
                val fileName = fileLocation?.filePath?.substringAfterLast('/')
                val line = fileLocation?.lineNumber

                return "at $symbolName($fileName:$line)"
            }
            return ""
        }
    }

    private enum class StepType {
        ROOT,
        PHASE,
        TASK,
        STEP,
        WARNING,
        ERROR,
    }

    interface DiagnosticSender {
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
        fun die(message: String, symbol: KSNode? = null): Nothing

        /** Run all the work in try-catch-finally, this handle start finish and die when throw exceptions */
        fun <R> work(block: (RecuperableDiagnosis) -> R): R {
            val diagnosis = RecuperableDiagnosis(this)
            return try {
                start()
                block(diagnosis)
            } catch (e: Exception) {
                val (message, symbol) = diagnosis.onDieProvider ?: Pair(e.message ?: "", null)
                die(message, symbol)
            } finally {
                finish()
            }
        }

        /** If the condition is false, die */
        fun require(condition: Boolean, message: String, symbol: KSNode? = null) {
            if (!condition) die(message, symbol)
        }

        /** If the value is null, die */
        fun <T> requireNotNull(value: T?, message: String, symbol: KSNode? = null): T {
            if (value == null) die(message, symbol)
            return value
        }

        class RecuperableDiagnosis(sender: DiagnosticSender) : DiagnosticSender by sender {
            var onDieProvider: Pair<String, KSNode?>? = null

            inline fun onDieProvide(crossinline onDie: (() -> Pair<String, KSNode?>?)) {
                onDieProvider = onDie()
            }
        }
    }
}
