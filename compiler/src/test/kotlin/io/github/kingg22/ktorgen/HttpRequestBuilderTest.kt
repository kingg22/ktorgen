package io.github.kingg22.ktorgen

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class HttpRequestBuilderTest {
    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun testAddRequestBuilderArgumentWhenIsDetectedLambda(kspVersion: KSPVersion) {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.GET
                import io.ktor.client.request.HttpRequestBuilder

                interface TestService {
                    @GET("posts")
                    suspend fun test(builder: HttpRequestBuilder.() -> Unit)
                }
            """.trimIndent(),
        )

        val expectedRequestBuilderArgumentText = "builder(this)"

        runKtorGenProcessor(source, kspVersion = kspVersion) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            val generatedFile = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            generatedFile.contains(expectedRequestBuilderArgumentText)
            compilationResultSubject.hasErrorCount(0)
        }
    }

    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun testAddRequestBuilderArgumentWhenIsDetectedBuilder(kspVersion: KSPVersion) {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.GET
                import io.ktor.client.request.HttpRequestBuilder
                import io.ktor.client.request.HttpRequest
                import io.ktor.client.request.HttpRequestData

                interface TestService {
                    @GET("posts")
                    suspend fun test(builder: HttpRequestBuilder)
                    @GET
                    suspend fun test2(request: HttpRequest)
                    @GET
                    suspend fun test3(requestData: HttpRequestData)
                }
            """.trimIndent(),
        )
        // Kotlinpoet can import extension functions with alias
        val expectedRequestBuilderArgumentText = listOf("builder", "request", "requestData")

        runKtorGenProcessor(source, kspVersion = kspVersion) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generatedFile = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            expectedRequestBuilderArgumentText.forEach { arg ->
                generatedFile.containsMatch("""(?:this\.)?\w*akeFrom\($arg\)""".toPattern())
            }
        }
    }

    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun testAddMultipleRequestBuilderArgumentThrowsError(kspVersion: KSPVersion) {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.GET
                import io.ktor.client.request.HttpRequestBuilder
                import io.ktor.client.request.HttpRequest
                import io.ktor.client.request.HttpRequestData

                interface TestService {
                    @GET("posts")
                    suspend fun test(builder: HttpRequestBuilder, request: HttpRequest, requestData: HttpRequestData)
                }
            """.trimIndent(),
        )

        runKtorGenProcessor(source, kspVersion = kspVersion) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(1)
            compilationResultSubject.hasErrorContaining(KtorGenLogger.ONLY_ONE_HTTP_REQUEST_BUILDER)
        }
    }

    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun testUnknowRoleOfParameterThrowsError(kspVersion: KSPVersion) {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.GET

                interface TestService {
                    @GET("posts")
                    suspend fun test(builder : String)
                }
            """.trimIndent(),
        )

        runKtorGenProcessor(source, kspVersion = kspVersion) { result ->
            result.hasErrorContaining(KtorGenLogger.PARAMETER_WITHOUT_ANNOTATION.trim())
            result.hasErrorCount(1)
            result.hasNoWarnings()
            assertNull(result.findGeneratedSource(TEST_SERVICE_IMPL_PATH))
        }
    }
}
