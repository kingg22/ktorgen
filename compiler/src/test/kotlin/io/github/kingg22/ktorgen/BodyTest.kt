package io.github.kingg22.ktorgen

class BodyTest {
    @Test
    fun testBodyUsedWithFormUrlEncodedThrowsError() {
        val source = Source.kotlin(
            "com.example.api.TestServiceWithFormUrlEncoded.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.POST
                import io.github.kingg22.ktorgen.http.Body
                import io.github.kingg22.ktorgen.http.FormUrlEncoded

                interface TestServiceWithFormUrlEncoded {
                    @FormUrlEncoded
                    @POST("user")
                    suspend fun test(@Body id: String): String
                }
            """.trimIndent(),
        )

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasErrorCount(1)
            compilationResultSubject.hasErrorContaining(KtorGenLogger.CONFLICT_BODY_TYPE)
        }
    }

    @Test
    fun testFunctionWithBody() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.*

                interface TestService {
                    @Header("Content-Type", Header.ContentTypes.Application.Json)
                    @POST("user")
                    suspend fun test(@Body id: String?): String
                }
            """.trimIndent(),
        )

        val expectedBodyDataArgumentText = "setBody(id)"

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generatedFile = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            generatedFile.contains(expectedBodyDataArgumentText)
        }
    }

    @Test
    fun testFunctionWithBodyAndNotContentTypeHeader() {
        val source = Source.kotlin(
            "TestService.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.*

                interface TestService {
                    @POST
                    suspend fun test(@Body id: Int?): String
                }
            """.trimIndent(),
        )

        val expectedBodyDataArgumentText = "setBody(id)"

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(1)
            compilationResultSubject.hasErrorContaining(KtorGenLogger.CONTENT_TYPE_BODY_UNKNOWN.trim())
            compilationResultSubject
                .generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
                .contains(expectedBodyDataArgumentText)
        }
    }

    @Test
    fun testNoBodyAnnotationsFoundNoGeneratedSetBody() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.GET

                interface TestService {
                    @GET("posts")
                    suspend fun test(): String
                }
            """.trimIndent(),
        )

        val notExpectedBodyDataArgumentText = "setBody("

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject
                .generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
                .doesNotContain(notExpectedBodyDataArgumentText)
        }
    }
}
