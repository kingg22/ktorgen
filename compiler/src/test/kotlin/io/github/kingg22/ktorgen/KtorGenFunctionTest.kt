package io.github.kingg22.ktorgen

import androidx.room.compiler.processing.util.Source
import kotlin.test.Test

class KtorGenFunctionTest {
    @Test
    fun testOptInAnnotationIsPropagatedInFunctionGenerated() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.GET
                import io.github.kingg22.ktorgen.http.Header
                import io.github.kingg22.ktorgen.http.HeaderParam

                import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

                interface TestService {
                    @Header("x", "y")
                    @GET("posts")
                    @OptIn(ExperimentalCompilerApi::class)
                    suspend fun test(@HeaderParam("testHeader") testParameterNonNullable: String?): String
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
    fun testRequiredOptInAnnotationIsPropagatedInFunctionGeneratedWithoutOptInAnnotation() {
        val source = Source.kotlin(
            "Source2.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.GET
                import io.github.kingg22.ktorgen.http.Header
                import io.github.kingg22.ktorgen.http.HeaderParam

                import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

                interface TestService {
                    @Header("x", "y")
                    @GET("posts")
                    @ExperimentalCompilerApi
                    suspend fun test(@HeaderParam("testHeader") testParameterNonNullable: String?): String
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
    fun testRequiredOptInAnnotationIsPropagatedInFunctionGeneratedWithOptInOption() {
        val source = Source.kotlin(
            "Source2.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGenFunction
                import io.github.kingg22.ktorgen.http.GET
                import io.github.kingg22.ktorgen.http.Header

                import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

                interface TestService {
                    @KtorGenFunction(optInAnnotations = [ExperimentalCompilerApi::class])
                    @Header("x", "y")
                    @GET("posts")
                    @ExperimentalCompilerApi
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
