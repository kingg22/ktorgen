package io.github.kingg22.ktorgen

/** Test related to the [@Field][io.github.kingg22.ktorgen.http.Field] annotation */
class FieldTest {
    @Test
    fun testNoFieldBuilderFoundWhenNoFieldAnnotationsFound() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.GET

                interface TestService {
                    @GET("posts")
                    suspend fun test(): Map<String,Int>
                }
            """.trimIndent(),
        )

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            val actualSource = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            actualSource.contains("HttpMethod.Get")
            actualSource.doesNotContain("FormDataContent")
            actualSource.doesNotContain("Parameters.build")
        }
    }

    @Test
    fun testFieldUsedWithoutFormEncodingThrowsError() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.GET
                import io.github.kingg22.ktorgen.http.Field

                interface TestService {
                    @GET("posts")
                    suspend fun test(@Field("name") testField: String)
                }
            """.trimIndent(),
        )

        runKtorGenProcessor(source) { result ->
            result.hasNoWarnings()
            // With strick check this warning is converted to error
            result.hasErrorCount(1)
            result.hasErrorContaining(KtorGenLogger.FORM_ENCODED_ANNOTATION_MISSING_FOUND_FIELD.trim())
        }
    }

    @Test
    fun testFieldAnnotationFoundAndAddItToFieldsBuilder() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.POST
                import io.github.kingg22.ktorgen.http.Field
                import io.github.kingg22.ktorgen.http.FormUrlEncoded

                interface TestService {
                    @FormUrlEncoded
                    @POST("posts")
                    suspend fun test(@Field("name") testField: String)
                }
            """.trimIndent(),
        )

        val expectedFieldsBuilderText = listOf(
            PARAMETERS_BUILD,
            "testField?.let {",
            PARAMETERS_APPEND_NAME_IT,
            SET_BODY_FORM_DATA,
        )

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            val actualSource = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            for (expectedLine in expectedFieldsBuilderText) {
                actualSource.contains(expectedLine)
            }
        }
    }

    @Test
    fun testFieldAnnotationWitDefaultValueFoundAndAddItToFieldsBuilder() {
        val source = Source.kotlin(
            "Source.kt",
            """
            package com.example.api

            import io.github.kingg22.ktorgen.http.POST
            import io.github.kingg22.ktorgen.http.Field
            import io.github.kingg22.ktorgen.http.FormUrlEncoded

            interface TestService {
                @FormUrlEncoded
                @POST("posts")
                suspend fun test(@Field name: String)
            }
            """.trimIndent(),
        )

        val expectedFieldsBuilderText = listOf(
            PARAMETERS_BUILD,
            "name?.let {",
            PARAMETERS_APPEND_NAME_IT,
            SET_BODY_FORM_DATA,
        )

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            val actualSource = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            for (expectedLine in expectedFieldsBuilderText) {
                actualSource.contains(expectedLine)
            }
        }
    }

    @Test
    fun testFieldAnnotationWithListFoundAndAddItToFieldsBuilder() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.POST
                import io.github.kingg22.ktorgen.http.Field
                import io.github.kingg22.ktorgen.http.FormUrlEncoded

                interface TestService {
                    @FormUrlEncoded
                    @POST("posts")
                    suspend fun test(@Field("name") testField: List<String>)
                }
            """.trimIndent(),
        )

        val expectedFieldsBuilderText = listOf(
            PARAMETERS_BUILD,
            "testField?.filterNotNull()?.forEach {",
            PARAMETERS_APPEND_NAME_IT,
            SET_BODY_FORM_DATA,
        )

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            val actualSource = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            for (expectedLine in expectedFieldsBuilderText) {
                actualSource.contains(expectedLine)
            }
        }
    }

    @Test
    fun testFieldAnnotationWithArrayFoundAndAddItToFieldsBuilder() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.POST
                import io.github.kingg22.ktorgen.http.Field
                import io.github.kingg22.ktorgen.http.FormUrlEncoded

                interface TestService {
                    @FormUrlEncoded
                    @POST("posts")
                    suspend fun test(@Field("name") testField: Array<String>)
                }
            """.trimIndent(),
        )

        val expectedFieldsBuilderText = listOf(
            PARAMETERS_BUILD,
            "testField?.filterNotNull()?.forEach {",
            PARAMETERS_APPEND_NAME_IT,
            SET_BODY_FORM_DATA,
        )

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            val actualSource = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            for (expectedLine in expectedFieldsBuilderText) {
                actualSource.contains(expectedLine)
            }
        }
    }

    @Test
    fun testFieldMapAnnotationFoundAndAddItToFieldsBuilder() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.FieldMap
                import io.github.kingg22.ktorgen.http.FormUrlEncoded
                import io.github.kingg22.ktorgen.http.POST

                interface TestService {
                    @FormUrlEncoded
                    @POST("posts")
                    suspend fun test(@FieldMap() testFieldMap: Map<String, String>)
                }
            """.trimIndent(),
        )

        val expectedFieldsBuilderText = listOf(
            PARAMETERS_BUILD,
            SET_BODY_FORM_DATA,
            "testFieldMap?.forEach {",
            "entry.value?.let { value ->",
            PARAMETERS_APPEND_ENTRY_KEY_VALUE,
        )

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            val actualSource = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            for (expectedLine in expectedFieldsBuilderText) {
                actualSource.contains(expectedLine)
            }
        }
    }

    @Test
    fun testFunctionWithFieldAndFieldMap() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.POST
                import io.github.kingg22.ktorgen.http.FieldMap
                import io.github.kingg22.ktorgen.http.Field
                import io.github.kingg22.ktorgen.http.FormUrlEncoded

                interface TestService {
                    @FormUrlEncoded
                    @POST("posts")
                    suspend fun example(@Field("name") testField: String, @FieldMap() name: Map<String, String>)
                }
            """.trimIndent(),
        )

        val expectedFieldsBuilderText = listOf(
            "testField?.let {",
            PARAMETERS_BUILD,
            PARAMETERS_APPEND_NAME_IT,
            "name?.forEach { entry ->",
            PARAMETERS_APPEND_ENTRY_KEY_VALUE,
            SET_BODY_FORM_DATA,
        )

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            val actualSource = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            for (expectedLine in expectedFieldsBuilderText) {
                actualSource.contains(expectedLine)
            }
        }
    }

    @Test
    fun testFieldMapTypeIsNotMapThrowsError() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.POST
                import io.github.kingg22.ktorgen.http.FieldMap
                import io.github.kingg22.ktorgen.http.FormUrlEncoded

                interface TestService {
                    @FormUrlEncoded
                    @POST("posts")
                    suspend fun example(@FieldMap name: String)
                }
            """.trimIndent(),
        )

        runKtorGenProcessor(source) { result ->
            result.hasNoWarnings()
            result.hasErrorCount(1)
            result.hasErrorContaining(KtorGenLogger.FIELD_MAP_PARAMETER_TYPE_MUST_BE_MAP_PAIR_STRING)
        }
    }

    @Test
    fun testFieldMapKeysIsNotStringThrowsError() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.POST
                import io.github.kingg22.ktorgen.http.FieldMap
                import io.github.kingg22.ktorgen.http.FormUrlEncoded

                interface TestService {
                    @FormUrlEncoded
                    @POST("posts")
                    suspend fun example(@FieldMap name: Map<Int,String>)
                }
            """.trimIndent(),
        )

        runKtorGenProcessor(source) { result ->
            result.hasNoWarnings()
            result.hasErrorCount(1)
            result.hasErrorContaining(KtorGenLogger.FIELD_MAP_PARAMETER_TYPE_MUST_BE_MAP_PAIR_STRING.trim())
        }
    }

    companion object {
        private const val SET_BODY_FORM_DATA = "this.setBody(FormDataContent(_formDataContent))"
        private const val PARAMETERS_BUILD = "= Parameters.build {"
        private val PARAMETERS_APPEND_NAME_IT = """this.append("name", """ stringTemplate $$"$it"
        private val PARAMETERS_APPEND_ENTRY_KEY_VALUE = "this.append(entry.key, " stringTemplate $$"$value"
    }
}
