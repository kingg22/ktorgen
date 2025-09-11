package io.github.kingg22.ktorgen

class KtorGenOptions(options: Map<String, String>) {
    /**
     * 0: Turn off all error related to checking
     *
     * 1: Check for errors
     *
     * 2: Turn errors into warnings
     */
    val errorsLoggingType = options[CHECKING_TYPE]?.toIntOrNull() ?: 2
    val printStackTraceOnException = options[PRINT_STACKTRACE_ON_EXCEPTION]?.toBooleanStrictOrNull() ?: false

    companion object {
        private const val CHECKING_TYPE = "ktorgen_check_type"
        private const val PRINT_STACKTRACE_ON_EXCEPTION = "ktorgen_print_stacktrace_on_exception"
    }
}
