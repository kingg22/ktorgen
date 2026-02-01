package io.github.kingg22.ktorgen

/** Tests focused on validating return types and the generated bodies. */
class ReturnTypeTest {

    // -- suspend + simple type --
    @Test
    fun suspendSimpleReturnGeneratesBodyCall() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGen
                import io.github.kingg22.ktorgen.http.GET

                @KtorGen
                interface TestService {
                    @GET("/posts")
                    suspend fun test(): String
                }
            """.trimIndent(),
        )

        runKtorGenProcessor(source) { result ->
            result.hasNoWarnings()
            result.hasErrorCount(0)
            val generated = result.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            // must be suspend override and use body<String>()
            generated.contains("override suspend fun test(): String")
            generated.contains(".body()")
        }
    }

    // -- suspend + Result<T> --
    @Test
    fun suspendResultReturnGeneratesTryCatch() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGen
                import io.github.kingg22.ktorgen.http.GET

                @KtorGen
                interface TestService {
                    @GET("/posts")
                    suspend fun test(): Result<String>
                }
            """.trimIndent(),
        )

        runKtorGenProcessor(source) { result ->
            result.hasNoWarnings()
            result.hasErrorCount(0)
            val generated = result.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            generated.contains("override suspend fun test(): Result<String>")
            generated.contains("try {")
            generated.contains(".body<String>()")
            generated.contains("Result.success(_result)")
            generated.contains("catch (_exception: Exception)")
            generated.contains("currentCoroutineContext().ensureActive()")
            generated.contains("Result.failure(_exception)")
        }
    }

    // -- non-suspend + Flow<T> is allowed and generates flow { ... } --
    @Test
    fun flowReturnNonSuspendAllowedAndGeneratesFlowBody() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGen
                import io.github.kingg22.ktorgen.http.GET
                import kotlinx.coroutines.flow.Flow

                @KtorGen
                interface TestService {
                    @GET("/posts")
                    fun test(): Flow<String>
                }
            """.trimIndent(),
        )

        runKtorGenProcessor(source) { result ->
            result.hasNoWarnings()
            result.hasErrorCount(0)
            val generated = result.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            // non-suspend override and uses flow builder
            generated.contains("override fun test(): Flow<String>")
            generated.contains("flow {")
            generated.contains("httpClient.request")
            generated.contains(".body<String>()")
            generated.contains("this.emit(_result)")
        }
    }

    // -- non-suspend + Flow<Result<T>> produces try/catch inside flow and emit Result --
    @Test
    fun flowResultReturnGeneratesFlowResultBody() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGen
                import io.github.kingg22.ktorgen.http.GET
                import kotlinx.coroutines.flow.Flow

                @KtorGen
                interface TestService {
                    @GET("/posts")
                    fun test(): Flow<Result<String>>
                }
            """.trimIndent(),
        )

        runKtorGenProcessor(source) { result ->
            result.hasNoWarnings()
            result.hasErrorCount(0)
            val generated = result.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            generated.contains("override fun test(): Flow<Result<String>>")
            generated.contains("flow {")
            generated.contains("try {")
            generated.contains(".body<String>()")
            generated.contains("this.emit(Result.success(_result))")
            generated.contains("catch (_exception: Exception)")
            generated.contains("this.emit(Result.failure(_exception))")
        }
    }

    // -- validation: non-suspend and non-Flow must be an error --
    @Test
    fun nonSuspendNonFlowThrowsError() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGen
                import io.github.kingg22.ktorgen.http.GET

                @KtorGen
                interface TestService {
                    @GET("/posts")
                    fun test(): String
                }
            """.trimIndent(),
        )

        runKtorGenProcessor(source) { result ->
            result.hasNoWarnings()
            result.hasErrorCount(1)
            // Message is prefixed by constant and finishes with a short return type name
            result.hasErrorContaining(KtorGenLogger.SUSPEND_FUNCTION_OR_FLOW.trim())
        }
    }

    // -- suspend + Unit --
    @Test
    fun suspendUnitReturnGeneratesBodyWithoutReturnValue() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGen
                import io.github.kingg22.ktorgen.http.POST

                @KtorGen
                interface TestService {
                    @POST("/upload")
                    suspend fun upload()
                }
            """.trimIndent(),
        )

        runKtorGenProcessor(source) { result ->
            result.hasNoWarnings()
            result.hasErrorCount(0)
            val generated = result.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)

            generated.contains("override suspend fun upload() {")
            // En Unit, el cuerpo solo ejecuta la llamada, no hay .body() ni retorno
            generated.contains("httpClient.request")
            generated.doesNotContain(".body<")
            generated.doesNotContain("return")
        }
    }

    // -- suspend + Result<Unit> --
    @Test
    fun suspendResultUnitReturnGeneratesTryCatchWithoutBodyCall() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGen
                import io.github.kingg22.ktorgen.http.POST

                @KtorGen
                interface TestService {
                    @POST("/upload")
                    suspend fun upload(): Result<Unit>
                }
            """.trimIndent(),
        )

        runKtorGenProcessor(source) { result ->
            result.hasNoWarnings()
            result.hasErrorCount(0)
            val generated = result.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)

            generated.contains("override suspend fun upload(): Result<Unit>")
            generated.contains("try {")
            generated.contains("httpClient.request")
            generated.doesNotContain(".body<")
            generated.contains("Result.success(Unit)")
            generated.contains("catch (_exception: Exception)")
            generated.contains("Result.failure(_exception)")
        }
    }

    // -- non-suspend + Flow<Unit> --
    @Test
    fun flowUnitReturnGeneratesFlowBodyWithoutBodyCall() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGen
                import io.github.kingg22.ktorgen.http.POST
                import kotlinx.coroutines.flow.Flow

                @KtorGen
                interface TestService {
                    @POST("/upload")
                    fun upload(): Flow<Unit>
                }
            """.trimIndent(),
        )

        runKtorGenProcessor(source) { result ->
            result.hasNoWarnings()
            result.hasErrorCount(0)
            val generated = result.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)

            generated.contains("override fun upload(): Flow<Unit>")
            generated.contains("flow {")
            generated.contains("httpClient.request")
            generated.doesNotContain(".body<")
            generated.contains("this.emit(Unit)")
        }
    }

    // -- non-suspend + Flow<Result<Unit>> --
    @Test
    fun flowResultUnitReturnGeneratesTryCatchWithEmitResult() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGen
                import io.github.kingg22.ktorgen.http.POST
                import kotlinx.coroutines.flow.Flow

                @KtorGen
                interface TestService {
                    @POST("/upload")
                    fun upload(): Flow<Result<Unit>>
                }
            """.trimIndent(),
        )

        runKtorGenProcessor(source) { result ->
            result.hasNoWarnings()
            result.hasErrorCount(0)
            val generated = result.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)

            generated.contains("override fun upload(): Flow<Result<Unit>>")
            generated.contains("flow {")
            generated.contains("try {")
            generated.contains("httpClient.request")
            generated.doesNotContain(".body<")
            generated.contains("this.emit(Result.success(Unit))")
            generated.contains("catch (_exception: Exception)")
            generated.contains("this.emit(Result.failure(_exception))")
        }
    }

    // -- non-suspend + HttpRequestBuilder --
    @Test
    fun nonSuspendHttpRequestBuilderGeneratesBuilderApply() {
        val source = Source.kotlin(
            "Source.kt",
            """
            package com.example.api

            import io.github.kingg22.ktorgen.core.KtorGen
            import io.github.kingg22.ktorgen.http.GET
            import io.ktor.client.request.HttpRequestBuilder

            @KtorGen
            interface TestService {
                @GET("/posts")
                fun test(): HttpRequestBuilder
            }
            """.trimIndent(),
        )

        runKtorGenProcessor(source) { result ->
            result.hasNoWarnings()
            result.hasErrorCount(0)

            val generated = result.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)

            generated.contains("override fun test(): HttpRequestBuilder")
            generated.contains("HttpRequestBuilder().apply")
            generated.contains("this.url {")
            generated.doesNotContain(".body<")
            generated.doesNotContain("flow {")
        }
    }

    // -- suspend + HttpRequestBuilder --
    @Test
    fun suspendHttpRequestBuilderGeneratesBuilderApply() {
        val source = Source.kotlin(
            "Source.kt",
            """
            package com.example.api

            import io.github.kingg22.ktorgen.core.KtorGen
            import io.github.kingg22.ktorgen.http.DELETE
            import io.ktor.client.request.HttpRequestBuilder

            @KtorGen
            interface TestService {
                @DELETE("/posts")
                suspend fun delete(): HttpRequestBuilder
            }
            """.trimIndent(),
        )

        runKtorGenProcessor(source) { result ->
            result.hasNoWarnings()
            result.hasErrorCount(0)

            val generated = result.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)

            generated.contains("override suspend fun delete(): HttpRequestBuilder")
            generated.contains("HttpRequestBuilder().apply")
            generated.doesNotContain(".body<")
            generated.doesNotContain("HttpStatement")
        }
    }

    // -- non-suspend + HttpStatement --
    @Test
    fun nonSuspendHttpStatementGeneratesStatementWithBuilder() {
        val source = Source.kotlin(
            "Source.kt",
            """
            package com.example.api

            import io.github.kingg22.ktorgen.core.KtorGen
            import io.github.kingg22.ktorgen.http.POST
            import io.ktor.client.statement.HttpStatement

            @KtorGen
            interface TestService {
                @POST("/login")
                fun login(): HttpStatement
            }
            """.trimIndent(),
        )

        runKtorGenProcessor(source) { result ->
            result.hasNoWarnings()
            result.hasErrorCount(0)

            val generated = result.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)

            generated.contains("override fun login(): HttpStatement")
            generated.contains("val _requestBuilder = HttpRequestBuilder().apply")
            generated.contains("return HttpStatement(_requestBuilder")
            generated.doesNotContain(".body<")
            generated.doesNotContain("flow {")
        }
    }

    // -- suspend + HttpStatement --
    @Test
    fun suspendHttpStatementGeneratesStatementWithBuilder() {
        val source = Source.kotlin(
            "Source.kt",
            """
            package com.example.api

            import io.github.kingg22.ktorgen.core.KtorGen
            import io.github.kingg22.ktorgen.http.POST
            import io.ktor.client.statement.HttpStatement

            @KtorGen
            interface TestService {
                @POST("/upload")
                suspend fun upload(): HttpStatement
            }
            """.trimIndent(),
        )

        runKtorGenProcessor(source) { result ->
            result.hasNoWarnings()
            result.hasErrorCount(0)

            val generated = result.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)

            generated.contains("override suspend fun upload(): HttpStatement")
            generated.contains("val _requestBuilder = HttpRequestBuilder().apply")
            generated.contains("return HttpStatement(_requestBuilder")
            generated.doesNotContain(".body<")
            generated.doesNotContain("Result")
        }
    }

    // -- suspend + Result<HttpRequestBuilder> --
    @Test
    fun suspendResultHttpRequestBuilderGeneratesTryCatchBuilder() {
        val source = Source.kotlin(
            "Source.kt",
            """
            package com.example.api

            import io.github.kingg22.ktorgen.core.KtorGen
            import io.github.kingg22.ktorgen.http.GET
            import io.ktor.client.request.HttpRequestBuilder

            @KtorGen
            interface TestService {
                @GET("/posts")
                suspend fun test(): Result<HttpRequestBuilder>
            }
            """.trimIndent(),
        )

        runKtorGenProcessor(source) { result ->
            result.hasNoWarnings()
            result.hasErrorCount(0)

            val generated = result.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)

            generated.contains("override suspend fun test(): Result<HttpRequestBuilder>")
            generated.contains("try {")
            generated.contains("HttpRequestBuilder().apply")
            generated.contains("Result.success")
            generated.contains("catch (_exception: Exception)")
            generated.contains("ensureActive()")
            generated.contains("Result.failure(_exception)")
            generated.doesNotContain(".body<")
            generated.doesNotContain("HttpStatement")
        }
    }

    // -- non-suspend + Result<HttpRequestBuilder> --
    @Test
    fun nonSuspendResultHttpRequestBuilderGeneratesTryCatchBuilder() {
        val source = Source.kotlin(
            "Source.kt",
            """
            package com.example.api

            import io.github.kingg22.ktorgen.core.KtorGen
            import io.github.kingg22.ktorgen.http.GET
            import io.ktor.client.request.HttpRequestBuilder

            @KtorGen
            interface TestService {
                @GET("/posts")
                fun test(): Result<HttpRequestBuilder>
            }
            """.trimIndent(),
        )

        runKtorGenProcessor(source) { result ->
            result.hasNoWarnings()
            result.hasErrorCount(0)

            val generated = result.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)

            generated.contains("override fun test(): Result<HttpRequestBuilder>")
            generated.contains("catch (_exception: Exception)")
            generated.doesNotContain("ensureActive()")
        }
    }

    // -- suspend + Result<HttpStatement> --
    @Test
    fun suspendResultHttpStatementGeneratesTryCatchStatement() {
        val source = Source.kotlin(
            "Source.kt",
            """
            package com.example.api

            import io.github.kingg22.ktorgen.core.KtorGen
            import io.github.kingg22.ktorgen.http.POST
            import io.ktor.client.statement.HttpStatement

            @KtorGen
            interface TestService {
                @POST("/login")
                suspend fun login(): Result<HttpStatement>
            }
            """.trimIndent(),
        )

        runKtorGenProcessor(source) { result ->
            result.hasNoWarnings()
            result.hasErrorCount(0)

            val generated = result.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)

            generated.contains("override suspend fun login(): Result<HttpStatement>")
            generated.contains("val _requestBuilder = HttpRequestBuilder().apply")
            generated.contains("Result.success(HttpStatement")
            generated.contains("catch (_exception: Exception)")
            generated.contains("ensureActive()")
            generated.doesNotContain(".body<")
        }
    }

    @Test
    fun nonSuspendResultHttpStatementGeneratesTryCatchStatement() {
        val source = Source.kotlin(
            "Source.kt",
            """
            package com.example.api

            import io.github.kingg22.ktorgen.core.KtorGen
            import io.github.kingg22.ktorgen.http.POST
            import io.ktor.client.statement.HttpStatement

            @KtorGen
            interface TestService {
                @POST("/login")
                fun login(): Result<HttpStatement>
            }
            """.trimIndent(),
        )

        runKtorGenProcessor(source) { result ->
            result.hasNoWarnings()
            result.hasErrorCount(0)

            val generated = result.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)

            generated.contains("override fun login(): Result<HttpStatement>")
            generated.contains("catch (_exception: Exception)")
            generated.doesNotContain("ensureActive()")
        }
    }
}
