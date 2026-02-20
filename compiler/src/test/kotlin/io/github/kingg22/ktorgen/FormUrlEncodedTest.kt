package io.github.kingg22.ktorgen

class FormUrlEncodedTest {
    @Test
    fun testFormUrlEncodedUsedWithNonBodyMethodThrowsError() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.Path
                import io.github.kingg22.ktorgen.http.GET
                import io.github.kingg22.ktorgen.http.Body
                import io.github.kingg22.ktorgen.http.FormUrlEncoded

                interface TestService {
                    @FormUrlEncoded
                    @GET("user/{id}")
                    suspend fun test(@Path id: String): String
                }
            """.trimIndent(),
        )

        runKtorGenProcessor(source) { resultSubject ->
            resultSubject.hasErrorCount(1)
            resultSubject.hasNoWarnings()
            resultSubject.hasErrorContaining(KtorGenLogger.FORM_ENCODED_ANNOTATION_MISMATCH_HTTP_METHOD.trim())
            resultSubject.hasErrorContaining(KtorGenLogger.FORM_ENCODED_MUST_CONTAIN_AT_LEAST_ONE_FIELD.trim())
        }
    }

    @Test
    fun testFormUrlEncodedUsedWithNoFieldAnnotationThrowsError() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.POST
                import io.github.kingg22.ktorgen.http.Body
                import io.github.kingg22.ktorgen.http.FormUrlEncoded

                interface TestService {
                    @FormUrlEncoded
                    @POST("user")
                    suspend fun test(@Body id: String): String
                }
            """.trimIndent(),
        )

        runKtorGenProcessor(source) { resultSubject ->
            resultSubject.hasErrorCount(1)
            resultSubject.hasNoWarnings()
            resultSubject.hasErrorContaining(KtorGenLogger.FORM_ENCODED_MUST_CONTAIN_AT_LEAST_ONE_FIELD.trim())
        }
    }

    @Test
    fun testFormUrlEncodedUsedAddHeader() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.POST
                import io.github.kingg22.ktorgen.http.Field
                import io.github.kingg22.ktorgen.http.FormUrlEncoded

                interface TestService {
                    @FormUrlEncoded
                    @POST("user")
                    suspend fun test(@Field("id") id: String): String?
                }
            """.trimIndent(),
        )

        val expectedHeaderCode = """this.contentType(ContentType.Application.FormUrlEncoded)"""

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            val generated = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            generated.contains(expectedHeaderCode)
        }
    }

    @Test
    fun whenFormUrlEncodedUsedWithMultipart_ThrowCompilationError() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.POST
                import io.github.kingg22.ktorgen.http.Field
                import io.github.kingg22.ktorgen.http.FormUrlEncoded
                import io.github.kingg22.ktorgen.http.Multipart

                interface TestService {
                    @Multipart
                    @FormUrlEncoded
                    @POST("user")
                    suspend fun test(@Field("id") id: String): String
                }
            """.trimIndent(),
        )

        runKtorGenProcessor(source) { result ->
            result.hasErrorCount(1)
            result.hasErrorContaining(KtorGenLogger.CONFLICT_BODY_TYPE)
        }
    }
}
