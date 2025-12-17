package io.github.kingg22.ktorgen

class HttpMethodTest {
    @Test
    fun testFunctionWithGET() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.GET

                interface TestService {
                    @GET("user")
                    suspend fun test(): String
                }
            """.trimIndent(),
        )
        val expectedSource = "method = HttpMethod.Get"

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            val actualSource = compilationResultSubject.generatedSourceFileWithPath(
                "com.example.api._TestServiceImpl".toRelativePath(),
            )
            actualSource.contains(expectedSource)
        }
    }

    @Test
    fun testCustomHttpMethod() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.HTTP
                import io.github.kingg22.ktorgen.http.Body

                interface TestService {
                    @HTTP("CUSTOM","user")
                    suspend fun test(): String
                }
            """.trimIndent(),
        )

        val expectedSource = """this.method = HttpMethod.parse("CUSTOM")"""

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            val actualSource = compilationResultSubject.generatedSourceFileWithPath(
                "com.example.api._TestServiceImpl".toRelativePath(),
            )
            actualSource.contains(expectedSource)
        }
    }

    @Test
    fun testCustomHttpMethodWithBody() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.HTTP
                import io.github.kingg22.ktorgen.http.Header
                import io.github.kingg22.ktorgen.http.Body

                interface TestService {
                    @Header("Content-Type", "application/json")
                    @HTTP("GET2", "user", true)
                    suspend fun test(@Body body: String): String
                }
            """.trimIndent(),
        )

        val expectedFunctionText = listOf(
            """this.method = HttpMethod.parse("GET2")""",
            "this.takeFrom(".stringTemplate("user") + ")",
            "this.setBody(body)",
        )

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            val actualSource = compilationResultSubject.generatedSourceFileWithPath(
                "com.example.api._TestServiceImpl".toRelativePath(),
            )
            for (expectedLine in expectedFunctionText) {
                actualSource.contains(expectedLine)
            }
        }
    }

    @Test
    fun testMultipleHttpMethodsFoundThrowsError() {
        val source = Source.kotlin(
            "com.example.api.GithubService.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.GET
                import io.github.kingg22.ktorgen.http.POST

                interface GithubService {
                    @GET("user/followers")
                    @POST("repos/foso/experimental/issues")
                    suspend fun test(): String
                }
            """.trimIndent(),
        )

        runKtorGenProcessor(source) { result ->
            result.hasNoWarnings()
            result.hasErrorContaining(KtorGenLogger.ONLY_ONE_HTTP_METHOD_IS_ALLOWED.trim())
        }
    }
}
