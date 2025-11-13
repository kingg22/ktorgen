package io.github.kingg22.ktorgen

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class CookieTest {
    // -- positives --
    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun cookieOnFunctionGeneratesCookieCall(kspVersion: KSPVersion) {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGen
                import io.github.kingg22.ktorgen.http.GET
                import io.github.kingg22.ktorgen.http.Cookie

                @KtorGen
                interface TestService {
                    @Cookie(
                        name = "session_id",
                        value = "abc123",
                        maxAge = 60,
                        expiresTimestamp = 1735689600000,
                        domain = "example.com",
                        path = "/",
                        secure = true,
                        httpOnly = false,
                        extensions = [Cookie.PairString(Cookie.SameSite, Cookie.SameSites.Strict)]
                    )
                    @GET("user")
                    suspend fun test(): String
                }
            """.trimIndent(),
        )

        val expectedLines = listOf(
            "this.takeFrom(" stringTemplate "user",
            "this.cookie(",
            "name = \"session_id\"",
            "value = " stringTemplate "abc123",
            "maxAge = 60",
            "expires = GMTDate(1_735_689_600_000)",
            "domain = " stringTemplate "example.com",
            "path = " stringTemplate "/",
            "secure = true",
            "httpOnly = false",
            "extensions = mapOf",
            stringTemplate("SameSite") + (" to " stringTemplate "Strict"),
        )

        runKtorGenProcessor(source, kspVersion = kspVersion) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generated = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            expectedLines.forEach { line ->
                generated.contains(line)
            }
        }
    }

    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun cookieOnParameterGeneratesCookieCallUsingParamValue(kspVersion: KSPVersion) {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGen
                import io.github.kingg22.ktorgen.http.GET
                import io.github.kingg22.ktorgen.http.Cookie

                @KtorGen
                interface TestService {
                    @GET("user")
                    suspend fun test(@Cookie("Auth") token: String): String
                }
            """.trimIndent(),
        )

        val expectedLines = listOf(
            "this.takeFrom(" stringTemplate "user",
            "this.cookie(",
            "name = \"Auth\"",
            "value = " stringTemplate $$"$token",
            "expires = null",
            "extensions = emptyMap()",
        )

        runKtorGenProcessor(source, kspVersion = kspVersion) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generated = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            expectedLines.forEach { line ->
                generated.contains(line)
            }
        }
    }

    // -- negatives --
    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun cookieOnFunctionWithoutValueThrowsError(kspVersion: KSPVersion) {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGen
                import io.github.kingg22.ktorgen.http.GET
                import io.github.kingg22.ktorgen.http.Cookie

                @KtorGen
                interface TestService {
                    @Cookie(name = "session_id")
                    @GET("user")
                    suspend fun test(): String
                }
            """.trimIndent(),
        )

        runKtorGenProcessor(source, kspVersion = kspVersion) { compilationResultSubject ->
            compilationResultSubject.hasWarningCount(0)
            compilationResultSubject.hasErrorCount(2) // androidx room compiler count exception as error, total = 2
            compilationResultSubject.hasErrorContaining(KtorGenLogger.COOKIE_ON_FUNCTION_WITHOUT_VALUE.trim())
        }
    }

    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun varargParameterWithMultipleCookieAnnotationsWarns(kspVersion: KSPVersion) {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGen
                import io.github.kingg22.ktorgen.http.GET
                import io.github.kingg22.ktorgen.http.Cookie

                @KtorGen
                interface TestService {
                    @GET("user")
                    suspend fun test(@Cookie("A") @Cookie("B") vararg cookieValues: String): String
                }
            """.trimIndent(),
        )

        runKtorGenProcessor(source, kspVersion = kspVersion) { result ->
            result.hasNoWarnings()
            result.hasErrorCount(1)
            result.hasErrorContaining(KtorGenLogger.VARARG_PARAMETER_EXPERIMENTAL.trim())
            result.hasErrorContaining(KtorGenLogger.VARARG_PARAMETER_WITH_LOT_ANNOTATIONS.trim())
        }
    }
}
