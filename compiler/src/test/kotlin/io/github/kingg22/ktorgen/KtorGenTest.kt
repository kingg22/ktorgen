package io.github.kingg22.ktorgen

import androidx.room.compiler.processing.util.Source
import kotlin.test.Test
import kotlin.test.assertNull

/** Test class only for [@KtorGen][io.github.kingg22.ktorgen.core.KtorGen] */
class KtorGenTest {
    @Test
    fun emptyInterfaceGeneratesNoOpFileWithoutErrors() {
        val api = Source.kotlin(
            "foo.bar.EmptyApi.kt",
            """
                package foo.bar

                import io.github.kingg22.ktorgen.core.KtorGen

                @KtorGen
                interface EmptyApi
            """.trimIndent(),
        )

        runKtorGenProcessor(api) { compilationResultSubject ->
            compilationResultSubject.hasErrorCount(0)
        }
    }

    @Test
    fun testNoGenerateOptionSkipValidationAndGeneration() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGen

                @KtorGen(generate=false)
                interface TestService {
                    suspend fun hello(): String
                }
            """.trimIndent(),
        )

        runKtorGenProcessor(source) {
            it.hasNoWarnings()
            it.hasErrorCount(0)
            assertNull(it.findGeneratedSource(TEST_SERVICE_IMPL_PATH))
        }
    }
}
