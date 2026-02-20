package io.github.kingg22.ktorgen

class IntegrationTest {
    @Test
    fun getWithBodyThrowWarningsAsError() {
        val source = Source.kotlin(
            "foo.bar.MyGetWarnings.kt",
            """
                package foo.bar

                import io.github.kingg22.ktorgen.http.*

                data class IssueData(val title: String, val body: String)

                interface MyGetWarnings {
                    // ⚠️ @GET con @Body
                    @GET("users/filter")
                    suspend fun getUsersWithBody(@Body filter: IssueData): List<Any>
                }
            """.trimIndent(),
        )

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasError()
            compilationResultSubject.hasErrorCount(1)
        }
    }

    @Test
    fun validGetGeneratesImplAndCanBeReferenced() {
        val api = Source.kotlin(
            "foo.bar.MyApi.kt",
            """
                package foo.bar

                import io.github.kingg22.ktorgen.http.GET

                interface MyApi {
                    @GET("/users")
                    suspend fun listUsers(): List<String>
                }
            """.trimIndent(),
        )
        val useGenerated = Source.kotlin(
            "foo.bar.UseImpl.kt",
            """
                package foo.bar

                import io.ktor.client.HttpClient

                // Refer to the generated class to ensure it exists and is public
                // Use the generated class to ensure it compiles and the function default
                @Suppress("unused") // Don't trigger unused warning error
                val ref = MyApiImpl(_httpClient = HttpClient())
            """.trimIndent(),
        )

        runKtorGenProcessor(api, useGenerated) { compilationResultSubject ->
            compilationResultSubject.hasErrorCount(0)
        }
    }

    @Test
    fun privateInterfaceCantGeneratedThrowsError() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGen

                @KtorGen
                private interface TestService
            """.trimIndent(),
        )

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(1)
            compilationResultSubject.hasErrorContaining(KtorGenLogger.PRIVATE_INTERFACE_CANT_GENERATE.trim())
        }
    }
}
