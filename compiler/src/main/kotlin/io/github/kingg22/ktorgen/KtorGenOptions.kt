package io.github.kingg22.ktorgen

private const val STRICK_CHECK_TYPE = "ktorgen.strict"
private const val PRINT_STACKTRACE_ON_EXCEPTION = "ktorgen.print_stacktrace"
private const val EXPERIMENTAL = "ktorgen.experimental"

data class KtorGenOptions(
    val errorsLoggingType: ErrorsLoggingType = ErrorsLoggingType.Warnings,
    val isPrintStackTraceOnException: Boolean = false,
    val experimental: Boolean = false,
) {
    constructor(
        options: Map<String, String>,
    ) : this(
        errorsLoggingType = options[STRICK_CHECK_TYPE]?.toIntOrNull().let { ErrorsLoggingType.fromInt(it) },
        isPrintStackTraceOnException = options[PRINT_STACKTRACE_ON_EXCEPTION]?.toBoolean() ?: false,
        experimental = options[EXPERIMENTAL]?.toBoolean() ?: false,
    )

    fun toMap() = mapOf(
        STRICK_CHECK_TYPE to errorsLoggingType.intValue.toString(),
        PRINT_STACKTRACE_ON_EXCEPTION to isPrintStackTraceOnException.toString(),
        EXPERIMENTAL to experimental.toString(),
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
        @JvmStatic
        fun strickCheckTypeToPair(value: ErrorsLoggingType) = STRICK_CHECK_TYPE to value.intValue.toString()
    }
}
