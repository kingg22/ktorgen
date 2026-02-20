package io.github.kingg22.ktorgen

// Missing KMP factory because current test framework only works with JVM target
class FactoriesTest {
    // -- top level factory function --
    @Test
    fun topLevelFactoryFunctionIsGeneratedWithAnnotation() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGen
                import io.github.kingg22.ktorgen.core.KtorGenTopLevelFactory
                import io.github.kingg22.ktorgen.http.GET

                @KtorGen(basePath = "/api/")
                @KtorGenTopLevelFactory
                interface TestService {
                    @GET
                    suspend fun test(): String
                }
            """.trimIndent(),
        )

        val expectedText = "public fun TestService(httpClient: HttpClient): TestService = TestServiceImpl(httpClient)"

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generated = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            generated.contains(expectedText)
        }
    }

    @Test
    fun noTopLevelFactoryAnnotationDontGeneratedThat() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGen
                import io.github.kingg22.ktorgen.http.GET

                interface TestService {
                    @GET
                    suspend fun test(): String
                }
            """.trimIndent(),
        )

        val nonExpectedText = "public fun TestService(httpClient: HttpClient)"
        val expectedText = "public class TestServiceImpl"

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generated = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            generated.contains(expectedText)
            generated.doesNotContain(nonExpectedText)
        }
    }

    // -- companion extension function --
    @Test
    fun companionExtensionFunctionIsGenerated() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGenTopLevelFactory
                import io.github.kingg22.ktorgen.core.KtorGenCompanionExtFactory
                import io.github.kingg22.ktorgen.http.GET

                interface TestService {
                    @GET
                    suspend fun test(): String

                    @KtorGenTopLevelFactory
                    @KtorGenCompanionExtFactory
                    companion object
                }
            """.trimIndent(),
        )

        val expectedText = listOf(
            "public fun TestService(httpClient: HttpClient): TestService = TestServiceImpl(httpClient)",
            "public fun TestService.Companion.create(httpClient: HttpClient): TestService = TestServiceImpl(httpClient)",
            "public class TestServiceImpl",
        )

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generated = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            expectedText.forEach { line ->
                generated.contains(line)
            }
        }
    }

    /**
     * A limitation of KSP is: can't alterate the source code,
     * only generate new files, need to explicitly check exist a companion object before generate invalid code
     */
    @Test
    fun companionExtFactoryAndNotHaveCompanionObjectThrowsError() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGenTopLevelFactory
                import io.github.kingg22.ktorgen.core.KtorGenCompanionExtFactory
                import io.github.kingg22.ktorgen.http.GET

                @KtorGenTopLevelFactory
                @KtorGenCompanionExtFactory
                interface TestService {
                    @GET
                    suspend fun test(): String
                }
            """.trimIndent(),
        )

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(1)
            compilationResultSubject.hasErrorContaining(KtorGenLogger.MISSING_COMPANION_TO_GENERATE.trim())
        }
    }

    @Test
    fun noCompanionExtFactoryAnnotationDontGeneratedThat() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGen
                import io.github.kingg22.ktorgen.http.GET

                interface TestService {
                    @GET
                    suspend fun test(): String
                }
            """.trimIndent(),
        )

        val nonExpectedText = "public fun TestService.Companion.create(httpClient: HttpClient)"
        val expectedText = "public class TestServiceImpl"

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generated = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            generated.contains(expectedText)
            generated.doesNotContain(nonExpectedText)
        }
    }

    // -- http client extension function --
    @Test
    fun httpClientExtFactoryAnnotationGenerateThat() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGenTopLevelFactory
                import io.github.kingg22.ktorgen.core.KtorGenCompanionExtFactory
                import io.github.kingg22.ktorgen.core.KtorGenHttpClientExtFactory
                import io.github.kingg22.ktorgen.http.GET

                @KtorGenTopLevelFactory
                @KtorGenCompanionExtFactory
                @KtorGenHttpClientExtFactory
                interface TestService {
                    @GET
                    suspend fun test(): String

                    companion object
                }
            """.trimIndent(),
        )

        val expectedText = listOf(
            "public fun TestService(httpClient: HttpClient): TestService = TestServiceImpl(httpClient)",
            "public fun TestService.Companion.create(httpClient: HttpClient): TestService = TestServiceImpl(httpClient)",
            "public fun HttpClient.createTestService(): TestService = TestServiceImpl(this)",
            "public class TestServiceImpl",
        )

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generated = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            expectedText.forEach { line ->
                generated.contains(line)
            }
        }
    }

    @Test
    fun noHttpClientExtFactoryAnnotationDontGeneratedThat() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGen
                import io.github.kingg22.ktorgen.http.GET

                interface TestService {
                    @GET
                    suspend fun test(): String
                }
            """.trimIndent(),
        )

        val nonExpectedText = "public fun HttpClient.create"
        val expectedText = "public class TestServiceImpl"

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generated = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            generated.contains(expectedText)
            generated.doesNotContain(nonExpectedText)
        }
    }
}
