package io.github.kingg22.ktorgen.logging

import com.google.devtools.ksp.symbol.FileLocation
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSValueParameter
import io.github.kingg22.ktorgen.DiagnosticSender
import io.github.kingg22.ktorgen.KtorGenFatalError
import io.github.kingg22.ktorgen.checkImplementation
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * DiagnosticTimer provides structured logging and timing for KSP processor execution.
 * Messages are logged chronologically with clear hierarchical context.
 */
@ConsistentCopyVisibility
internal data class ChronoDiagnostic private constructor(private val root: Step) :
    DiagnosticSender by root,
    DiagnosticSender.DiagnosticHolder {
    private val allMessages inline get() = root.allMessages

    constructor(name: String) : this(Step(name, 0, null, mutableListOf()))

    @Deprecated("Use createTask instead", replaceWith = ReplaceWith("createTask(name)"))
    override fun createPhase(name: String): DiagnosticSender = root.createTask(name)

    /** Generate all the report in mode verbose, this not mark as finish the root timer */
    override fun buildReport(): String = buildString {
        appendLine("┌─ ${root.name} [${root.formattedDuration()}] ${if (root.hasErrors()) "❌" else "✓"}")
        allMessages.forEach { entry ->
            val indent = "┣─" + "──".repeat(entry.depth)
            val path = buildBreadcrumb(entry.step)
            appendLine("$indent${entry.severity.icon} $path")
            appendLine("$indent   ${entry.message}")
            entry.symbolInfo?.let { appendLine("$indent  → $it") }
        }
        appendLine("└─ Summary: ${allMessages.size} msgs (${errorCount()} errors, ${warningCount()} warnings)")
    }

    /** Generate report of all errors, this not mark as finish the root timer */
    override fun buildErrorsMessage(): String = buildFilteredReport("❌ ERRORS", ERROR)

    /** Generate report of all warnings, this not mark as finish the root timer */
    override fun buildWarningsMessage(): String = buildFilteredReport("⚠️  WARNINGS", WARNING)

    /** Generate report of errors and warning, this not mark as finish the root timer */
    override fun buildErrorsAndWarningsMessage(): String = buildString {
        val issues = allMessages.filter { it.severity == ERROR || it.severity == WARNING }
        val errors = issues.count { it.severity == ERROR }
        val warnings = issues.count { it.severity == WARNING }

        appendLine("┌─ ISSUES: $errors errors, $warnings warnings")
        if (issues.isEmpty()) {
            appendLine("│  No issues found")
        } else {
            issues.forEach { entry ->
                val path = buildCompactPath(entry.step)
                appendLine("│ ${entry.severity.icon} $path")
                appendLine("│    ${entry.message}")
                entry.symbolInfo?.let { appendLine("│    → $it") }
            }
        }
        appendLine("└─")
    }

    private fun buildFilteredReport(title: String, severity: Severity): String = buildString {
        val filtered = allMessages.filter { it.severity == severity }

        appendLine("┌─ $title (${filtered.size})")
        if (filtered.isEmpty()) {
            appendLine("│  None found")
        } else {
            filtered.forEach { entry ->
                val path = buildCompactPath(entry.step)
                appendLine("│ ${severity.icon} $path")
                appendLine("│    ${entry.message}")
                entry.symbolInfo?.let { appendLine("│    → $it") }
            }
        }
        appendLine("└─")
    }

    private fun buildBreadcrumb(step: Step): String {
        val parts = mutableListOf<String>()
        var current: Step? = step

        while (current != null) {
            parts.add(0, current.name)
            current = current.parent
        }

        return parts.joinToString(" → ")
    }

    private fun buildCompactPath(step: Step): String {
        val parts = mutableListOf<String>()
        var current: Step? = step

        while (current != null && current.parent != null) {
            parts.add(0, current.name)
            current = current.parent
        }

        return parts.joinToString(" → ")
    }

    private fun errorCount() = allMessages.count { it.severity == ERROR }
    private fun warningCount() = allMessages.count { it.severity == WARNING }

    private data class LogEntry(
        val step: Step,
        val depth: Int,
        val severity: Severity,
        val message: String,
        val symbolInfo: String?,
    )

    private enum class Severity(val icon: String) {
        INFO("ℹ️"),
        WARNING("⚠️"),
        ERROR("❌"),
    }

    private class Step(val name: String, val depth: Int, val parent: Step?, val allMessages: MutableList<LogEntry>) :
        DiagnosticSender {
        private var startNanos: Long = 0
        private var endNanos: Long = 0
        private val children = mutableListOf<Step>()

        init {
            if (parent != null) {
                checkImplementation(parent.isInProgress) {
                    "Cannot create child step '$name' - parent '${parent.name}' is not in progress"
                }
                parent.children.add(this)
            }
        }

        override val isStarted: Boolean get() = startNanos != 0L
        override val isFinish: Boolean get() = endNanos != 0L

        override fun hasErrors(): Boolean = allMessages.any { it.step == this && it.severity == ERROR } ||
            children.any { it.hasErrors() }

        override fun hasWarnings(): Boolean = allMessages.any { it.step == this && it.severity == WARNING } ||
            children.any { it.hasWarnings() }

        override fun start() {
            checkImplementation(!isStarted) { "Step '$name' already started" }
            parent?.let {
                checkImplementation(it.isInProgress) {
                    "Cannot start step '$name' - parent '${it.name}' is not in progress"
                }
            }
            startNanos = System.nanoTime()
        }

        override fun finish() {
            checkImplementation(isStarted) { "Step '$name' not started" }
            checkImplementation(!isFinish) { "Step '$name' already finished" }
            parent?.let {
                checkImplementation(it.isInProgress) {
                    "Cannot finish step '$name' - parent '${it.name}' is not in progress"
                }
            }
            endNanos = System.nanoTime()
        }

        override fun createTask(name: String): DiagnosticSender = Step(name, depth + 1, this, allMessages)

        override fun addStep(message: String, symbol: KSNode?) {
            allMessages.add(
                LogEntry(
                    step = this,
                    depth = depth,
                    severity = INFO,
                    message = message.trim(),
                    symbolInfo = extractSymbolInfo(symbol),
                ),
            )
        }

        override fun addWarning(message: String, symbol: KSNode?) {
            allMessages.add(
                LogEntry(
                    step = this,
                    depth = depth,
                    severity = WARNING,
                    message = message.trim(),
                    symbolInfo = extractSymbolInfo(symbol),
                ),
            )
        }

        override fun addError(message: String, symbol: KSNode?) {
            allMessages.add(
                LogEntry(
                    step = this,
                    depth = depth,
                    severity = ERROR,
                    message = message.trim(),
                    symbolInfo = extractSymbolInfo(symbol),
                ),
            )
        }

        override fun die(message: String, symbol: KSNode?, cause: Exception?): Nothing {
            var suppressedException: Throwable? = null

            try {
                addError(message, symbol)

                // Finish all in-progress steps
                collectAllSteps()
                    .filter { it.isInProgress }
                    .forEach { it.finish() }
            } catch (e: KtorGenFatalError) {
                throw e
            } catch (e: Throwable) {
                suppressedException = e
            }

            val exception = KtorGenFatalError(message, cause)
            suppressedException?.let { exception.addSuppressed(it) }
            throw exception
        }

        fun formattedDuration(): String {
            if (endNanos == 0L || startNanos == 0L) return "--"
            val ms = (endNanos - startNanos).div(1_000_000.0).roundTo(2)
            return "${ms}ms"
        }

        private fun collectAllSteps(): List<Step> {
            val steps = mutableListOf<Step>()
            var current: Step? = this

            // Go to root
            while (current?.parent != null) {
                current = current.parent
            }

            // Collect all from root
            fun collect(step: Step) {
                steps.add(step)
                step.children.forEach { collect(it) }
            }

            current?.let { collect(it) }
            return steps
        }

        private fun extractSymbolInfo(symbol: KSNode?): String? {
            if (symbol == null) return null

            val location = symbol.location as? FileLocation ?: return null
            val fileName = location.filePath.substringAfterLast('/')
            val line = location.lineNumber

            return when (symbol) {
                is KSDeclaration -> {
                    val name = symbol.qualifiedName?.asString() ?: "<unknown>"
                    "$name ($fileName:$line)"
                }

                is KSValueParameter -> {
                    val type = symbol.type.resolve()
                    val varargPrefix = if (symbol.isVararg) "vararg " else ""
                    val paramName = symbol.name?.asString() ?: "<unknown>"
                    val typeName = type.declaration.simpleName.asString()
                    val nullable = if (type.isMarkedNullable) "?" else ""
                    val function = (symbol.parent as? KSDeclaration)?.qualifiedName?.asString() ?: "<unknown>"
                    "$varargPrefix$paramName: $typeName$nullable in $function ($fileName:$line)"
                }

                else -> "$fileName:$line"
            }
        }

        private fun Double.roundTo(decimals: Int): Double {
            val factor = 10.0.pow(decimals.toDouble())
            return (this * factor).roundToInt() / factor
        }
    }
}
