package io.github.kingg22.ktorgen

import androidx.room.compiler.processing.util.Source
import io.github.kingg22.ktorgen.model.KTORG_GENERATED_COMMENT
import kotlin.test.Test

/** Test related to [@KtorGenFunction][io.github.kingg22.ktorgen.core.KtorGenFunction] annotation */
class KtorGenFunctionTest {
    // -- propagate annotation --
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

    // -- optIn annotations --
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

    // -- generate? --
    @Test
    fun testGenerateFalseDontGenerateFunction() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGenFunction
                import io.github.kingg22.ktorgen.http.GET
                import io.github.kingg22.ktorgen.http.Header

                interface TestService {
                    @KtorGenFunction(generate = false)
                    @Header("x", "y")
                    @GET("posts")
                    suspend fun test(): String = "test"
                }
            """.trimIndent(),
        )

        val expectedText = listOf("public class _TestServiceImpl", ": TestService")
        val nonExpectedText = listOf("override suspend fun test(): String")

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)

            val generatedFile = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)

            for (expectedLine in expectedText) {
                generatedFile.contains(expectedLine)
            }
            for (nonExpectedLine in nonExpectedText) {
                generatedFile.doesNotContain(nonExpectedLine)
            }
        }
    }

    /**
     * When a function is declared on an interface, for implemented classes is mandatory to override it,
     * the processor should throw an error if the generated file does not override it == no compile
     */
    @Test
    fun testGenerateFalseAndMandatoryFunctionThrowsError() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGenFunction
                import io.github.kingg22.ktorgen.http.GET

                interface TestService {
                    @KtorGenFunction(generate = false)
                    @GET("posts")
                    suspend fun test(): String

                    @GET("posts")
                    suspend fun test2(): String
                }
            """.trimIndent(),
        )

        runKtorGenProcessor(source) {
            it.hasNoWarnings()
            it.hasErrorCount(1)
            it.hasErrorContaining(KtorGenLogger.ABSTRACT_FUNCTION_IGNORED.trim())
        }
    }

    // -- annotations --
    @Test
    fun testPassAnnotationsGeneratedFunctionHaveThose() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGenFunction
                import io.github.kingg22.ktorgen.http.GET

                interface TestService {
                    @KtorGenFunction(annotations = [org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi::class])
                    @GET("posts")
                    suspend fun test(): String
                }
            """.trimIndent(),
        )

        val expectedAnnotationsText = "@ExperimentalCompilerApi"
        val noExpectedAnnotationsText = "@OptIn(ExperimentalCompilerApi::class)"

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generatedFile = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            generatedFile.contains(expectedAnnotationsText)
            generatedFile.doesNotContain(noExpectedAnnotationsText)
        }
    }

    @Test
    fun testPassAnnotationsAndPropagateAnnotationGeneratedFunctionHaveThose() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGenFunction
                import io.github.kingg22.ktorgen.http.GET

                annotation class MyAnnotation

                interface TestService {
                    @KtorGenFunction(annotations = [org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi::class])
                    @GET("posts")
                    @MyAnnotation
                    suspend fun test(): String
                }
            """.trimIndent(),
        )

        val expectedAnnotationsText = listOf("@ExperimentalCompilerApi", "@MyAnnotation")
        val noExpectedAnnotationsText = "@OptIn(ExperimentalCompilerApi::class)"

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generatedFile = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            for (expectedLine in expectedAnnotationsText) {
                generatedFile.contains(expectedLine)
            }
            generatedFile.doesNotContain(noExpectedAnnotationsText)
        }
    }

    // -- custom header kdoc --
    @Test
    fun testCustomHeaderOptionIsGenerated() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGenFunction
                import io.github.kingg22.ktorgen.http.GET

                interface TestService {
                    @KtorGenFunction(customHeader = "Hello, World!")
                    @GET("posts")
                    suspend fun test(): String
                }
            """.trimIndent(),
        )

        val expectedCustomHeaderText = "* Hello, World!"
        val noExpectedCustomHeaderText = KTORG_GENERATED_COMMENT

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generatedFile = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            generatedFile.contains(expectedCustomHeaderText)
            generatedFile.doesNotContain(noExpectedCustomHeaderText)
        }
    }

    @Test
    fun testDefaultCustomHeaderIsGenerated() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGenFunction
                import io.github.kingg22.ktorgen.http.GET

                interface TestService {
                    @GET("posts")
                    suspend fun test(): String
                }
            """.trimIndent(),
        )

        val expectedCustomHeaderText = KTORG_GENERATED_COMMENT

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generatedFile = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            generatedFile.contains(expectedCustomHeaderText)
        }
    }
}
