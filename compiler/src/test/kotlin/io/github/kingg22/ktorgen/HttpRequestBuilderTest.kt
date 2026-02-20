package io.github.kingg22.ktorgen

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
        // Kotlinpoet can import extension functions with alias
        val expectedRequestBuilderArgumentText = listOf("builder", "request", "requestData")

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generatedFile = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            expectedRequestBuilderArgumentText.forEach { arg ->
                generatedFile.containsMatch("""(?:this\.)?\w*akeFrom\($arg\)""".toPattern())
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

        runKtorGenProcessor(source) { result ->
            result.hasErrorContaining(KtorGenLogger.PARAMETER_WITHOUT_ANNOTATION.trim())
            result.hasErrorCount(1)
            result.hasNoWarnings()
            assertNull(result.findGeneratedSource(TEST_SERVICE_IMPL_PATH))
        }
    }

    @Test
    fun testUnknownLambdaThrowsError() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.GET

                interface TestService {
                    @GET("posts")
                    suspend fun test(builder: StringBuilder.() -> Unit)

                    @GET("posts")
                    suspend fun test2(builder: StringBuilder.(String) -> Unit)
                }
            """.trimIndent(),
        )

        runKtorGenProcessor(source) { compilation ->
            compilation.hasErrorContaining(KtorGenLogger.PARAMETER_WITHOUT_ANNOTATION.trim())
            compilation.hasErrorCount(2)
            compilation.hasNoWarnings()
            assertNull(compilation.findGeneratedSource(TEST_SERVICE_IMPL_PATH))
        }
    }

    @Test
    fun testHttpRequestBuilderLambdaReturnNoUnitThrowsError() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.GET
                import io.ktor.client.request.HttpRequestBuilder

                interface TestService {
                    @GET("posts")
                    suspend fun test(builder: HttpRequestBuilder.() -> String)
                }
            """.trimIndent(),
        )

        runKtorGenProcessor(source) { compilation ->
            compilation.hasErrorContaining(KtorGenLogger.PARAMETER_WITHOUT_ANNOTATION.trim())
            compilation.hasErrorCount(1)
            compilation.hasNoWarnings()
            assertNull(compilation.findGeneratedSource(TEST_SERVICE_IMPL_PATH))
        }
    }
}
