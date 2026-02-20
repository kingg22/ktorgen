package io.github.kingg22.ktorgen

import io.github.kingg22.ktorgen.KtorGenOptions.Companion.strickCheckTypeToPair
import io.github.kingg22.ktorgen.KtorGenOptions.ErrorsLoggingType
import io.github.kingg22.ktorgen.model.KTORG_GENERATED_FILE_COMMENT

/** Test class only for [@KtorGen][io.github.kingg22.ktorgen.core.KtorGen] */
class KtorGenTest {
    // -- name --
    @Test
    fun defaultNameIsGenerated() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                @io.github.kingg22.ktorgen.core.KtorGen
                interface TestService
            """.trimIndent(),
        )

        val expectedNames = listOf("public class TestServiceImpl")

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generated = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            expectedNames.forEach { line ->
                generated.contains(line)
            }
        }
    }

    @Test
    fun customNameOptionIsGenerated() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGen
                import io.github.kingg22.ktorgen.http.GET

                @KtorGen(name = "HelloWorldApi")
                interface TestService {
                    @GET
                    suspend fun test(): String
                }
            """.trimIndent(),
        )

        val expectedNames = listOf("public class HelloWorldApi")
        val nonExpectedNames = listOf("public class _TestServiceImpl", "public class TestServiceImpl")

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generated = compilationResultSubject.generatedSourceFileWithPath(
                "com.example.api.HelloWorldApi".toRelativePath(),
            )
            assertNull(
                compilationResultSubject.findGeneratedSource(TEST_SERVICE_IMPL_PATH),
            )
            expectedNames.forEach { line ->
                generated.contains(line)
            }
            nonExpectedNames.forEach { line ->
                generated.doesNotContain(line)
            }
        }
    }

    // -- generate? --
    @Test
    fun emptyInterfaceGeneratesNoOpFileWithoutErrors() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGen

                @KtorGen
                interface TestService
            """.trimIndent(),
        )

        val expectedText = listOf("public class TestServiceImpl", ": TestService")
        val nonExpectedText = listOf("override", "public fun")

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasErrorCount(0)
            compilationResultSubject.hasNoWarnings()
            val generated = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            expectedText.forEach { line ->
                generated.contains(line)
            }
            nonExpectedText.forEach { line ->
                generated.doesNotContain(line)
            }
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

    // -- base path --
    @Test
    fun basePathIsAppendedToFunctions() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGen
                import io.github.kingg22.ktorgen.http.GET

                @KtorGen(basePath = "/api")
                interface TestService {
                    @GET
                    suspend fun test(): String
                }
            """.trimIndent(),
        )

        val expectedText = "this.takeFrom(".stringTemplate("/api") + ")"

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generated = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            generated.contains(expectedText)
        }
    }

    @Test
    fun emptyBasePathDontAppendAnything() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGen
                import io.github.kingg22.ktorgen.http.GET

                @KtorGen
                interface TestService {
                    @GET
                    suspend fun test(): String
                }
            """.trimIndent(),
        )

        val nonExpectedText = "this.takeFrom("

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generated = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            generated.doesNotContain(nonExpectedText)
        }
    }

    /**
     * In method like [io.github.kingg22.ktorgen.http.GET] you can put a template and complete it with parameters,
     * the same apply to [basePath][io.github.kingg22.ktorgen.core.KtorGen.basePath],
     * the method inheritance the full path and need be completed.
     */
    @Test
    fun templateBasePathRequiredValueNotProvidedThrowsError() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGen
                import io.github.kingg22.ktorgen.http.GET

                @KtorGen(basePath = "/api/{user}")
                interface TestService {
                    @GET
                    suspend fun test(): String
                }
            """.trimIndent(),
        )

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(1)
            compilationResultSubject.hasErrorContaining(KtorGenLogger.MISSING_PATH_VALUE)
        }
    }

    @Test
    fun invalidSyntaxOfPathThrowsWarningWithWarningCheckType() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGen
                import io.github.kingg22.ktorgen.http.GET

                @KtorGen(basePath = "/api/")
                interface TestService {
                    @GET("/user")
                    suspend fun test(): String
                }
            """.trimIndent(),
        )

        runKtorGenProcessor(
            source,
            processorOptions = mapOf(strickCheckTypeToPair(ErrorsLoggingType.Warnings)),
        ) { compilationResultSubject ->
            compilationResultSubject.hasErrorCount(0)
            compilationResultSubject.hasWarningCount(1)
            compilationResultSubject.hasWarningContaining(KtorGenLogger.DOUBLE_SLASH_IN_URL_PATH)
            val result = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            result.contains("this.takeFrom(".stringTemplate("/api//user"))
        }
    }

    @Test
    fun validSyntaxOfPathSplitWithBaseAndMethodPass() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGen
                import io.github.kingg22.ktorgen.http.GET

                @KtorGen(basePath = "/api/v1")
                interface TestService {
                    @GET("0/user")
                    suspend fun test(): String
                }
            """.trimIndent(),
        )

        runKtorGenProcessor(
            source,
            processorOptions = mapOf(strickCheckTypeToPair(ErrorsLoggingType.Warnings)),
        ) { compilationResultSubject ->
            compilationResultSubject.hasErrorCount(0)
            compilationResultSubject.hasWarningCount(0)
            val result = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            result.contains("this.takeFrom(".stringTemplate("/api/v10/user"))
        }
    }

    // -- custom File Header --
    @Test
    fun optionCustomFileHeaderIsGenerated() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGen

                @KtorGen(customFileHeader = "Hello, World!")
                interface TestService
            """.trimIndent(),
        )

        val expectedText = "// Hello, World!"
        val nonExpectedText = "// $KTORG_GENERATED_FILE_COMMENT"

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasErrorCount(0)
            compilationResultSubject.hasNoWarnings()
            val generated = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            generated.contains(expectedText)
            generated.doesNotContain(nonExpectedText)
        }
    }

    @Test
    fun defaultCustomFileHeaderIsGenerated() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGen

                @KtorGen
                interface TestService
            """.trimIndent(),
        )

        val expectedText = "// $KTORG_GENERATED_FILE_COMMENT"

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasErrorCount(0)
            compilationResultSubject.hasNoWarnings()
            val generated = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            generated.contains(expectedText)
        }
    }

    // -- custom class header --
    @Test
    fun optionCustomClassHeaderIsGenerated() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGen

                @KtorGen(customClassHeader = "Hello, World")
                interface TestService
            """.trimIndent(),
        )

        val expectedHeader = "* Hello, World"
        val nonExpectedHeader = "\n\n@Generated\npublic class"

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasErrorCount(0)
            compilationResultSubject.hasNoWarnings()
            val generated = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            generated.contains(expectedHeader)
            generated.doesNotContain(nonExpectedHeader)
        }
    }

    @Test
    fun defaultCustomClassHeaderIsEmpty() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGen

                @KtorGen
                interface TestService
            """.trimIndent(),
        )

        val expectedHeader = "\n@Generated\npublic class"

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasErrorCount(0)
            compilationResultSubject.hasNoWarnings()
            val generated = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            generated.contains(expectedHeader)
        }
    }
}
