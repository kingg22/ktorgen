package io.github.kingg22.ktorgen

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class HeaderMapTest {
    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun testNullableHeaderMapWithStringValueAnnotationFound_AddHeader(kspVersion: KSPVersion) {
        val source =
            Source.kotlin(
                "Source.kt",
                """
                package com.example.api

                import io.github.kingg22.ktorgen.http.GET
                import io.github.kingg22.ktorgen.http.HeaderMap

                interface TestService {
                    @GET("posts")
                    suspend fun test(@HeaderMap testParameter: Map<String,String>?): String
                }
                """.trimIndent(),
            )

        val expectedHeadersArgumentText = listOf("testParameter?.forEach {", "this.append(it.key, it.value)")

        runKtorGenProcessor(source, kspVersion = kspVersion) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val actualSource = compilationResultSubject.generatedSourceFileWithPath(
                "com.example.api._TestServiceImpl".toRelativePath(),
            )
            for (expectedLine in expectedHeadersArgumentText) {
                actualSource.contains(expectedLine)
            }
        }
    }

    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun testHeaderMapWithNullableStringValueAnnotationFound_AddHeader(kspVersion: KSPVersion) {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.GET
                import io.github.kingg22.ktorgen.http.HeaderMap

                interface TestService {
                    @GET("posts")
                    suspend fun test(@HeaderMap testParameter: Map<String,String?>): String
                }
            """.trimIndent(),
        )

        val expectedHeadersArgumentText = listOf(
            "testParameter.forEach {",
            "it.value?.let { value ->",
            "this.append(it.key, value)",
        )

        runKtorGenProcessor(source, kspVersion = kspVersion) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val actualSource = compilationResultSubject.generatedSourceFileWithPath(
                "com.example.api._TestServiceImpl".toRelativePath(),
            )
            for (expectedLine in expectedHeadersArgumentText) {
                actualSource.contains(expectedLine)
            }
        }
    }

    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun testHeaderMapTypeIsNotMapThrowsError(kspVersion: KSPVersion) {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.GET
                import io.github.kingg22.ktorgen.http.HeaderMap

                interface TestService {
                    @GET("posts")
                    suspend fun test(@HeaderMap testParameter: String): String
                }
            """.trimIndent(),
        )

        runKtorGenProcessor(source, kspVersion = kspVersion) { result ->
            result.hasErrorCount(1)
            result.hasErrorContaining(KtorGenLogger.HEADER_MAP_PARAMETER_TYPE_MUST_BE_MAP_PAIR_STRING)
        }
    }

    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun testHeaderMapKeysIsNotStringThrowsError(kspVersion: KSPVersion) {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.GET
                import io.github.kingg22.ktorgen.http.HeaderMap

                interface TestService {
                    @GET("posts")
                    suspend fun test(@HeaderMap testParameter: Map<Int,String>): String
                }
            """.trimIndent(),
        )

        runKtorGenProcessor(source, kspVersion = kspVersion) { result ->
            result.hasErrorCount(1)
            result.hasErrorContaining(KtorGenLogger.HEADER_MAP_PARAMETER_TYPE_MUST_BE_MAP_PAIR_STRING)
        }
    }
}
