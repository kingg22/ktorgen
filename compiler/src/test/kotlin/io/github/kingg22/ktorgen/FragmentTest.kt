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

    @Test
    fun testFragmentEmpty_throwsError() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.GET
                import io.github.kingg22.ktorgen.http.Fragment

                interface TestService {
                    @GET("posts")
                    @Fragment(encoded = true)
                    suspend fun test(): String
                }
            """.trimIndent(),
        )

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(1)
            compilationResultSubject.hasErrorContaining(KtorGenLogger.URL_FRAGMENT_IN_FUNCTION_IS_BLANK)
        }
    }

    // -- on parameter target --
    @Test
    fun testFragmentParam_generatesFragmentAssignmentParam() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.GET
                import io.github.kingg22.ktorgen.http.Fragment

                interface TestService {
                    @GET("posts")
                    suspend fun test(@Fragment section: String): String
                }
            """.trimIndent(),
        )

        val expected = $$"this.fragment = \"$section\""

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generatedFile = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            generatedFile.contains(expected)
        }
    }

    @Test
    fun testFragmentNullableParam_generatesFragmentAssignmentParamSafety() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.GET
                import io.github.kingg22.ktorgen.http.Fragment

                interface TestService {
                    @GET("posts")
                    suspend fun test(@Fragment section: String?): String
                }
            """.trimIndent(),
        )

        val expected = listOf("section?.let", $$"this.fragment = \"$it\"")

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generatedFile = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            for (expected in expected) {
                generatedFile.contains(expected)
            }
        }
    }

    @Test
    fun testEncodedFragmentParam_generatesFragmentAssignmentParam() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.GET
                import io.github.kingg22.ktorgen.http.Fragment

                interface TestService {
                    @GET("posts")
                    suspend fun test(@Fragment(encoded = true) section: String): String
                }
            """.trimIndent(),
        )

        val expected = $$"this.encodedFragment = \"$section\""

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generatedFile = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            generatedFile.contains(expected)
        }
    }

    @Test
    fun testEncodedFragmentNullableParam_generatesFragmentAssignmentParamSafety() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.GET
                import io.github.kingg22.ktorgen.http.Fragment

                interface TestService {
                    @GET("posts")
                    suspend fun test(@Fragment(encoded = true) section: String?): String
                }
            """.trimIndent(),
        )

        val expected = listOf("section?.let", $$"this.encodedFragment = \"$it\"")

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generatedFile = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            for (expected in expected) {
                generatedFile.contains(expected)
            }
        }
    }

    @Test
    fun testMultipleFragmentParam_throwsError() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.GET
                import io.github.kingg22.ktorgen.http.Fragment

                interface TestService {
                    @GET("posts")
                    suspend fun test(@Fragment section: String, @Fragment subSection: String): String
                }
            """.trimIndent(),
        )

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(1)
            compilationResultSubject.hasErrorContaining(KtorGenLogger.MULTIPLE_URL_FRAGMENT)
        }
    }

    @Test
    fun testMultipleFragment_throwsError() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.GET
                import io.github.kingg22.ktorgen.http.Fragment

                interface TestService {
                    @GET("posts")
                    @Fragment("#L")
                    suspend fun test(@Fragment section: String): String
                }
            """.trimIndent(),
        )

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(1)
            compilationResultSubject.hasErrorContaining(KtorGenLogger.MULTIPLE_URL_FRAGMENT)
        }
    }
}
