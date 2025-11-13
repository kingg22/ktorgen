package io.github.kingg22.ktorgen

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

/** Tests focused on validating return types and the generated bodies. */
class ReturnTypeTest {

    // -- suspend + simple type --
    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun suspendSimpleReturnGeneratesBodyCall(kspVersion: KSPVersion) {
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

        runKtorGenProcessor(source, kspVersion = kspVersion) { result ->
            result.hasNoWarnings()
            result.hasErrorCount(0)
            val generated = result.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            // must be suspend override and use body<String>()
            generated.contains("override suspend fun test(): String")
            generated.contains(".body()")
        }
    }

    // -- suspend + Result<T> --
    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun suspendResultReturnGeneratesTryCatch(kspVersion: KSPVersion) {
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

        runKtorGenProcessor(source, kspVersion = kspVersion) { result ->
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
    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun flowReturnNonSuspendAllowedAndGeneratesFlowBody(kspVersion: KSPVersion) {
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

        runKtorGenProcessor(source, kspVersion = kspVersion) { result ->
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
    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun flowResultReturnGeneratesFlowResultBody(kspVersion: KSPVersion) {
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

        runKtorGenProcessor(source, kspVersion = kspVersion) { result ->
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
    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun nonSuspendNonFlowThrowsError(kspVersion: KSPVersion) {
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

        runKtorGenProcessor(source, kspVersion = kspVersion) { result ->
            result.hasNoWarnings()
            result.hasErrorCount(1)
            // Message is prefixed by constant and finishes with a short return type name
            result.hasErrorContaining(KtorGenLogger.SUSPEND_FUNCTION_OR_FLOW.trim())
        }
    }

    // -- suspend + Unit --
    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun suspendUnitReturnGeneratesBodyWithoutReturnValue(kspVersion: KSPVersion) {
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

        runKtorGenProcessor(source, kspVersion = kspVersion) { result ->
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
    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun suspendResultUnitReturnGeneratesTryCatchWithoutBodyCall(kspVersion: KSPVersion) {
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

        runKtorGenProcessor(source, kspVersion = kspVersion) { result ->
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
    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun flowUnitReturnGeneratesFlowBodyWithoutBodyCall(kspVersion: KSPVersion) {
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

        runKtorGenProcessor(source, kspVersion = kspVersion) { result ->
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
    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun flowResultUnitReturnGeneratesTryCatchWithEmitResult(kspVersion: KSPVersion) {
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

        runKtorGenProcessor(source, kspVersion = kspVersion) { result ->
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
}
