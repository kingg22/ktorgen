package io.github.kingg22.ktorgen

import androidx.room.compiler.processing.util.Source
import io.github.kingg22.ktorgen.model.KTORG_GENERATED_FILE_COMMENT
import kotlin.test.Test
import kotlin.test.assertNull

/** Test class only for [@KtorGen][io.github.kingg22.ktorgen.core.KtorGen] */
class KtorGenTest {
    // -- name --
    @Test
    fun defaultNameIsGenerated() {
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

        val expectedNames = listOf("public class _TestServiceImpl", "public fun TestService")

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

        // the option name is only for custom class name, the function always have the name of the interface
        // for customize functions, do manually!
        val expectedNames = listOf("public class HelloWorldApi", "public fun TestService")
        val nonExpectedNames = listOf("public class _TestServiceImpl")

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generated = compilationResultSubject.generatedSourceFileWithPath(
                "com.example.api.HelloWorldApi".toRelativePath(),
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
        val api = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGen

                @KtorGen
                interface TestService
            """.trimIndent(),
        )

        val expectedText = listOf("public class _TestServiceImpl", ": TestService", "public fun TestService(")
        val nonExpectedText = listOf("override")

        runKtorGenProcessor(api) { compilationResultSubject ->
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
    fun invalidSyntaxOfPathThrowsError() {
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

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(1)
            compilationResultSubject.hasErrorContaining(KtorGenLogger.URL_SYNTAX_ERROR)
        }
    }

    // -- top level factory function --
    @Test
    fun defaultFactoryFunctionIsGenerated() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGen
                import io.github.kingg22.ktorgen.http.GET

                @KtorGen(basePath = "/api/")
                interface TestService {
                    @GET
                    suspend fun test(): String
                }
            """.trimIndent(),
        )

        val expectedText = "public fun TestService(httpClient: HttpClient): TestService = _TestServiceImpl(httpClient)"

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generated = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            generated.contains(expectedText)
        }
    }

    @Test
    fun optionFactoryFunctionFalseDontGeneratedThat() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGen
                import io.github.kingg22.ktorgen.http.GET

                @KtorGen(generateTopLevelFunction = false)
                interface TestService {
                    @GET
                    suspend fun test(): String
                }
            """.trimIndent(),
        )

        val nonExpectedText = "public fun TestService(httpClient: HttpClient)"
        val expectedText = "public class _TestServiceImpl"

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generated = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            generated.contains(expectedText)
            generated.doesNotContain(nonExpectedText)
        }
    }

    // -- companion extension function --
    @Test
    fun companionExtensionFunctionIsGenerated() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGen
                import io.github.kingg22.ktorgen.http.GET

                interface TestService {
                    @GET
                    suspend fun test(): String

                    @KtorGen(generateTopLevelFunction = true, generateCompanionExtFunction = true)
                    companion object
                }
            """.trimIndent(),
        )

        val expectedText = listOf(
            "public fun TestService(httpClient: HttpClient): TestService = _TestServiceImpl(httpClient)",
            "public fun TestService.Companion.create(httpClient: HttpClient): TestService = _TestServiceImpl(httpClient)",
            "public class _TestServiceImpl",
        )

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generated = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            expectedText.forEach { line ->
                generated.contains(line)
            }
        }
    }

    /**
     * A limitation of KSP is: can't alterate the source code,
     * only generate new files, need explicit check exist companion object before generate invalid code
     */
    @Test
    fun optionCompanionExtensionFunctionTrueAndNotHaveCompanionObjectThrowsError() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGen
                import io.github.kingg22.ktorgen.http.GET

                @KtorGen(generateTopLevelFunction = true, generateCompanionExtFunction = true)
                interface TestService {
                    @GET
                    suspend fun test(): String
                }
            """.trimIndent(),
        )

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(1)
            compilationResultSubject.hasErrorContaining(KtorGenLogger.MISSING_COMPANION_TO_GENERATE.trim())
        }
    }

    @Test
    fun optionCompanionExtensionFunctionFalseDontGeneratedThat() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGen
                import io.github.kingg22.ktorgen.http.GET

                @KtorGen(generateTopLevelFunction = false, generateCompanionExtFunction = false)
                interface TestService {
                    @GET
                    suspend fun test(): String
                }
            """.trimIndent(),
        )

        val nonExpectedText = "public fun TestService.Companion.create(httpClient: HttpClient)"
        val expectedText = "public class _TestServiceImpl"

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generated = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            generated.contains(expectedText)
            generated.doesNotContain(nonExpectedText)
        }
    }

    // -- http client extension function --
    @Test
    fun optionHttpClientExtensionFunctionTrueGenerateThat() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGen
                import io.github.kingg22.ktorgen.http.GET

                @KtorGen(
                    generateTopLevelFunction = true,
                    generateCompanionExtFunction = true,
                    generateHttpClientExtension = true,
                )
                interface TestService {
                    @GET
                    suspend fun test(): String

                    companion object
                }
            """.trimIndent(),
        )

        val expectedText = listOf(
            "public fun TestService(httpClient: HttpClient): TestService = _TestServiceImpl(httpClient)",
            "public fun TestService.Companion.create(httpClient: HttpClient): TestService = _TestServiceImpl(httpClient)",
            "public fun HttpClient.createTestService(): TestService = _TestServiceImpl(this)",
            "public class _TestServiceImpl",
        )

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generated = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            expectedText.forEach { line ->
                generated.contains(line)
            }
        }
    }

    @Test
    fun optionHttpClientExtFunctionFalseDontGeneratedThat() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGen
                import io.github.kingg22.ktorgen.http.GET

                @KtorGen(generateTopLevelFunction = false, generateHttpClientExtension = false)
                interface TestService {
                    @GET
                    suspend fun test(): String
                }
            """.trimIndent(),
        )

        val nonExpectedText = "public fun HttpClient.create"
        val expectedText = "public class _TestServiceImpl"

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generated = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            generated.contains(expectedText)
            generated.doesNotContain(nonExpectedText)
        }
    }

    // -- propagate annotations --
    @Test
    fun testOptInAnnotationIsPropagatedInClassGenerated() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.GET
                import io.github.kingg22.ktorgen.http.Header

                import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

                @OptIn(ExperimentalCompilerApi::class)
                interface TestService {
                    @Header("x", "y")
                    @GET("posts")
                    suspend fun test(): String
                }
            """.trimIndent(),
        )

        val expectedHeadersArgumentText = "@OptIn(ExperimentalCompilerApi::class)"

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            val generatedFile = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            generatedFile.contains(expectedHeadersArgumentText)
            compilationResultSubject.hasErrorCount(0)
        }
    }

    @Test
    fun testRequiredOptInAnnotationIsPropagatedInClassGeneratedWithoutOptInAnnotation() {
        val source = Source.kotlin(
            "Source2.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.http.GET
                import io.github.kingg22.ktorgen.http.Header

                import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

                @ExperimentalCompilerApi
                interface TestService {
                    @Header("x", "y")
                    @GET("posts")
                    suspend fun test(): String
                }
            """.trimIndent(),
        )

        val expectedHeadersArgumentText = "@ExperimentalCompilerApi"
        val noExpectedHeadersArgumentText = "@OptIn(ExperimentalCompilerApi::class)"

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generatedFile = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            generatedFile.contains(expectedHeadersArgumentText)
            generatedFile.doesNotContain(noExpectedHeadersArgumentText)
        }
    }

    // -- annotations --
    @Test
    fun optionAnnotationsGenerateCodeHaveThose() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGen

                @Target(AnnotationTarget.CLASS)
                annotation class TestAnnotation

                // empty target means all targets, annotation option respect it
                annotation class MyAnnotation

                @KtorGen(annotations = [TestAnnotation::class, MyAnnotation::class])
                interface TestService
            """.trimIndent(),
        )

        val expectedText = listOf(
            "@TestAnnotation\n@MyAnnotation\n@Generated\npublic class _TestServiceImpl",
            ": TestService",
            "\n@Generated\n@MyAnnotation\npublic fun TestService(",
        )

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generatedFile = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            expectedText.forEach { line ->
                generatedFile.contains(line)
            }
        }
    }

    // -- optIn annotation --
    @Test
    fun testRequiredOptInAnnotationIsPropagatedInClassGeneratedWithOptInOption() {
        val source = Source.kotlin(
            "Source2.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGen
                import io.github.kingg22.ktorgen.http.GET
                import io.github.kingg22.ktorgen.http.Header

                import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

                @KtorGen(optInAnnotations = [ExperimentalCompilerApi::class])
                @ExperimentalCompilerApi
                interface TestService {
                    @Header("x", "y")
                    @GET("posts")
                    suspend fun test(): String
                }
            """.trimIndent(),
        )

        val expectedHeadersArgumentText = "@OptIn(ExperimentalCompilerApi::class)"
        val noExpectedHeadersArgumentText = "@ExperimentalCompilerApi"

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generatedFile = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            generatedFile.contains(expectedHeadersArgumentText)
            generatedFile.doesNotContain(noExpectedHeadersArgumentText)
        }
    }

    // -- function annotations --
    @Test
    fun optionFunctionAnnotationsIsGenerated() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGen

                @Target(AnnotationTarget.FUNCTION)
                annotation class TestAnnotation

                // empty target means all targets, annotation option respect it
                annotation class MyAnnotation

                @KtorGen(functionAnnotations = [TestAnnotation::class, MyAnnotation::class, kotlin.jvm.JvmSynthetic::class])
                interface TestService
            """.trimIndent(),
        )

        val expectedText = listOf(
            "\n@Generated\npublic class _TestServiceImpl",
            ": TestService",
            "\n@Generated\n@TestAnnotation\n@MyAnnotation\n@JvmSynthetic\npublic fun TestService(",
        )

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generatedFile = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            expectedText.forEach { line ->
                generatedFile.contains(line)
            }
        }
    }

    // -- visibility modifier --
    @Test
    fun optionVisibilityModifierAllGeneratedCodeHaveIt() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGen

                @KtorGen(visibilityModifier = "internal")
                interface TestService
            """.trimIndent(),
        )

        val expectedVisibility = listOf(
            "internal class _TestServiceImpl",
            "internal fun TestService(",
            "internal constructor",
        )

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generated = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            expectedVisibility.forEach { line ->
                generated.contains(line)
            }
        }
    }

    @Test
    fun optionVisibilityModifierInvalidValueThrowsError() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGen

                @KtorGen(visibilityModifier = "aksjdkajsdkjaskfh")
                interface TestService
            """.trimIndent(),
        )

        runKtorGenProcessor(source) {
            it.hasErrorCount(1)
            it.hasNoWarnings()
            it.hasErrorContaining(KtorGenLogger.INVALID_VISIBILITY_MODIFIER.trim())
        }
    }

    // -- class visibility modifier --
    @Test
    fun optionClassVisibilityIsGenerated() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGen

                @KtorGen(classVisibilityModifier = "PRIVATE")
                interface TestService
            """.trimIndent(),
        )

        val expectedVisibility = listOf(
            "private class _TestServiceImpl",
            "public fun TestService(",
            "public constructor", // can't be private because the function need instance it
        )

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generated = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            expectedVisibility.forEach { line ->
                generated.contains(line)
            }
        }
    }

    @Test
    fun privateClassVisibilityWithoutFunctionThrowsError() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGen

                @KtorGen(classVisibilityModifier = "PRIVATE", generateTopLevelFunction = false)
                interface TestService
            """.trimIndent(),
        )

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(1)
            compilationResultSubject.hasErrorContaining(KtorGenLogger.PRIVATE_CLASS_NO_ACCESS.trim())
        }
    }

    // -- constructor visibility modifier --
    @Test
    fun optionConstructorVisibilityModifierIsGenerated() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGen

                @KtorGen(constructorVisibilityModifier = "internal")
                interface TestService
            """.trimIndent(),
        )

        val expectedVisibility = listOf(
            "public class _TestServiceImpl",
            "public fun TestService(",
            "internal constructor",
        )

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generated = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            expectedVisibility.forEach { line ->
                generated.contains(line)
            }
        }
    }

    @Test
    fun privateConstructorVisibilityThrowsError() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGen

                @KtorGen(constructorVisibilityModifier = "private")
                interface TestService
            """.trimIndent(),
        )

        runKtorGenProcessor(source) {
            it.hasErrorCount(1)
            it.hasNoWarnings()
            it.hasErrorContaining(KtorGenLogger.PRIVATE_CONSTRUCTOR.trim())
        }
    }

    // -- function visibility modifier --
    @Test
    fun optionFunctionVisibilityModifierIsGenerated() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGen

                @KtorGen(functionVisibilityModifier = "INTERNAL")
                interface TestService
            """.trimIndent(),
        )

        val expectedVisibility = listOf(
            "public class _TestServiceImpl",
            "internal fun TestService(",
            "public constructor",
        )

        runKtorGenProcessor(source) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generated = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            expectedVisibility.forEach { line ->
                generated.contains(line)
            }
        }
    }

    @Test
    fun privateFunctionVisibilityThrowsError() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGen

                @KtorGen(functionVisibilityModifier = "private")
                interface TestService
            """.trimIndent(),
        )

        runKtorGenProcessor(source) {
            it.hasErrorCount(1)
            it.hasNoWarnings()
            it.hasErrorContaining(KtorGenLogger.PRIVATE_FUNCTION.trim())
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
