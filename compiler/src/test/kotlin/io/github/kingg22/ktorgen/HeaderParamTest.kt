package io.github.kingg22.ktorgen

class HeaderParamTest {
    @Test
    fun testNoHeaderAnnotationsFoundNoAddItToRequestBuilder() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.GET

                interface TestService {
                    @GET("posts")
                    suspend fun test(): List<Triple<String, Int?,String>>
                }
            """.trimIndent(),
        )

        val notExpectedHeadersArgumentText = "this.headers {"

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generatedFile = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            generatedFile.doesNotContain(notExpectedHeadersArgumentText)
            generatedFile.doesNotContain("setContentType(")
        }
    }

    @Test
    fun testHeaderAnnotationWithNonNullableStringFoundAddHeader() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.GET
                import io.github.kingg22.ktorgen.http.HeaderParam

                interface TestService {
                    @GET("posts")
                    suspend fun test(@HeaderParam("testHeader") testParameter: String): String
                }
            """.trimIndent(),
        )

        val expectedHeadersArgumentText = listOf(
            "this.headers {",
            $$"""this.append("testHeader", "$testParameter")""",
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

    @Test
    fun testHeaderAnnotationWithNullableStringFoundAddHeader() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.GET
                import io.github.kingg22.ktorgen.http.HeaderParam

                interface TestService {
                    @GET("posts")
                    suspend fun test(@HeaderParam("testHeader") testParameter: String?): String
                }
            """.trimIndent(),
        )

        val expectedHeadersArgumentText = $$"""this.append("testHeader", "$testParameter")"""

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generatedFile = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            generatedFile.contains(expectedHeadersArgumentText)
        }
    }

    @Test
    fun testHeaderAnnotationWithNonNullableIntFoundAddHeader() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.GET
                import io.github.kingg22.ktorgen.http.HeaderParam

                interface TestService {
                    @GET("posts")
                    suspend fun test(@HeaderParam("testHeader") testParameter: Int): String
                }
            """.trimIndent(),
        )

        val expectedHeadersArgumentText = $$"""append("testHeader", "$testParameter")"""

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generatedFile = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            generatedFile.contains(expectedHeadersArgumentText)
        }
    }

    @Test
    fun testHeaderAnnotationWithNullableIntFoundAddHeader() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.GET
                import io.github.kingg22.ktorgen.http.HeaderParam

                interface TestService {
                    @GET("posts")
                    suspend fun test(@HeaderParam("testHeader") testParameter: Int?): String
                }
            """.trimIndent(),
        )

        val expectedHeadersArgumentText = listOf(
            "testParameter?.let {",
            $$"""this.append("testHeader", "$testParameter")""",
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

    @Test
    fun testHeaderAnnotationWithListTypeFoundAddHeader() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.GET
                import io.github.kingg22.ktorgen.http.HeaderParam

                interface TestService {
                    @GET("posts")
                    suspend fun test(@HeaderParam("testHeader") testParameter: List<Int>): String
                }
            """.trimIndent(),
        )

        val expectedHeadersArgumentText = listOf(
            "testParameter.forEach {",
            $$"""this.append("testHeader", "$it")""",
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

    @Test
    fun testHeaderAnnotationWithNullableListTypeFoundAddHeader() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.GET
                import io.github.kingg22.ktorgen.http.HeaderParam

                interface TestService {
                    @GET("posts")
                    suspend fun test(@HeaderParam("testHeader") testParameter: List<Int>?): String
                }
            """.trimIndent(),
        )

        val expectedHeadersArgumentText = listOf(
            "testParameter?.forEach {",
            $$"""append("testHeader", "$it")""",
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

    @Test
    fun testHeaderAnnotationWithNullableListTypeAndNullableIntFoundAddHeader() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.GET
                import io.github.kingg22.ktorgen.http.HeaderParam

                interface TestService {
                    @GET("posts")
                    suspend fun test(@HeaderParam("testHeader") testParameter: List<Int?>?): String
                }
            """.trimIndent(),
        )

        val expectedHeadersArgumentText = listOf(
            "testParameter?.filterNotNull()?.forEach {",
            $$"""append("testHeader", "$it")""",
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

    @Test
    fun testHeaderAnnotationWithArrayTypeFoundAddHeader() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.GET
                import io.github.kingg22.ktorgen.http.HeaderParam

                interface TestService {
                    @GET("posts")
                    suspend fun test(@HeaderParam("testHeader") testParameter: Array<String>): String
                }
            """.trimIndent(),
        )

        val expectedHeadersArgumentText = listOf(
            "testParameter.forEach {",
            """append("testHeader", it)""",
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
