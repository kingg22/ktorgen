package io.github.kingg22.ktorgen

import androidx.room.compiler.processing.util.Source
import kotlin.test.Test

/** Tests focused on constructor and properties generation (ConstructorGenerator) */
class ConstructorGeneratorTest {
    // Interface declares HttpClient property -> generated class must override it (not private)
    // and factory function must include it among constructor params.
    @Test
    fun constructorWithHttpClientProperty_overridesAndFactoryContainsIt() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGen
                import io.github.kingg22.ktorgen.http.GET
                import io.ktor.client.HttpClient

                @KtorGen
                interface TestService {
                    val httpClient: HttpClient
                    val otraCosa: Boolean
                    var valorCambiante: Boolean?
                    val token: String

                    @GET
                    suspend fun test(): String
                }
            """.trimIndent(),
        )

        // Expect: top-level factory function has all constructor parameters including httpClient
        val expectedFactoryLine = "_TestServiceImpl(httpClient, otraCosa, valorCambiante, token)"

        // Expect: class has override properties (httpClient is override, not private)
        val expectedClassHeader = "public class _TestServiceImpl"
        val expectedOverrideHttpClient = "override val httpClient: HttpClient"
        val expectedOverrideOtraCosa = "override val otraCosa: Boolean"
        val expectedOverrideValorCambiante = "override var valorCambiante: Boolean?"
        val expectedOverrideToken = "override val token: String"

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generated = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            generated.contains(expectedClassHeader)
            generated.contains(expectedFactoryLine)
            generated.contains(expectedOverrideHttpClient)
            generated.contains(expectedOverrideOtraCosa)
            generated.contains(expectedOverrideValorCambiante)
            generated.contains(expectedOverrideToken)
        }
    }

    // Interface DOES NOT declare HttpClient property -> generated class must keep a private _httpClient
    // and factory function should still expose parameter named httpClient (not _httpClient).
    @Test
    fun constructorWithoutHttpClientProperty_usesPrivateAndFactoryHasHttpClientParam() {
        val source = Source.kotlin(
            "Source2.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGen
                import io.github.kingg22.ktorgen.http.GET

                @KtorGen
                interface TestService {
                    val otraCosa: Boolean
                    var valorCambiante: Boolean?

                    @GET
                    suspend fun test(): String
                }
            """.trimIndent(),
        )

        val expectedFactoryLine = "_TestServiceImpl(httpClient, otraCosa, valorCambiante)"

        val expectedClassHeader = "public class _TestServiceImpl"
        val expectedPrivateHttpClient = "private val _httpClient: HttpClient"
        val nonExpectedOverrideHttpClient = "override val httpClient: HttpClient"
        val expectedOverrideOtraCosa = "override val otraCosa: Boolean"
        val expectedOverrideValorCambiante = "override var valorCambiante: Boolean?"

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generated = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            generated.contains(expectedClassHeader)
            generated.contains(expectedFactoryLine)
            generated.contains(expectedPrivateHttpClient)
            generated.doesNotContain(nonExpectedOverrideHttpClient)
            generated.contains(expectedOverrideOtraCosa)
            generated.contains(expectedOverrideValorCambiante)
        }
    }
}
