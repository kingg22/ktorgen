package io.github.kingg22.ktorgen

import androidx.room.compiler.processing.util.Source
import kotlin.test.Test

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

        runKtorGenProcessor(source) {
            it.hasErrorCount(1)
            it.hasNoWarnings()
            it.hasErrorContaining(KtorGenLogger.FORM_ENCODED_ANNOTATION_MISMATCH_HTTP_METHOD.trim())
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

        runKtorGenProcessor(source) {
            it.hasErrorCount(1)
            it.hasNoWarnings()
            it.hasErrorContaining(KtorGenLogger.FORM_ENCODED_MUST_CONTAIN_AT_LEAST_ONE_FIELD.trim())
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
            val generated = compilationResultSubject.generatedSourceFileWithPath(
                "com.example.api._TestServiceImpl".toRelativePath(),
            )
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

        runKtorGenProcessor(source) {
            it.hasErrorCount(1)
            it.hasErrorContaining(KtorGenLogger.CONFLICT_BODY_TYPE)
        }
    }
}
