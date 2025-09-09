@file:JvmName("UtilTest")
@file:JvmMultifileClass

package io.github.kingg22.ktorgen

import androidx.room.compiler.processing.util.CompilationResultSubject
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.runKspProcessorTest

val KTORGEN_STRICT_CHECK = mapOf("ktorgen_check_type" to "1")

/**
 * Runs the KtorGen processor on the given sources and returns the compilation result using androidx room compiler processing test
 * @param sources the sources to be compiled and expect some result
 * @param onCompilationResult the callback to be called with the compilation result
 */
fun runKtorGenProcessor(
    vararg sources: Source,
    processorOptions: Map<String, String> = emptyMap(),
    kotlincArguments: List<String> = emptyList(),
    onCompilationResult: (CompilationResultSubject) -> Unit,
) = runKspProcessorTest(
    sources = sources.toList(),
    options = KTORGEN_STRICT_CHECK + processorOptions,
    symbolProcessorProviders = listOf(KtorGenSymbolProcessorProvider()),
    kotlincArguments = listOf("-Wextra", "-Werror") + kotlincArguments,
    onCompilationResult = onCompilationResult,
)

/** @return `this"""str"""` */
infix fun String.stringTemplate(str: String) = "$this\"\"\"$str\"\"\""
