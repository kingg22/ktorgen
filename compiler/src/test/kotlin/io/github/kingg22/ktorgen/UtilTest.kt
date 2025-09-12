@file:JvmName("UtilTest")
@file:JvmMultifileClass
@file:Suppress("NOTHING_TO_INLINE")

package io.github.kingg22.ktorgen

import androidx.room.compiler.processing.util.CompilationResultSubject
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.runKspProcessorTest
import java.io.File

val KTORGEN_STRICT_CHECK = mapOf("ktorgen_check_type" to "1", "ktorgen_print_stacktrace_on_exception" to "true")

/**
 * Runs the KtorGen processor on the given sources and returns the compilation result using androidx room compiler processing test
 * @param sources the sources to be compiled and expect some result
 * @param onCompilationResult the callback to be called with the compilation result
 */
inline fun runKtorGenProcessor(
    vararg sources: Source,
    processorOptions: Map<String, String> = KTORGEN_STRICT_CHECK,
    kotlincArguments: List<String> = emptyList(),
    noinline onCompilationResult: (CompilationResultSubject) -> Unit,
) = runKspProcessorTest(
    sources = sources.toList(),
    options = processorOptions,
    symbolProcessorProviders = listOf(KtorGenSymbolProcessorProvider()),
    kotlincArguments = listOf("-Wextra", "-Werror") + kotlincArguments,
    onCompilationResult = onCompilationResult,
)

/** @return `this"""str"""` */
inline infix fun String.stringTemplate(str: String) = "$this\"\"\"$str\"\"\""

inline fun String.toRelativePath() = replace('.', File.separatorChar)
    .removeSuffix("kt")
    .removeSuffix(File.separator)
    .replace("${File.separatorChar}${File.separatorChar}", File.separator) + ".kt"

val TEST_SERVICE_IMPL_PATH = "com.example.api._TestServiceImpl".toRelativePath()
