package io.github.kingg22.ktorgen

class HeaderTest {
    @Test
    fun testAllHeaderAnnotations() {
        val source = Source.kotlin(
            "com.example.api.TestService.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.*

                interface TestService {
                    @Header("x", "y")
                    @Header("a", "b")
                    @GET("posts")
                    suspend fun test(
                        @HeaderParam("testHeader") testParameterNonNullable: String?,
                        @HeaderParam("testHeader") testParameterNullable: String?,
                        @HeaderMap testParameter2: Map<String,String>,
                    ): String
                }
            """.trimIndent(),
        )

        val expectedLines = listOf(
            $$"""this.append("testHeader", "$testParameterNonNullable")""",
            $$"""this.append("testHeader", "$testParameterNullable")""",
            """this.append(it.key, it.value)""",
            """this.append("x", "y")""",
            """this.append("a", "b")""",
        )

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val resultFile = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            expectedLines.forEach { line ->
                resultFile.contains(line)
            }
        }
    }

    @Test
    fun testHeadersAnnotationFoundAddHeader() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.GET
                import io.github.kingg22.ktorgen.http.Header

                interface TestService {
                    @Header("x", "y")
                    @GET("posts")
                    suspend fun test(): String
                }
            """.trimIndent(),
        )

        val expectedHeadersArgumentText = listOf(
            "this.headers {",
            """this.append("x", "y")""",
        )

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generatedFile = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            expectedHeadersArgumentText.forEach { line ->
                generatedFile.contains(line)
            }
        }
    }
}
