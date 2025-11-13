@file:Suppress("NOTHING_TO_INLINE")

package io.github.kingg22.ktorgen

import androidx.room.compiler.processing.util.CompilationResultSubject
import androidx.room.compiler.processing.util.KOTLINC_LANGUAGE_1_9_ARGS
import androidx.room.compiler.processing.util.runKspProcessorTest
import io.github.kingg22.ktorgen.KSPVersion.KSP1
import io.github.kingg22.ktorgen.KSPVersion.KSP2
import java.io.File
import androidx.room.compiler.processing.util.Source as RoomSource
import org.junit.jupiter.api.Assertions as JunitAssertions

/**
 * Runs the KtorGen processor on the given sources and returns the compilation result using androidx room compiler processing test
 * @param sources the sources to be compiled and expect some result
 * @param onCompilationResult the callback to be called with the compilation result
 */
fun runKtorGenProcessor(
    vararg sources: Source,
    kspVersion: KSPVersion,
    processorOptions: Map<String, String> = mapOf(
        KtorGenOptions.STRICK_CHECK_TYPE to KtorGenOptions.ErrorsLoggingType.Errors.intValue.toString(),
    ),
    kotlincArguments: List<String> = emptyList(),
    onCompilationResult: (CompilationResultSubject) -> Unit,
) {
    when (kspVersion) {
        KSP2 -> runKspProcessorTest(
            sources = sources.toList(),
            options = processorOptions,
            symbolProcessorProviders = listOf(KtorGenSymbolProcessorProvider()),
            kotlincArguments = listOf("-Wextra", "-Werror") + kotlincArguments,
            onCompilationResult = onCompilationResult,
        )

        KSP1 -> runKspProcessorTest(
            sources = sources.toList(),
            symbolProcessorProviders = listOf(KtorGenSymbolProcessorProvider()),
            kotlincArguments = KOTLINC_LANGUAGE_1_9_ARGS + kotlincArguments,
            options = mapOf("ksp.useKsp2" to "false") + processorOptions,
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

val TEST_SERVICE_IMPL_PATH = "com.example.api._TestServiceImpl".toRelativePath()

const val SOURCE_FILE_NAME = "Source.kt"

const val CLASS_TEST_SERVICE_IMPL = "public class _TestServiceImpl"

const val IMPLEMENT_TEST_SERVICE = ") : TestService"

/**
 * Ksp version supported by KtorGen processor and going to test.
 *
 * This is for unit tests.
 * In integration test can probe with real dependency and Ktor client versions
 */
enum class KSPVersion { KSP1, KSP2 }

// abstract of the library.
// only have junit parametrized in other files!

/** Factory to create an input source code can be [Source.kotlin][RoomSource.kotlin] or [Source.java][RoomSource.java] */
typealias Source = RoomSource

typealias Test = org.junit.jupiter.api.Test

inline fun assertNull(actual: Any?, message: String? = null) = JunitAssertions.assertNull(actual, message)
