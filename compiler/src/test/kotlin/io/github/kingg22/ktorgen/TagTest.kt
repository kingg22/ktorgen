package io.github.kingg22.ktorgen

class TagTest {
    @Test
    fun testTagsAnnotationFoundAddedAsAttributeKey() {
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

        runKtorGenProcessor(source) { compilationResultSubject ->
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
