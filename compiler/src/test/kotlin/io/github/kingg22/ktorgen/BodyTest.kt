package io.github.kingg22.ktorgen

import androidx.room.compiler.processing.util.Source
import kotlin.test.Test

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
            val generatedFile = compilationResultSubject.generatedSourceFileWithPath(
                """com\example\api\_TestServiceImpl.kt""",
            )
            generatedFile.contains(expectedBodyDataArgumentText)
        }
    }

    @Test
    fun testFunctionWithBodyAndNotContentTypeHeader() {
        val source = Source.kotlin(
            "TestServiceWithoutContentTypeHeader.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.*

                interface TestServiceWithoutContentTypeHeader {
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
            compilationResultSubject.generatedSourceFileWithPath(
                """com\example\api\_TestServiceWithoutContentTypeHeaderImpl.kt""",
            ).contains(expectedBodyDataArgumentText)
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
            val generatedFile = compilationResultSubject.generatedSourceFileWithPath(
                """com\example\api\_TestServiceImpl.kt""",
            )
            generatedFile.doesNotContain(notExpectedBodyDataArgumentText)
        }
    }
}
