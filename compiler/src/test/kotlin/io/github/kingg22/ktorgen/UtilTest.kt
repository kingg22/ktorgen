@file:Suppress("NOTHING_TO_INLINE")

package io.github.kingg22.ktorgen

import androidx.room.compiler.processing.util.CompilationResultSubject
import androidx.room.compiler.processing.util.runKspProcessorTest
import androidx.room.compiler.processing.util.runProcessorTest
import io.github.kingg22.ktorgen.KtorGenOptions.Companion.strickCheckTypeToPair
import io.github.kingg22.ktorgen.KtorGenOptions.ErrorsLoggingType
import java.io.File
import androidx.room.compiler.processing.util.Source as RoomSource
import org.junit.jupiter.api.Assertions as JunitAssertions
import org.junit.jupiter.api.Disabled as JunitDisabled
import org.junit.jupiter.api.Test as JupiterTest

/**
 * Runs the KtorGen processor on the given sources and returns the compilation result using androidx room compiler processing test
 * @param sources the sources to be compiled and expect some result
 * @param onCompilationResult the callback to be called with the compilation result
 */
fun runKtorGenProcessor(
    vararg sources: Source,
    processorOptions: Map<String, String> = emptyMap(),
    kotlincArguments: List<String> = emptyList(),
    ktorgenOptions: KtorGenOptions? = null,
    onCompilationResult: (CompilationResultSubject) -> Unit,
) {
    val processorOptions =
        (ktorgenOptions?.toMap() ?: mapOf(strickCheckTypeToPair(ErrorsLoggingType.Errors))) + processorOptions

    if (sources.all { it is RoomSource.KotlinSource }) {
        // Omit KAPT if all sources are Kotlin sources.
        runKspProcessorTest(
            sources = sources.toList(),
            options = processorOptions,
            symbolProcessorProviders = listOf(KtorGenSymbolProcessorProvider()),
            kotlincArguments = listOf("-Wextra", "-Werror") + kotlincArguments,
            onCompilationResult = onCompilationResult,
        )
    } else {
        // Run with javac, kotlinc, kapt and ksp.
        runProcessorTest(
            sources = sources.toList(),
            kotlincArguments = listOf("-Wextra", "-Werror") + kotlincArguments,
            javacProcessors = emptyList(),
            options = processorOptions,
            symbolProcessorProviders = listOf(KtorGenSymbolProcessorProvider()),
            onCompilationResult = onCompilationResult,
        )
    }
}

/** @return `this"""str"""` */
inline infix fun String.stringTemplate(str: String) = this + stringTemplate(string = str)

/** @return `"""str"""` */
inline fun stringTemplate(string: String) = "\"\"\"$string\"\"\""

inline fun String.toRelativePath() = replace('.', File.separatorChar)
    .removeSuffix("kt")
    .removeSuffix(File.separator)
    .replace("${File.separatorChar}${File.separatorChar}", File.separator) + ".kt"

val TEST_SERVICE_IMPL_PATH = "com.example.api.TestServiceImpl".toRelativePath()

const val SOURCE_FILE_NAME = "Source.kt"

const val CLASS_TEST_SERVICE_IMPL = "public class TestServiceImpl"

const val IMPLEMENT_TEST_SERVICE = ") : TestService"

// abstract of the library.

/** Factory to create an input source code can be [Source.kotlin][RoomSource.kotlin] or [Source.java][RoomSource.java] */
typealias Source = RoomSource

typealias Test = JupiterTest

typealias Ignore = JunitDisabled

inline fun assertNull(actual: Any?, message: String? = null) {
    JunitAssertions.assertNull(actual, message)
}
