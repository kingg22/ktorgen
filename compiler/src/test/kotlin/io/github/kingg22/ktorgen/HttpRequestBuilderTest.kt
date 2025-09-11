package io.github.kingg22.ktorgen

import androidx.room.compiler.processing.util.Source
import kotlin.test.Test
import kotlin.test.assertNull

class HttpRequestBuilderTest {
    @Test
    fun testAddRequestBuilderArgumentWhenIsDetectedLambda() {
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

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            val generatedFile = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            generatedFile.contains(expectedRequestBuilderArgumentText)
            compilationResultSubject.hasErrorCount(0)
        }
    }

    @Test
    fun testAddRequestBuilderArgumentWhenIsDetectedBuilder() {
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

        val expectedRequestBuilderArgumentText = listOf(
            "this.takeFrom(builder)",
            "this.takeFrom(request)",
            "this.takeFrom(requestData)",
        )

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generatedFile = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            expectedRequestBuilderArgumentText.forEach {
                generatedFile.contains(it)
            }
        }
    }

    @Test
    fun testAddMultipleRequestBuilderArgumentThrowsError() {
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

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(1)
            compilationResultSubject.hasErrorContaining(KtorGenLogger.ONLY_ONE_HTTP_REQUEST_BUILDER)
        }
    }

    @Test
    fun testUnknowRoleOfParameterThrowsError() {
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

        runKtorGenProcessor(source) {
            it.hasErrorContaining(KtorGenLogger.PARAMETER_WITHOUT_ANNOTATION.trim())
            it.hasErrorCount(1)
            it.hasNoWarnings()
            assertNull(it.findGeneratedSource(TEST_SERVICE_IMPL_PATH))
        }
    }
}
