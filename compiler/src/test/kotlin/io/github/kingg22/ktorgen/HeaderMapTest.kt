package io.github.kingg22.ktorgen

class HeaderMapTest {
    @Test
    fun testNullableHeaderMapWithStringValueAnnotationFound_AddHeader() {
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

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val actualSource = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            for (expectedLine in expectedHeadersArgumentText) {
                actualSource.contains(expectedLine)
            }
        }
    }

    @Test
    fun testHeaderMapWithNullableStringValueAnnotationFound_AddHeader() {
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

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val actualSource = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            for (expectedLine in expectedHeadersArgumentText) {
                actualSource.contains(expectedLine)
            }
        }
    }

    @Test
    fun testHeaderMapTypeIsNotMapThrowsError() {
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

        runKtorGenProcessor(source) { result ->
            result.hasErrorCount(1)
            result.hasErrorContaining(KtorGenLogger.HEADER_MAP_PARAMETER_TYPE_MUST_BE_MAP_PAIR_STRING)
        }
    }

    @Test
    fun testHeaderMapKeysIsNotStringThrowsError() {
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

        runKtorGenProcessor(source) { result ->
            result.hasErrorCount(1)
            result.hasErrorContaining(KtorGenLogger.HEADER_MAP_PARAMETER_TYPE_MUST_BE_MAP_PAIR_STRING)
        }
    }
}
