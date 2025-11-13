package io.github.kingg22.ktorgen

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

/** Test related to Url construction */
class UrlTest {
    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun testFunctionWithGet(kspVersion: KSPVersion) {
        val source = Source.kotlin(
            "com.example.api.TestService.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.GET

                interface TestService {
                    @GET("user")
                    suspend fun test(): String
                }
            """.trimIndent(),
        )

        val expectedFunctionSource = "takeFrom(".stringTemplate("user") + ")"

        runKtorGenProcessor(source, kspVersion = kspVersion) { compilationResultSubject ->
            compilationResultSubject
                .generatedSourceFileWithPath("com.example.api._TestServiceImpl".toRelativePath())
                .contains(expectedFunctionSource)
        }
    }

    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun testFunctionWithGetAndPath(kspVersion: KSPVersion) {
        val source = Source.kotlin(
            "com.example.api.TestServiceWithPath.kt",
            """
                package com.example.api
                import io.github.kingg22.ktorgen.http.GET
                import io.github.kingg22.ktorgen.http.Path

                interface TestServiceWithPath {
                    @GET("user/{id}")
                    suspend fun test(@Path("id") userId: String): String
                }
            """.trimIndent(),
        )

        val expectedFunctionText = "takeFrom(".stringTemplate($$"""user/${"$userId".encodeURLPath()}""") + ")"

        runKtorGenProcessor(source, kspVersion = kspVersion) { compilationResultSubject ->
            val actualSource = compilationResultSubject.generatedSourceFileWithPath(
                "com.example.api._TestServiceWithPathImpl".toRelativePath(),
            )
            actualSource.contains(expectedFunctionText)
        }
    }

    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun testFunctionWithGETAndPathWithInferredName(kspVersion: KSPVersion) {
        val source = Source.kotlin(
            "com.example.api.TestServiceWithPathInferredName.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.GET
                import io.github.kingg22.ktorgen.http.Path

                interface TestServiceWithPathInferredName {
                    @GET("user/{userId}")
                    suspend fun test(@Path userId: String): String
                }
            """.trimIndent(),
        )

        val expectedFunctionText = "takeFrom(".stringTemplate($$"""user/${"$userId".encodeURLPath()}""") + ")"

        runKtorGenProcessor(source, kspVersion = kspVersion) { compilationResultSubject ->
            val generated = compilationResultSubject.generatedSourceFileWithPath(
                "com.example.api._TestServiceWithPathInferredNameImpl".toRelativePath(),
            )
            generated.contains(expectedFunctionText)
        }
    }

    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun testFunctionWithGetAndUrl(kspVersion: KSPVersion) {
        val source = Source.kotlin(
            "com.example.api.TestServiceWithUrl.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.GET
                import io.github.kingg22.ktorgen.http.Url

                interface TestServiceWithUrl {
                    @GET
                    suspend fun test(@Url url: String): String
                }
            """.trimIndent(),
        )

        val expectedFunctionSource = "takeFrom(url)"

        runKtorGenProcessor(source, kspVersion = kspVersion) { compilationResultSubject ->
            val generated = compilationResultSubject.generatedSourceFileWithPath(
                "com.example.api._TestServiceWithUrlImpl".toRelativePath(),
            )
            generated.contains(expectedFunctionSource)
        }
    }

    // Multiple
    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun testGetNoHavePathSuccess(kspVersion: KSPVersion) {
        val source = Source.kotlin(
            "com.example.api.TestServiceGetEmpty.kt",
            """
                package com.example.api
                import io.github.kingg22.ktorgen.http.GET

                interface TestServiceGetEmpty {
                    @GET
                    suspend fun test(): String
                }
            """.trimIndent(),
        )

        runKtorGenProcessor(source, kspVersion = kspVersion) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject
                .generatedSourceFileWithPath("com.example.api._TestServiceGetEmptyImpl".toRelativePath())
                .contains("override suspend fun test(): String")
        }
    }

    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun testWhenMultipleParameterWithUrlAnnotationThrowsError(kspVersion: KSPVersion) {
        val source = Source.kotlin(
            "com.example.api.TestServiceMultipleUrl.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.GET
                import io.github.kingg22.ktorgen.http.Url

                interface TestServiceMultipleUrl {
                    @GET
                    suspend fun test(@Url url: String, @Url url2: String): String
                }
            """.trimIndent(),
        )

        runKtorGenProcessor(source, kspVersion = kspVersion) { compilationResultSubject ->
            compilationResultSubject.hasError()
            compilationResultSubject.hasErrorCount(1)
            compilationResultSubject.hasErrorContaining(KtorGenLogger.MULTIPLE_URL_FOUND.trim())
        }
    }

    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun testFunctionWithGetAndAlreadyEncodedPath(kspVersion: KSPVersion) {
        val source = Source.kotlin(
            "com.example.api.TestServiceWithEncodedPath.kt",
            """
                package com.example.api
                import io.github.kingg22.ktorgen.http.GET
                import io.github.kingg22.ktorgen.http.Path

                interface TestServiceWithEncodedPath {
                    @GET("user/{id}")
                    suspend fun test(@Path("id", encoded = true) id: String): String
                }
            """.trimIndent(),
        )

        val expectedFunctionText = "takeFrom(".stringTemplate($$"user/$id") + ")"

        runKtorGenProcessor(source, kspVersion = kspVersion) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            val actualSource = compilationResultSubject.generatedSourceFileWithPath(
                "com.example.api._TestServiceWithEncodedPathImpl".toRelativePath(),
            )
            actualSource.contains(expectedFunctionText)
        }
    }

    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun testPathParameterIsNullableThrowsError(kspVersion: KSPVersion) {
        val source = Source.kotlin(
            "com.example.api.TestServiceWithNullablePath.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.GET
                import io.github.kingg22.ktorgen.http.Path

                interface TestServiceWithNullablePath {
                    @GET("user/{id}")
                    suspend fun test(@Path("id") id: String?): String
                }
            """.trimIndent(),
        )

        runKtorGenProcessor(source, kspVersion = kspVersion) { compilationResultSubject ->
            compilationResultSubject.hasErrorCount(1)
            compilationResultSubject.hasErrorContaining(KtorGenLogger.PATH_PARAMETER_TYPE_MAY_NOT_BE_NULLABLE.trim())
        }
    }

    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun testPathWithEmptyHttpAnnotationValueThrowsError(kspVersion: KSPVersion) {
        val source = Source.kotlin(
            "com.example.api.TestServiceWithEmptyHttpAnnotationValue.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.GET
                import io.github.kingg22.ktorgen.http.Path

                interface TestServiceWithEmptyHttpAnnotationValue {
                    @GET
                    suspend fun test(@Path("id") id: String): String
                }
            """.trimIndent(),
        )

        runKtorGenProcessor(source, kspVersion = kspVersion) { compilationResultSubject ->
            compilationResultSubject.hasErrorCount(1)
            compilationResultSubject.hasErrorContaining(KtorGenLogger.PATH_CAN_ONLY_BE_USED_WITH_RELATIVE_URL_ON)
        }
    }
}
