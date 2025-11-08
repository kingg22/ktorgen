package io.github.kingg22.ktorgen

/** Fatal error during a [DiagnosticSender.work] */
open class KtorGenFatalError(message: String? = null, cause: Throwable? = null) :
    RuntimeException("Fatal error occurred. ${message.orEmpty()}".trim(), cause) {

    /** Ups, an implementation error. **/
    @KtorGenWithoutCoverage // The idea is not having implementation errors, right?
    class KtorGenImplementationError(message: String? = null, cause: Throwable? = null) :
        KtorGenFatalError(
            "Implementation error: $message\n" +
                "Please report this issue on [GitHub issue tracker](https://github.com/kingg22/ktorgen/issues/new)",
            cause,
        )
}
