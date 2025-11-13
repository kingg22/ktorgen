package io.github.kingg22.ktorgen

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class QueryTest {
    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun testNotEncodedQueryAnnotationAndEncoded(kspVersion: KSPVersion) {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.GET
                import io.github.kingg22.ktorgen.http.Query

                interface TestService {
                    @GET("posts")
                    suspend fun test(
                        @Query("name") testQuery: String,
                        @Query(encoded = true) testQuery2: Int,
                    )
                }
            """.trimIndent(),
        )

        val expectedQueriesArgumentText = listOf(
            "this.takeFrom(".stringTemplate("posts"),
            "testQuery?.let {",
            """this.parameters.append("name", """ stringTemplate $$"$it",
            "testQuery2?.let {",
            """encodedParameters.append("testQuery2", """ stringTemplate $$"$it",
        )

        runKtorGenProcessor(source, kspVersion = kspVersion) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            val generatedFile = compilationResultSubject.generatedSourceFileWithPath(
                "com.example.api._TestServiceImpl".toRelativePath(),
            )
            for (expectedLine in expectedQueriesArgumentText) {
                generatedFile.contains(expectedLine)
            }
        }
    }

    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun testQueryAnnotationWithList(kspVersion: KSPVersion) {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.GET
                import io.github.kingg22.ktorgen.http.Query

                interface TestService {
                    @GET("posts")
                    suspend fun test(@Query("user",true) testQuery2: List<String>)
                }
            """.trimIndent(),
        )

        val expectedQueriesArgumentText = listOf(
            "this.takeFrom(".stringTemplate("posts"),
            "testQuery2?.filterNotNull()?.forEach {",
            """encodedParameters.append("user", """ stringTemplate $$"$it",
        )

        runKtorGenProcessor(source, kspVersion = kspVersion) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            val generatedFile = compilationResultSubject.generatedSourceFileWithPath(
                "com.example.api._TestServiceImpl".toRelativePath(),
            )
            for (expectedLine in expectedQueriesArgumentText) {
                generatedFile.contains(expectedLine)
            }
        }
    }

    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun testQueryNamesFound(kspVersion: KSPVersion) {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.GET
                import io.github.kingg22.ktorgen.http.QueryName

                interface TestService {
                    @GET("posts")
                    suspend fun test(@QueryName testQueryName: String, @QueryName testQueryName2: String)
                }
            """.trimIndent(),
        )

        val expectedQueriesArgumentText = listOf(
            "this.takeFrom(" stringTemplate "posts",
            "parameters.appendAll(".stringTemplate($$"$testQueryName") + ", emptyList())",
            "parameters.appendAll(".stringTemplate($$"$testQueryName2") + ", emptyList())",
        )

        runKtorGenProcessor(source, kspVersion = kspVersion) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            val generatedFile = compilationResultSubject.generatedSourceFileWithPath(
                "com.example.api._TestServiceImpl".toRelativePath(),
            )
            for (expectedLine in expectedQueriesArgumentText) {
                generatedFile.contains(expectedLine)
            }
        }
    }

    // TODO can add a query with value int and convert to string? Can be nullable?
    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun testNotEncodedQueryMapAnnotationAndEncodedFound(kspVersion: KSPVersion) {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.GET
                import io.github.kingg22.ktorgen.http.QueryMap

                interface TestService {
                    @GET("posts")
                    suspend fun test(
                        @QueryMap testQueryMap: Map<String, String>,
                        @QueryMap(true) testQueryMap2: Map<String, String>,
                    )
                }
            """.trimIndent(),
        )

        val expectedQueriesArgumentText = listOf(
            "this.takeFrom(" stringTemplate "posts",
            "testQueryMap?.forEach { entry ->",
            "this.parameters.append(entry.key, " stringTemplate $$"$value",
            "testQueryMap2?.forEach { entry ->",
            "entry.value?.let { value ->",
            "encodedParameters.append(entry.key, " stringTemplate $$"$value",
        )

        runKtorGenProcessor(source, kspVersion = kspVersion) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            val generatedFile = compilationResultSubject.generatedSourceFileWithPath(
                "com.example.api._TestServiceImpl".toRelativePath(),
            )
            for (expectedLine in expectedQueriesArgumentText) {
                generatedFile.contains(expectedLine)
            }
            compilationResultSubject.hasErrorCount(0)
        }
    }

    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun testFunctionWithQueryAndQueryNameAndQueryMap(kspVersion: KSPVersion) {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.GET
                import io.github.kingg22.ktorgen.http.QueryMap
                import io.github.kingg22.ktorgen.http.Query
                import io.github.kingg22.ktorgen.http.QueryName

                interface TestService {
                    @GET("posts")
                    suspend fun example(
                        @Query("name") testQuery: String,
                        @QueryName testQueryName: String,
                        @QueryMap(true) name: Map<String, String>,
                    )
                }
            """.trimIndent(),
        )

        val expectedQueriesArgumentText = listOf(
            "this.takeFrom(" stringTemplate "posts",
            "testQuery?.let {",
            """this.parameters.append("name", """ stringTemplate $$"$it",
            "parameters.appendAll(".stringTemplate($$"$testQueryName") + ", emptyList())",
            "name?.forEach { entry ->",
            "entry.value?.let { value ->",
            "encodedParameters.append(entry.key, " stringTemplate $$"$value",
        )

        runKtorGenProcessor(source, kspVersion = kspVersion) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            val generatedFile = compilationResultSubject.generatedSourceFileWithPath(
                "com.example.api._TestServiceImpl".toRelativePath(),
            )
            for (expectedLine in expectedQueriesArgumentText) {
                generatedFile.contains(expectedLine)
            }
        }
    }

    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun testQueryMapTypeIsNotMapThrowsError(kspVersion: KSPVersion) {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.GET
                import io.github.kingg22.ktorgen.http.QueryMap

                interface TestService {
                    @GET("posts")
                    suspend fun test(@QueryMap testQueryMap: String)
                }
            """.trimIndent(),
        )

        runKtorGenProcessor(source, kspVersion = kspVersion) { result ->
            result.hasNoWarnings()
            result.hasErrorContaining(KtorGenLogger.QUERY_MAP_PARAMETER_TYPE_MUST_BE_MAP_PAIR_STRING)
        }
    }

    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun testQueryMapKeysIsNotStringThrowsError(kspVersion: KSPVersion) {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.GET
                import io.github.kingg22.ktorgen.http.QueryMap

                interface TestService {
                    @GET("posts")
                    suspend fun test(@QueryMap() testQueryMap: Map<Int, String>)
                }
            """.trimIndent(),
        )

        runKtorGenProcessor(source, kspVersion = kspVersion) { result ->
            result.hasNoWarnings()
            result.hasErrorContaining(KtorGenLogger.QUERY_MAP_PARAMETER_TYPE_MUST_BE_MAP_PAIR_STRING)
        }
    }
}
