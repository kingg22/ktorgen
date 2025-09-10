package io.github.kingg22.ktorgen

import androidx.room.compiler.processing.util.Source
import kotlin.test.Test

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
            val resultFile = compilationResultSubject.generatedSourceFileWithPath(
                """com\example\api\_TestServiceImpl.kt""",
            )

            expectedLines.forEach { line ->
                resultFile.contains(line)
            }
        }
    }
}
