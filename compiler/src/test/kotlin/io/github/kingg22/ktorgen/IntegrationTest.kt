package io.github.kingg22.ktorgen

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class IntegrationTest {
    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun getWithBodyThrowWarningsAsError(kspVersion: KSPVersion) {
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

        runKtorGenProcessor(source, kspVersion = kspVersion) { compilationResultSubject ->
            compilationResultSubject.hasError()
            compilationResultSubject.hasErrorCount(1)
        }
    }

    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun validGetGeneratesImplAndCanBeReferenced(kspVersion: KSPVersion) {
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
                // Use the generated class to ensure it compiles and the function default, need to cast because return interface
                @Suppress("unused") // Don't trigger unused warning error
                val ref: _MyApiImpl = MyApi(httpClient = HttpClient()) as _MyApiImpl
            """.trimIndent(),
        )

        runKtorGenProcessor(api, useGenerated, kspVersion = kspVersion) { compilationResultSubject ->
            compilationResultSubject.hasErrorCount(0)
        }
    }

    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun privateInterfaceCantGeneratedThrowsError(kspVersion: KSPVersion) {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGen

                @KtorGen
                private interface TestService
            """.trimIndent(),
        )

        runKtorGenProcessor(source, kspVersion = kspVersion) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(1)
            compilationResultSubject.hasErrorContaining(KtorGenLogger.PRIVATE_INTERFACE_CANT_GENERATE.trim())
        }
    }
}
