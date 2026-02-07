package io.github.kingg22.ktorgen

data class KtorGenOptions(val errorsLoggingType: ErrorsLoggingType, val isPrintStackTraceOnException: Boolean) {
    constructor(
        options: Map<String, String>,
    ) : this(
        errorsLoggingType = options[STRICK_CHECK_TYPE]?.toIntOrNull().let { ErrorsLoggingType.fromInt(it) },
        isPrintStackTraceOnException = options[PRINT_STACKTRACE_ON_EXCEPTION]?.toBoolean() ?: false,
    )

    enum class ErrorsLoggingType(val intValue: Int) {
        /** Turn off all error related to checking */
        Off(0),

        /** Check for errors */
        Errors(1),

        /** Turn errors into warnings */
        Warnings(2),
        ;

        companion object {
            @JvmStatic
            fun fromInt(value: Int?) = value?.let { entries.firstOrNull { it.intValue == value } } ?: Warnings
        }
    }

    companion object {
        const val STRICK_CHECK_TYPE = "ktorgen_check_type"
        const val PRINT_STACKTRACE_ON_EXCEPTION = "ktorgen_print_stacktrace_on_exception"
    }
}
