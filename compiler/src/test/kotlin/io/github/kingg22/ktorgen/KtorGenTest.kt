package io.github.kingg22.ktorgen

import androidx.room.compiler.processing.util.Source
import kotlin.test.Test
import kotlin.test.assertNull

/** Test class only for [@KtorGen][io.github.kingg22.ktorgen.core.KtorGen] */
class KtorGenTest {
    @Test
    fun emptyInterfaceGeneratesNoOpFileWithoutErrors() {
        val api = Source.kotlin(
            "foo.bar.EmptyApi.kt",
            """
                package foo.bar

                import io.github.kingg22.ktorgen.core.KtorGen

                @KtorGen
                interface EmptyApi
            """.trimIndent(),
        )

        runKtorGenProcessor(api) { compilationResultSubject ->
            compilationResultSubject.hasErrorCount(0)
        }
    }

    @Test
    fun testNoGenerateOptionSkipValidationAndGeneration() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGen

                @KtorGen(generate=false)
                interface TestService {
                    suspend fun hello(): String
                }
            """.trimIndent(),
        )

        runKtorGenProcessor(source) {
            it.hasNoWarnings()
            it.hasErrorCount(0)
            assertNull(it.findGeneratedSource(TEST_SERVICE_IMPL_PATH))
        }
    }

    @Test
    fun testOptInAnnotationIsPropagatedInClassGenerated() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.GET
                import io.github.kingg22.ktorgen.http.Header

                import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

                @OptIn(ExperimentalCompilerApi::class)
                interface TestService {
                    @Header("x", "y")
                    @GET("posts")
                    suspend fun test(): String
                }
            """.trimIndent(),
        )

        val expectedHeadersArgumentText = "@OptIn(ExperimentalCompilerApi::class)"

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            val generatedFile = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            generatedFile.contains(expectedHeadersArgumentText)
            compilationResultSubject.hasErrorCount(0)
        }
    }

    @Test
    fun testRequiredOptInAnnotationIsPropagatedInClassGeneratedWithoutOptInAnnotation() {
        val source = Source.kotlin(
            "Source2.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.GET
                import io.github.kingg22.ktorgen.http.Header

                import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

                @ExperimentalCompilerApi
                interface TestService {
                    @Header("x", "y")
                    @GET("posts")
                    suspend fun test(): String
                }
            """.trimIndent(),
        )

        val expectedHeadersArgumentText = "@ExperimentalCompilerApi"
        val noExpectedHeadersArgumentText = "@OptIn(ExperimentalCompilerApi::class)"

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generatedFile = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            generatedFile.contains(expectedHeadersArgumentText)
            generatedFile.doesNotContain(noExpectedHeadersArgumentText)
        }
    }

    @Test
    fun testRequiredOptInAnnotationIsPropagatedInClassGeneratedWithOptInOption() {
        val source = Source.kotlin(
            "Source2.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGen
                import io.github.kingg22.ktorgen.http.GET
                import io.github.kingg22.ktorgen.http.Header

                import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

                @KtorGen(optInAnnotations = [ExperimentalCompilerApi::class])
                @ExperimentalCompilerApi
                interface TestService {
                    @Header("x", "y")
                    @GET("posts")
                    suspend fun test(): String
                }
            """.trimIndent(),
        )

        val expectedHeadersArgumentText = "@OptIn(ExperimentalCompilerApi::class)"
        val noExpectedHeadersArgumentText = "@ExperimentalCompilerApi"

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generatedFile = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            generatedFile.contains(expectedHeadersArgumentText)
            generatedFile.doesNotContain(noExpectedHeadersArgumentText)
        }
    }
}
