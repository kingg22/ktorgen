package io.github.kingg22.ktorgen

import androidx.room.compiler.processing.util.Source
import kotlin.test.Test

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
                // Use the generated class to ensure it compiles and the function default, need cast because return interface
                @Suppress("unused")
                val ref: _MyApiImpl = MyApi(httpClient = HttpClient()) as _MyApiImpl
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

        runKtorGenProcessor(source) {
            it.hasNoWarnings()
            it.hasErrorCount(1)
            it.hasErrorContaining(KtorGenLogger.PRIVATE_INTERFACE_CANT_GENERATE.trim())
        }
    }
}
