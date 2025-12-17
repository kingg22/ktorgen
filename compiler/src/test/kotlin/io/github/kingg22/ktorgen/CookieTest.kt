package io.github.kingg22.ktorgen

class CookieTest {
    // -- positives --
    @Test
    fun cookieOnFunctionGeneratesCookieCall() {
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

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generated = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            expectedLines.forEach { line ->
                generated.contains(line)
            }
        }
    }

    @Test
    fun cookieOnParameterGeneratesCookieCallUsingParamValue() {
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

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generated = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            expectedLines.forEach { line ->
                generated.contains(line)
            }
        }
    }

    // -- negatives --
    @Test
    fun cookieOnFunctionWithoutValueThrowsError() {
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

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasWarningCount(0)
            compilationResultSubject.hasErrorCount(2) // androidx room compiler count exception as error, total = 2
            compilationResultSubject.hasErrorContaining(KtorGenLogger.COOKIE_ON_FUNCTION_WITHOUT_VALUE.trim())
        }
    }

    @Test
    fun varargParameterWithMultipleCookieAnnotationsWarns() {
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

        runKtorGenProcessor(source) { result ->
            result.hasNoWarnings()
            result.hasErrorCount(1)
            result.hasErrorContaining(KtorGenLogger.VARARG_PARAMETER_EXPERIMENTAL.trim())
            result.hasErrorContaining(KtorGenLogger.VARARG_PARAMETER_WITH_LOT_ANNOTATIONS.trim())
        }
    }
}
