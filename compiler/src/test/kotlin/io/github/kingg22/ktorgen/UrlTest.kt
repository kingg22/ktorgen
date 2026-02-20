package io.github.kingg22.ktorgen

/** Test related to Url construction */
class UrlTest {
    @Test
    fun testFunctionWithGet() {
        val source = Source.kotlin(
            "com.example.api.TestService.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.GET

                interface TestService {
                    @GET("user")
                    suspend fun test(): String
                }
            """.trimIndent(),
        )

        val expectedFunctionSource = "takeFrom(".stringTemplate("user") + ")"

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject
                .generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
                .contains(expectedFunctionSource)
        }
    }

    @Test
    fun testFunctionWithGetAndPath() {
        val source = Source.kotlin(
            "com.example.api.TestService.kt",
            """
                package com.example.api
                import io.github.kingg22.ktorgen.http.GET
                import io.github.kingg22.ktorgen.http.Path

                interface TestService {
                    @GET("user/{id}")
                    suspend fun test(@Path("id") userId: String): String
                }
            """.trimIndent(),
        )

        val expectedFunctionText = "takeFrom(".stringTemplate($$"""user/${"$userId".encodeURLPath()}""") + ")"

        runKtorGenProcessor(source) { compilationResultSubject ->
            val actualSource = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            actualSource.contains(expectedFunctionText)
        }
    }

    @Test
    fun testFunctionWithGETAndPathWithInferredName() {
        val source = Source.kotlin(
            "com.example.api.TestService.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.GET
                import io.github.kingg22.ktorgen.http.Path

                interface TestService {
                    @GET("user/{userId}")
                    suspend fun test(@Path userId: String): String
                }
            """.trimIndent(),
        )

        val expectedFunctionText = "takeFrom(".stringTemplate($$"""user/${"$userId".encodeURLPath()}""") + ")"

        runKtorGenProcessor(source) { compilationResultSubject ->
            val generated = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            generated.contains(expectedFunctionText)
        }
    }

    @Test
    fun testFunctionWithGetAndUrl() {
        val source = Source.kotlin(
            "com.example.api.TestService.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.GET
                import io.github.kingg22.ktorgen.http.Url

                interface TestService {
                    @GET
                    suspend fun test(@Url url: String): String
                }
            """.trimIndent(),
        )

        val expectedFunctionSource = "takeFrom(url)"

        runKtorGenProcessor(source) { compilationResultSubject ->
            val generated = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            generated.contains(expectedFunctionSource)
        }
    }

    // Multiple
    @Test
    fun testGetNoHavePathSuccess() {
        val source = Source.kotlin(
            "com.example.api.TestService.kt",
            """
                package com.example.api
                import io.github.kingg22.ktorgen.http.GET

                interface TestService {
                    @GET
                    suspend fun test(): String
                }
            """.trimIndent(),
        )

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject
                .generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
                .contains("override suspend fun test(): String")
        }
    }

    @Test
    fun testWhenMultipleParameterWithUrlAnnotationThrowsError() {
        val source = Source.kotlin(
            "com.example.api.TestServiceMultipleUrl.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.GET
                import io.github.kingg22.ktorgen.http.Url

                interface TestServiceMultipleUrl {
                    @GET
                    suspend fun test(@Url url: String, @Url url2: String): String
                }
            """.trimIndent(),
        )

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasError()
            compilationResultSubject.hasErrorCount(1)
            compilationResultSubject.hasErrorContaining(KtorGenLogger.MULTIPLE_URL_FOUND.trim())
        }
    }

    @Test
    fun testFunctionWithGetAndAlreadyEncodedPath() {
        val source = Source.kotlin(
            "com.example.api.TestService.kt",
            """
                package com.example.api
                import io.github.kingg22.ktorgen.http.GET
                import io.github.kingg22.ktorgen.http.Path

                interface TestService {
                    @GET("user/{id}")
                    suspend fun test(@Path("id", encoded = true) id: String): String
                }
            """.trimIndent(),
        )

        val expectedFunctionText = "takeFrom(".stringTemplate($$"user/$id") + ")"

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            val actualSource = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            actualSource.contains(expectedFunctionText)
        }
    }

    @Test
    fun testPathParameterIsNullableThrowsError() {
        val source = Source.kotlin(
            "com.example.api.TestServiceWithNullablePath.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.GET
                import io.github.kingg22.ktorgen.http.Path

                interface TestServiceWithNullablePath {
                    @GET("user/{id}")
                    suspend fun test(@Path("id") id: String?): String
                }
            """.trimIndent(),
        )

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasErrorCount(1)
            compilationResultSubject.hasErrorContaining(KtorGenLogger.PATH_PARAMETER_TYPE_MAY_NOT_BE_NULLABLE.trim())
        }
    }

    @Test
    fun testPathWithEmptyHttpAnnotationValueThrowsError() {
        val source = Source.kotlin(
            "com.example.api.TestServiceWithEmptyHttpAnnotationValue.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.GET
                import io.github.kingg22.ktorgen.http.Path

                interface TestServiceWithEmptyHttpAnnotationValue {
                    @GET
                    suspend fun test(@Path("id") id: String): String
                }
            """.trimIndent(),
        )

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasErrorCount(1)
            compilationResultSubject.hasErrorContaining(KtorGenLogger.PATH_CAN_ONLY_BE_USED_WITH_RELATIVE_URL_ON)
        }
    }
}
