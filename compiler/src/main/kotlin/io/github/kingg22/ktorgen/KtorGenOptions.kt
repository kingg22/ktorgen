package io.github.kingg22.ktorgen

class KtorGenOptions(options: Map<String, String>) {
    /**
     * 0: Turn off all error related to checking
     *
     * 1: Check for errors
     *
     * 2: Turn errors into warnings
     */
    val errorsLoggingType: Int = options[CHECKING_TYPE]?.toIntOrNull() ?: 1

    companion object {
        const val CHECKING_TYPE = "ktorgen_check_type"
        const val QUALIFIED_TYPE = "ktorgen_qualified_type_name"
    }
}
