package io.github.kingg22.ktorgen

/** Test related to interface extends other interfaces */
class InheritanceTest {
    @Test
    fun testInterfaceGeneratedIsExtendedOtherInterfaceAddDelegationInChild() {
        val source = Source.kotlin(
            SOURCE_FILE_NAME,
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
            CLASS_TEST_SERVICE_IMPL,
            "superTestService: SuperTestService",
            IMPLEMENT_TEST_SERVICE,
            "SuperTestService by superTestService",
            OVERRIDE_FUN_TEST,
            "override suspend fun test2(): String",
        )

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            compilationResultSubject.generatedSourceFileWithPath(
                "com.example.api.SuperTestServiceImpl".toRelativePath(),
            ).contains("HttpMethod.Get")
            val generatedFile = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            for (expectedLine in expectedGeneratedCode) {
                generatedFile.contains(expectedLine)
            }
        }
    }

    @Test
    fun testInterfaceNoGeneratedIsExtendedAddDelegationInChild() {
        val source = Source.kotlin(
            SOURCE_FILE_NAME,
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
            "this.method = HttpMethod.Get",
            CLASS_TEST_SERVICE_IMPL,
            "superTestService1: SuperTestService1,",
            "superTestService2: SuperTestService2",
            IMPLEMENT_TEST_SERVICE,
            "SuperTestService1 by superTestService1",
            "SuperTestService2 by superTestService2",
            OVERRIDE_FUN_TEST,
            "override suspend fun test3(): String",
        )

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            assertNull(
                compilationResultSubject.findGeneratedSource("com.example.api.SuperTestService1Impl".toRelativePath()),
            )
            assertNull(
                compilationResultSubject.findGeneratedSource("com.example.api.SuperTestService2Impl".toRelativePath()),
            )
            val generatedFile = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            for (expectedLine in expectedHeadersArgumentText) {
                generatedFile.contains(expectedLine)
            }
        }
    }

    @Test
    fun testSealeadInterfaceGenerateNormalValidClass() {
        val source = Source.kotlin(
            SOURCE_FILE_NAME,
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.GET

                sealed interface TestService {
                    @GET("posts")
                    suspend fun test(): String
                }
            """.trimIndent(),
        )

        val expectedLines = listOf(
            CLASS_TEST_SERVICE_IMPL,
            IMPLEMENT_TEST_SERVICE,
            OVERRIDE_FUN_TEST,
        )

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generatedFile = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            for (expectedLine in expectedLines) {
                generatedFile.contains(expectedLine)
            }
        }
    }

    companion object {
        private const val OVERRIDE_FUN_TEST = "override suspend fun test(): String"
    }
}
