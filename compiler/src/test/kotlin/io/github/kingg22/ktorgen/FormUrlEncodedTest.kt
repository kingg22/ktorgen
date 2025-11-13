package io.github.kingg22.ktorgen

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class FormUrlEncodedTest {
    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun testFormUrlEncodedUsedWithNonBodyMethodThrowsError(kspVersion: KSPVersion) {
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

        runKtorGenProcessor(source, kspVersion = kspVersion) { resultSubject ->
            resultSubject.hasErrorCount(1)
            resultSubject.hasNoWarnings()
            resultSubject.hasErrorContaining(KtorGenLogger.FORM_ENCODED_ANNOTATION_MISMATCH_HTTP_METHOD.trim())
            resultSubject.hasErrorContaining(KtorGenLogger.FORM_ENCODED_MUST_CONTAIN_AT_LEAST_ONE_FIELD.trim())
        }
    }

    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun testFormUrlEncodedUsedWithNoFieldAnnotationThrowsError(kspVersion: KSPVersion) {
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

        runKtorGenProcessor(source, kspVersion = kspVersion) { resultSubject ->
            resultSubject.hasErrorCount(1)
            resultSubject.hasNoWarnings()
            resultSubject.hasErrorContaining(KtorGenLogger.FORM_ENCODED_MUST_CONTAIN_AT_LEAST_ONE_FIELD.trim())
        }
    }

    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun testFormUrlEncodedUsedAddHeader(kspVersion: KSPVersion) {
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

        runKtorGenProcessor(source, kspVersion = kspVersion) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            val generated = compilationResultSubject.generatedSourceFileWithPath(
                "com.example.api._TestServiceImpl".toRelativePath(),
            )
            generated.contains(expectedHeaderCode)
        }
    }

    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun whenFormUrlEncodedUsedWithMultipart_ThrowCompilationError(kspVersion: KSPVersion) {
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

        runKtorGenProcessor(source, kspVersion = kspVersion) { result ->
            result.hasErrorCount(1)
            result.hasErrorContaining(KtorGenLogger.CONFLICT_BODY_TYPE)
        }
    }
}
