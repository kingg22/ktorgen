package io.github.kingg22.ktorgen

import androidx.room.compiler.processing.util.Source
import kotlin.test.Test
import kotlin.test.assertNull

/** Test related to interface extends other interfaces */
class InheritanceTest {
    @Test
    fun testInterfaceGeneratedIsExtendedOtherInterfaceAddDelegationInChild() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.GET

                interface SuperTestService{
                    @GET("posts")
                    suspend fun test(): String
                }

                interface TestService : SuperTestService {
                    @GET("post")
                    override suspend fun test(): String
                    @GET("posts")
                    suspend fun test2(): String
                }
            """.trimIndent(),
        )

        val expectedGeneratedCode = listOf(
            "public class _TestServiceImpl",
            "superTestService: SuperTestService",
            ") : TestService",
            "SuperTestService by superTestService",
            "override suspend fun test(): String",
            "override suspend fun test2(): String",
        )

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            compilationResultSubject.generatedSourceFileWithPath(
                "com.example.api._SuperTestServiceImpl".toRelativePath(),
            ).contains("HttpMethod.parse(\"GET\")")
            val generatedFile = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            for (expectedLine in expectedGeneratedCode) {
                generatedFile.contains(expectedLine)
            }
        }
    }

    @Test
    fun testInterfaceNoGeneratedIsExtendedAddDelegationInChild() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGen
                import io.github.kingg22.ktorgen.http.GET

                @KtorGen(generate=false)
                interface SuperTestService1 {
                    suspend fun test(): String
                }

                @KtorGen(generate=false)
                interface SuperTestService2 {
                    @GET("posts")
                    suspend fun test2(): String
                }

                interface TestService : SuperTestService1, SuperTestService2 {
                    @GET("posts")
                    override suspend fun test(): String

                    @GET("posts")
                    suspend fun test3(): String
                }
            """.trimIndent(),
        )

        val expectedHeadersArgumentText = listOf(
            "this.method = HttpMethod.parse(\"GET\")",
            "public class _TestServiceImpl",
            "superTestService1: SuperTestService1,",
            "superTestService2: SuperTestService2",
            ") : TestService",
            "SuperTestService1 by superTestService1",
            "SuperTestService2 by superTestService2",
            "override suspend fun test(): String",
            "override suspend fun test3(): String",
        )

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            assertNull(
                compilationResultSubject.findGeneratedSource("com.example.api._SuperTestService1Impl".toRelativePath()),
            )
            assertNull(
                compilationResultSubject.findGeneratedSource("com.example.api._SuperTestService2Impl".toRelativePath()),
            )
            val generatedFile = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            for (expectedLine in expectedHeadersArgumentText) {
                generatedFile.contains(expectedLine)
            }
        }
    }
}
