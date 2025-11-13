package io.github.kingg22.ktorgen

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class TagTest {
    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun testTagsAnnotationFoundAddedAsAttributeKey(kspVersion: KSPVersion) {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.GET
                import io.github.kingg22.ktorgen.http.Tag

                interface TestService {
                    @GET("posts")
                    suspend fun test(@Tag myTag1 : String, @Tag("myTag2") someParameter: Int?): String
                }
            """.trimIndent(),
        )

        val expectedHeadersArgumentText = listOf(
            """attributes.put(AttributeKey("myTag1"), myTag1)""",
            """attributes.put(AttributeKey("myTag2"), _value)""",
        )

        runKtorGenProcessor(source, kspVersion = kspVersion) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.generatedSourceFileWithPath(
                "com.example.api._TestServiceImpl".toRelativePath(),
            ).let { actualSource ->
                for (expectedLine in expectedHeadersArgumentText) {
                    actualSource.contains(expectedLine)
                }
            }
        }
    }
}
