package io.github.kingg22.ktorgen

import kotlin.math.pow
import kotlin.math.roundToInt

// Copy of Paris Processor timer https://github.com/airbnb/paris/blob/d8b5edbc56253bcdd0d0c57930d2e91113dd0f37/paris-processor/src/main/java/com/airbnb/paris/processor/Timer.kt
class Timer(val name: String) {
    private val timingSteps = mutableListOf<TimingStep>()
    private var startNanos: Long? = null
    private var lastTimingNanos: Long? = null

    @Synchronized
    fun start(): Timer {
        timingSteps.clear()
        startNanos = System.nanoTime()
        lastTimingNanos = startNanos
        return this
    }

    @Synchronized
    fun markStepCompleted(stepDescription: String) {
        val nowNanos = System.nanoTime()
        val lastNanos = lastTimingNanos ?: error("Timer was not started")
        lastTimingNanos = nowNanos

        timingSteps.add(TimingStep(nowNanos - lastNanos, stepDescription))
    }

    @Synchronized
    fun finishAndPrint(logger: KtorGenLogger = KtorGenLogger.instance) {
        val start = startNanos ?: error("Timer was not started")
        val message = buildString {
            appendLine("$name finished in ${formatNanos(System.nanoTime() - start)}")
            timingSteps.forEach { step ->
                appendLine(" - ${step.description} (${formatNanos(step.durationNanos)})")
            }
        }

        logger.info(message, null)
    }

    private class TimingStep(val durationNanos: Long, val description: String)

    private fun formatNanos(nanos: Long): String {
        val diffMs = nanos.div(1_000_000.0).roundTo(3)
        return "$diffMs ms"
    }

    private fun Double.roundTo(numFractionDigits: Int): Double {
        val factor = 10.0.pow(numFractionDigits.toDouble())
        return (this * factor).roundToInt() / factor
    }
}
