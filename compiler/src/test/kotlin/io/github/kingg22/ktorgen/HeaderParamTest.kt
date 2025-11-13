package io.github.kingg22.ktorgen

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class HeaderParamTest {
    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun testNoHeaderAnnotationsFoundNoAddItToRequestBuilder(kspVersion: KSPVersion) {
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

        runKtorGenProcessor(source, kspVersion = kspVersion) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generatedFile = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            generatedFile.doesNotContain(notExpectedHeadersArgumentText)
            generatedFile.doesNotContain("setContentType(")
        }
    }

    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun testHeaderAnnotationWithNonNullableStringFoundAddHeader(kspVersion: KSPVersion) {
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

        runKtorGenProcessor(source, kspVersion = kspVersion) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generatedFile = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            expectedHeadersArgumentText.forEach { line ->
                generatedFile.contains(line)
            }
        }
    }

    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun testHeaderAnnotationWithNullableStringFoundAddHeader(kspVersion: KSPVersion) {
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

        runKtorGenProcessor(source, kspVersion = kspVersion) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generatedFile = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            generatedFile.contains(expectedHeadersArgumentText)
        }
    }

    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun testHeaderAnnotationWithNonNullableIntFoundAddHeader(kspVersion: KSPVersion) {
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

        runKtorGenProcessor(source, kspVersion = kspVersion) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generatedFile = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            generatedFile.contains(expectedHeadersArgumentText)
        }
    }

    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun testHeaderAnnotationWithNullableIntFoundAddHeader(kspVersion: KSPVersion) {
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

        runKtorGenProcessor(source, kspVersion = kspVersion) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generatedFile = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            expectedHeadersArgumentText.forEach { line ->
                generatedFile.contains(line)
            }
        }
    }

    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun testHeaderAnnotationWithListTypeFoundAddHeader(kspVersion: KSPVersion) {
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

        runKtorGenProcessor(source, kspVersion = kspVersion) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generatedFile = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            expectedHeadersArgumentText.forEach { line ->
                generatedFile.contains(line)
            }
        }
    }

    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun testHeaderAnnotationWithNullableListTypeFoundAddHeader(kspVersion: KSPVersion) {
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

        runKtorGenProcessor(source, kspVersion = kspVersion) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generatedFile = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            expectedHeadersArgumentText.forEach { line ->
                generatedFile.contains(line)
            }
        }
    }

    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun testHeaderAnnotationWithNullableListTypeAndNullableIntFoundAddHeader(kspVersion: KSPVersion) {
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

        runKtorGenProcessor(source, kspVersion = kspVersion) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generatedFile = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            expectedHeadersArgumentText.forEach { line ->
                generatedFile.contains(line)
            }
        }
    }

    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun testHeaderAnnotationWithArrayTypeFoundAddHeader(kspVersion: KSPVersion) {
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

        runKtorGenProcessor(source, kspVersion = kspVersion) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generatedFile = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            expectedHeadersArgumentText.forEach { line ->
                generatedFile.contains(line)
            }
        }
    }
}
