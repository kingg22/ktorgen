package io.github.kingg22.ktorgen

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

/** Test related to the [@Part][io.github.kingg22.ktorgen.http.Part] annotation */
class PartTest {
    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun testNoPartAnnotationsFoundNoCreateFormData(kspVersion: KSPVersion) {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.GET

                interface TestService {
                    @GET("posts")
                    suspend fun test(): String
                }
            """.trimIndent(),
        )

        val noExpectedPartsArgumentText = listOf(MULTIPART_FORM_DATA, PART_DATA_LIST)

        runKtorGenProcessor(source, kspVersion = kspVersion) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            val actualSource = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            noExpectedPartsArgumentText.forEach {
                actualSource.doesNotContain(it)
            }
        }
    }

    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun testPartAnnotationFoundAndAddItToPartsArgument(kspVersion: KSPVersion) {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.POST
                import io.github.kingg22.ktorgen.http.Multipart
                import io.github.kingg22.ktorgen.http.Part

                interface TestService {
                    @POST("posts")
                    @Multipart
                    suspend fun test(@Part("name") testPart: String)
                }
            """.trimIndent(),
        )

        val expectedPartsArgumentText = listOf(
            MULTIPART_FORM_DATA,
            SET_BODY_MULTIPART_FORM_DATA,
            "testPart?.let {",
            """append("name", """ stringTemplate $$"$it",
        )

        runKtorGenProcessor(source, kspVersion = kspVersion) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            val actualSource = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            expectedPartsArgumentText.forEach {
                actualSource.contains(it)
            }
        }
    }

    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun testPartAnnotationWithListFoundAddItToPartsArgument(kspVersion: KSPVersion) {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.POST
                import io.github.kingg22.ktorgen.http.Multipart
                import io.github.kingg22.ktorgen.http.Part

                interface TestService {
                    @POST("posts")
                    @Multipart
                    suspend fun test(@Part("name") testPart: List<String>)
                }
            """.trimIndent(),
        )

        val expectedPartsArgumentText = listOf(
            MULTIPART_FORM_DATA,
            PART_DATA_LIST,
            "testPart?.filterNotNull()?.forEach {",
            """this.append("name", """ stringTemplate $$"$it",
            SET_BODY_MULTIPART_FORM_DATA,
        )

        runKtorGenProcessor(source, kspVersion = kspVersion) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            val actualSource = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            expectedPartsArgumentText.forEach {
                actualSource.contains(it)
            }
        }
    }

    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun testPartAnnotationWithoutMultipartAnnotation(kspVersion: KSPVersion) {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.POST
                import io.github.kingg22.ktorgen.http.Multipart
                import io.github.kingg22.ktorgen.http.Part

                interface TestService {
                    @POST("posts")
                    suspend fun test(@Part("name") testPart: List<String>)
                }
            """.trimIndent(),
        )

        val expectedPartsArgumentText = listOf(
            MULTIPART_FORM_DATA,
            PART_DATA_LIST,
            "testPart?.filterNotNull()?.forEach {",
            """this.append("name", """ stringTemplate $$"$it",
            SET_BODY_MULTIPART_FORM_DATA,
        )

        runKtorGenProcessor(
            source,
            kspVersion = kspVersion,
            processorOptions = mapOf(
                KtorGenOptions.STRICK_CHECK_TYPE to KtorGenOptions.ErrorsLoggingType.Off.intValue.toString(),
            ),
        ) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val actualSource = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            expectedPartsArgumentText.forEach { actualSource.contains(it) }
        }
    }

    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun testPartMapAnnotationFoundAddItToPartsArgument(kspVersion: KSPVersion) {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.Multipart
                import io.github.kingg22.ktorgen.http.PartMap
                import io.github.kingg22.ktorgen.http.POST

                interface TestService {
                    @POST("posts")
                    @Multipart
                    suspend fun test(@PartMap() testPartMap: Map<String, String>)
                }
            """.trimIndent(),
        )

        val expectedPartsArgumentText = listOf(
            PART_DATA_LIST,
            MULTIPART_FORM_DATA,
            SET_BODY_MULTIPART_FORM_DATA,
            "testPartMap?.forEach { entry ->",
            "entry.value?.let {",
            "append(entry.key, " stringTemplate $$"$value",
        )

        runKtorGenProcessor(source, kspVersion = kspVersion) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            val actualSource = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            expectedPartsArgumentText.forEach {
                actualSource.contains(it)
            }
        }
    }

    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun testFunctionWithPartAndPartMap(kspVersion: KSPVersion) {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.POST
                import io.github.kingg22.ktorgen.http.Multipart
                import io.github.kingg22.ktorgen.http.PartMap
                import io.github.kingg22.ktorgen.http.Part

                interface TestService {
                    @POST("posts")
                    @Multipart
                    suspend fun example(@Part("name") testPart: String, @PartMap() name: Map<String, String>)
                }
            """.trimIndent(),
        )

        val expectedPartsArgumentText = listOf(
            PART_DATA_LIST,
            MULTIPART_FORM_DATA,
            SET_BODY_MULTIPART_FORM_DATA,
            "testPart?.let {",
            """append("name", """ stringTemplate $$"$it",
            "name?.forEach { entry ->",
            "entry.value?.let {",
            "append(entry.key, " stringTemplate $$"$value",
        )

        runKtorGenProcessor(source, kspVersion = kspVersion) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            val actualSource = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            expectedPartsArgumentText.forEach {
                actualSource.contains(it)
            }
        }
    }

    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun testPartMapTypeIsNotMapThrowsError(kspVersion: KSPVersion) {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.POST
                import io.github.kingg22.ktorgen.http.PartMap
                import io.github.kingg22.ktorgen.http.Multipart

                interface TestService {
                   @POST("posts")
                   @Multipart
                   suspend fun example(@PartMap() name: String)
                }
            """.trimIndent(),
        )

        runKtorGenProcessor(source, kspVersion = kspVersion) { result ->
            result.hasNoWarnings()
            result.hasErrorCount(1)
            result.hasErrorContaining(KtorGenLogger.PART_MAP_PARAMETER_TYPE_MUST_BE_MAP_PAIR_STRING.trim())
        }
    }

    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun testPartNullableThrowsError(kspVersion: KSPVersion) {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.POST
                import io.github.kingg22.ktorgen.http.Part
                import io.github.kingg22.ktorgen.http.Multipart

                interface TestService {
                   @POST("posts")
                   @Multipart
                   suspend fun example(@Part name: String?)
                }
            """.trimIndent(),
        )

        val expectedCode = listOf(
            MULTIPART_FORM_DATA,
            PART_DATA_LIST,
            SET_BODY_MULTIPART_FORM_DATA,
            "name?.let {",
            """append("name", """ stringTemplate $$"$it",
        )

        runKtorGenProcessor(source, kspVersion = kspVersion) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val actualSource = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            expectedCode.forEach {
                actualSource.contains(it)
            }
        }
    }

    companion object {
        const val MULTIPART_FORM_DATA = "val _multiPartDataContent = formData {"
        const val PART_DATA_LIST = "val _partDataList = mutableListOf<PartData>()"
        const val SET_BODY_MULTIPART_FORM_DATA =
            "this.setBody(MultiPartFormDataContent(_partDataList + _multiPartDataContent))"
    }
}
