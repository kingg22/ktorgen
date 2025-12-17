package io.github.kingg22.ktorgen

/** Tests for [@Fragment][io.github.kingg22.ktorgen.http.Fragment] */
class FragmentTest {
    // -- on the function target --
    @Test
    fun testFragmentNotEncoded_generatesFragmentAssignment() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.GET
                import io.github.kingg22.ktorgen.http.Fragment

                interface TestService {
                    @GET("posts")
                    @Fragment("header")
                    suspend fun test(): String
                }
            """.trimIndent(),
        )

        val expected = "this.fragment = " stringTemplate "header"

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generatedFile = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            generatedFile.contains(expected)
        }
    }

    @Test
    fun testFragmentEncoded_generatesEncodedFragmentAssignment() {
        val source = Source.kotlin(
            "Source2.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.GET
                import io.github.kingg22.ktorgen.http.Fragment

                interface TestService {
                    @GET("posts")
                    @Fragment(value = "header%20text", encoded = true)
                    suspend fun test(): String
                }
            """.trimIndent(),
        )

        val expected = "this.encodedFragment = " stringTemplate "header%20text"

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generatedFile = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            generatedFile.contains(expected)
        }
    }
}
