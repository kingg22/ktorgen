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
        const val FUNCTION_OR_PARAMETERS_TYPES_MUST_NOT_INCLUDE_TYPE_VARIABLE_OR_WILDCARD =
            "$KTOR_GEN Function or parameters types must not include a type variable or wildcard:"
        const val FORM_URL_ENCODED_CAN_ONLY_BE_SPECIFIED_ON_HTTP_METHODS_WITH_REQUEST_BODY =
            "$KTOR_GEN FormUrlEncoded can only be specified on HTTP methods with request body (e.g., @POST)."
        const val MULTIPART_CAN_ONLY_BE_SPECIFIED_ON_HTTP_METHODS =
            "$KTOR_GEN Multipart can only be specified on HTTP methods with request body (e.g., @POST)"
        const val GET_METHOD_MUST_NOT_INCLUDE_BODY =
            " method must not include body. See https://datatracker.ietf.org/doc/html/rfc7231#section-4.3.1"
    }
}
