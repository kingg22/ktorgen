package io.github.kingg22.ktorgen

class KtorGenAnnotationPropagationTest {
    // -- propagate annotations --
    @Test
    fun testOptInAnnotationIsPropagatedInClassGenerated() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGenAnnotationPropagation
                import io.github.kingg22.ktorgen.core.KtorGenExperimental
                import io.github.kingg22.ktorgen.http.GET
                import io.github.kingg22.ktorgen.http.Header

                import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

                @OptIn(ExperimentalCompilerApi::class, KtorGenExperimental::class)
                @KtorGenAnnotationPropagation
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

                import io.github.kingg22.ktorgen.core.KtorGenAnnotationPropagation
                import io.github.kingg22.ktorgen.core.KtorGenExperimental
                import io.github.kingg22.ktorgen.http.GET
                import io.github.kingg22.ktorgen.http.Header

                import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

                @OptIn(KtorGenExperimental::class)
                @ExperimentalCompilerApi
                @KtorGenAnnotationPropagation
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

    // -- annotations --
    @Test
    fun optionAnnotationsGenerateCodeHaveThose() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGenAnnotationPropagation
                import io.github.kingg22.ktorgen.core.KtorGenExperimental
                import io.github.kingg22.ktorgen.core.KtorGenTopLevelFactory

                @Target(AnnotationTarget.CLASS)
                annotation class TestAnnotation

                // an empty target means all targets, an annotation option respects it
                annotation class MyAnnotation

                @OptIn(KtorGenExperimental::class)
                @KtorGenAnnotationPropagation(annotations = [TestAnnotation::class, MyAnnotation::class])
                @KtorGenTopLevelFactory
                interface TestService
            """.trimIndent(),
        )

        val expectedText = listOf(
            "@TestAnnotation\n@MyAnnotation\n@Generated\npublic class TestServiceImpl",
            ": TestService",
            "\n@Generated\n@MyAnnotation\npublic fun TestService(",
        )

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generatedFile = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            expectedText.forEach { line ->
                generatedFile.contains(line)
            }
        }
    }

    // -- optIn annotation --
    @Test
    fun testRequiredOptInAnnotationIsPropagatedInClassGeneratedWithOptInOption() {
        val source = Source.kotlin(
            "Source2.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGenAnnotationPropagation
                import io.github.kingg22.ktorgen.core.KtorGenExperimental
                import io.github.kingg22.ktorgen.http.GET
                import io.github.kingg22.ktorgen.http.Header

                import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

                @OptIn(KtorGenExperimental::class)
                @KtorGenAnnotationPropagation(optInAnnotations = [ExperimentalCompilerApi::class])
                @ExperimentalCompilerApi
                interface TestService {
                    @Header("x", "y")
                    @GET("posts")
                    suspend fun test(): String
                }
            """.trimIndent(),
        )

        val expectedOptIn = "@OptIn(ExperimentalCompilerApi::class)"
        val directAnnotation = "@ExperimentalCompilerApi"

        runKtorGenProcessor(source) { result ->
            /* // Kotlin 1.9 no permite pasar una anotación de tipo requiresOptIn como parametro de otra anotación
            result.hasErrorCount(1)
            result.hasErrorContaining(
                "This class can only be used as an annotation or as an argument to @OptIn",
            )
             */

            // Kotlin 2.x + KSP2 debería compilar bien
            result.hasNoWarnings()
            result.hasErrorCount(0)
            val generatedFile = result.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            generatedFile.contains(expectedOptIn)
            generatedFile.doesNotContain(directAnnotation)
        }
    }

    // -- function annotations --
    @Test
    fun optionFunctionAnnotationsIsGenerated() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGenAnnotationPropagation
                import io.github.kingg22.ktorgen.core.KtorGenExperimental
                import io.github.kingg22.ktorgen.core.KtorGenTopLevelFactory

                @Target(AnnotationTarget.FUNCTION)
                annotation class TestAnnotation

                // an empty target means all targets, an annotation option respects it
                annotation class MyAnnotation

                @OptIn(KtorGenExperimental::class)
                @KtorGenAnnotationPropagation(
                    factoryFunctionAnnotations = [
                        TestAnnotation::class, MyAnnotation::class, kotlin.jvm.JvmSynthetic::class,
                    ]
                )
                @KtorGenTopLevelFactory
                interface TestService
            """.trimIndent(),
        )

        val expectedText = listOf(
            "\n@Generated\npublic class TestServiceImpl",
            ": TestService",
            "\n@Generated\n@TestAnnotation\n@MyAnnotation\n@JvmSynthetic\npublic fun TestService(",
        )

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generatedFile = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            expectedText.forEach { line ->
                generatedFile.contains(line)
            }
        }
    }
}
