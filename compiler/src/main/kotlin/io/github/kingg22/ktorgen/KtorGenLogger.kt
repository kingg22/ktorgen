package io.github.kingg22.ktorgen

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSNode

class KtorGenLogger(private val kspLogger: KSPLogger, private val loggingType: Int) : KSPLogger by kspLogger {
    init {
        instance = this
    }

    override fun error(message: String, symbol: KSNode?) {
        when (loggingType) {
            0 -> {
                // Do nothing
            }

            1 -> {
                kspLogger.error("$KTOR_GEN $message", symbol)
            }

            2 -> {
                // Turn errors into compile warnings
                kspLogger.warn("$KTOR_GEN $message", symbol)
            }
        }
    }

    companion object {
        lateinit var instance: KtorGenLogger
        const val KTOR_GEN = "[KtorGen]:"
        const val KTOR_GEN_TYPE_NOT_ALLOWED =
            "$KTOR_GEN Only interfaces and it companion objects can be annotated with @KtorGen"
        const val NO_HTTP_ANNOTATION_AT = "$KTOR_GEN No Http annotation at "
        const val ONLY_ONE_HTTP_METHOD_IS_ALLOWED = "$KTOR_GEN Only one HTTP method is allowed."
    }
}
