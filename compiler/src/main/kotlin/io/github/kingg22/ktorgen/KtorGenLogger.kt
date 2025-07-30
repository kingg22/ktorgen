package io.github.kingg22.ktorgen

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSNode

class KtorGenLogger(private val kspLogger: KSPLogger, private val loggingType: Int) : KSPLogger by kspLogger {
    override fun error(message: String, symbol: KSNode?) {
        when (loggingType) {
            0 -> {
                // Do nothing
            }

            1 -> {
                kspLogger.error("[KtorGen]: $message", symbol)
            }

            2 -> {
                // Turn errors into compile warnings
                kspLogger.warn("[KtorGen]: $message", symbol)
            }
        }
    }

    companion object {
        const val KTOR_GEN_TYPE_NOT_ALLOWED = "Only interfaces and it companion objects can be annotated with @KtorGen"
    }
}
