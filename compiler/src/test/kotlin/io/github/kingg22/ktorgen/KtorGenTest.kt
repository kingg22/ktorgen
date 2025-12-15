package io.github.kingg22.ktorgen

import io.github.kingg22.ktorgen.KSPVersion.KSP1
import io.github.kingg22.ktorgen.KSPVersion.KSP2
import io.github.kingg22.ktorgen.model.KTORG_GENERATED_FILE_COMMENT
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

/** Test class only for [@KtorGen][io.github.kingg22.ktorgen.core.KtorGen] */
class KtorGenTest {
    // -- name --
    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun defaultNameIsGenerated(kspVersion: KSPVersion) {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                @io.github.kingg22.ktorgen.core.KtorGen
                interface TestService
            """.trimIndent(),
        )

        val expectedNames = listOf("public class _TestServiceImpl", "public fun TestService")

        runKtorGenProcessor(source, kspVersion = kspVersion) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generated = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            expectedNames.forEach { line ->
                generated.contains(line)
            }
        }
    }

    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun customNameOptionIsGenerated(kspVersion: KSPVersion) {
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

        // the option name is only for custom class name, the function always has the name of the interface
        // for customize functions, do manually!
        val expectedNames = listOf("public class HelloWorldApi", "public fun TestService")
        val nonExpectedNames = listOf("public class _TestServiceImpl")

        runKtorGenProcessor(source, kspVersion = kspVersion) { compilationResultSubject ->
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
    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun emptyInterfaceGeneratesNoOpFileWithoutErrors(kspVersion: KSPVersion) {
        val source = Source.kotlin(
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

        runKtorGenProcessor(source, kspVersion = kspVersion) { compilationResultSubject ->
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

    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun testNoGenerateOptionSkipValidationAndGeneration(kspVersion: KSPVersion) {
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

        runKtorGenProcessor(source, kspVersion = kspVersion) {
            it.hasNoWarnings()
            it.hasErrorCount(0)
            assertNull(it.findGeneratedSource(TEST_SERVICE_IMPL_PATH))
        }
    }

    // -- base path --
    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun basePathIsAppendedToFunctions(kspVersion: KSPVersion) {
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

        runKtorGenProcessor(source, kspVersion = kspVersion) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generated = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            generated.contains(expectedText)
        }
    }

    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun emptyBasePathDontAppendAnything(kspVersion: KSPVersion) {
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

        runKtorGenProcessor(source, kspVersion = kspVersion) { compilationResultSubject ->
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
    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun templateBasePathRequiredValueNotProvidedThrowsError(kspVersion: KSPVersion) {
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

        runKtorGenProcessor(source, kspVersion = kspVersion) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(1)
            compilationResultSubject.hasErrorContaining(KtorGenLogger.MISSING_PATH_VALUE)
        }
    }

    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun invalidSyntaxOfPathThrowsWarningWithWarningCheckType(kspVersion: KSPVersion) {
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
            kspVersion = kspVersion,
            processorOptions = mapOf(
                KtorGenOptions.STRICK_CHECK_TYPE to KtorGenOptions.ErrorsLoggingType.Warnings.intValue.toString(),
            ),
        ) { compilationResultSubject ->
            compilationResultSubject.hasErrorCount(0)
            compilationResultSubject.hasWarningCount(1)
            compilationResultSubject.hasWarningContaining(KtorGenLogger.DOUBLE_SLASH_IN_URL_PATH)
            val result = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            result.contains("this.takeFrom(".stringTemplate("/api//user"))
        }
    }

    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun validSyntaxOfPathSplitWithBaseAndMethodPass(kspVersion: KSPVersion) {
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
            kspVersion = kspVersion,
            processorOptions = mapOf(
                KtorGenOptions.STRICK_CHECK_TYPE to KtorGenOptions.ErrorsLoggingType.Warnings.intValue.toString(),
            ),
        ) { compilationResultSubject ->
            compilationResultSubject.hasErrorCount(0)
            compilationResultSubject.hasWarningCount(0)
            val result = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            result.contains("this.takeFrom(".stringTemplate("/api/v10/user"))
        }
    }

    // -- top level factory function --
    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun defaultFactoryFunctionIsGenerated(kspVersion: KSPVersion) {
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

        runKtorGenProcessor(source, kspVersion = kspVersion) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generated = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            generated.contains(expectedText)
        }
    }

    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun optionFactoryFunctionFalseDontGeneratedThat(kspVersion: KSPVersion) {
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

        runKtorGenProcessor(source, kspVersion = kspVersion) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generated = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            generated.contains(expectedText)
            generated.doesNotContain(nonExpectedText)
        }
    }

    // -- companion extension function --
    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun companionExtensionFunctionIsGenerated(kspVersion: KSPVersion) {
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

        runKtorGenProcessor(source, kspVersion = kspVersion) { compilationResultSubject ->
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
     * only generate new files, need to explicitly check exist a companion object before generate invalid code
     */
    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun optionCompanionExtensionFunctionTrueAndNotHaveCompanionObjectThrowsError(kspVersion: KSPVersion) {
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

        runKtorGenProcessor(source, kspVersion = kspVersion) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(1)
            compilationResultSubject.hasErrorContaining(KtorGenLogger.MISSING_COMPANION_TO_GENERATE.trim())
        }
    }

    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun optionCompanionExtensionFunctionFalseDontGeneratedThat(kspVersion: KSPVersion) {
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

        runKtorGenProcessor(source, kspVersion = kspVersion) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generated = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            generated.contains(expectedText)
            generated.doesNotContain(nonExpectedText)
        }
    }

    // -- http client extension function --
    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun optionHttpClientExtensionFunctionTrueGenerateThat(kspVersion: KSPVersion) {
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

        runKtorGenProcessor(source, kspVersion = kspVersion) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generated = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            expectedText.forEach { line ->
                generated.contains(line)
            }
        }
    }

    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun optionHttpClientExtFunctionFalseDontGeneratedThat(kspVersion: KSPVersion) {
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

        runKtorGenProcessor(source, kspVersion = kspVersion) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generated = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            generated.contains(expectedText)
            generated.doesNotContain(nonExpectedText)
        }
    }

    // -- propagate annotations --
    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun testOptInAnnotationIsPropagatedInClassGenerated(kspVersion: KSPVersion) {
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

        runKtorGenProcessor(source, kspVersion = kspVersion) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            val generatedFile = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            generatedFile.contains(expectedHeadersArgumentText)
            compilationResultSubject.hasErrorCount(0)
        }
    }

    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun testRequiredOptInAnnotationIsPropagatedInClassGeneratedWithoutOptInAnnotation(kspVersion: KSPVersion) {
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

        runKtorGenProcessor(source, kspVersion = kspVersion) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generatedFile = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            generatedFile.contains(expectedHeadersArgumentText)
            generatedFile.doesNotContain(noExpectedHeadersArgumentText)
        }
    }

    // -- annotations --
    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun optionAnnotationsGenerateCodeHaveThose(kspVersion: KSPVersion) {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGen

                @Target(AnnotationTarget.CLASS)
                annotation class TestAnnotation

                // an empty target means all targets, an annotation option respects it
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

        runKtorGenProcessor(source, kspVersion = kspVersion) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generatedFile = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            expectedText.forEach { line ->
                generatedFile.contains(line)
            }
        }
    }

    // -- optIn annotation --
    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun testRequiredOptInAnnotationIsPropagatedInClassGeneratedWithOptInOption(kspVersion: KSPVersion) {
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

        val expectedOptIn = "@OptIn(ExperimentalCompilerApi::class)"
        val directAnnotation = "@ExperimentalCompilerApi"

        runKtorGenProcessor(source, kspVersion = kspVersion) { result ->
            when (kspVersion) {
                KSP1 -> {
                    // Kotlin 1.9 no permite pasar una anotación de tipo requiresOptIn como parametro de otra anotación
                    result.hasErrorCount(1)
                    result.hasErrorContaining(
                        "This class can only be used as an annotation or as an argument to @OptIn",
                    )
                }

                KSP2 -> {
                    // Kotlin 2.x + KSP2 debería compilar bien
                    result.hasNoWarnings()
                    result.hasErrorCount(0)
                    val generatedFile = result.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
                    generatedFile.contains(expectedOptIn)
                    generatedFile.doesNotContain(directAnnotation)
                }
            }
        }
    }

    // -- function annotations --
    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun optionFunctionAnnotationsIsGenerated(kspVersion: KSPVersion) {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGen

                @Target(AnnotationTarget.FUNCTION)
                annotation class TestAnnotation

                // an empty target means all targets, an annotation option respects it
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

        runKtorGenProcessor(source, kspVersion = kspVersion) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generatedFile = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            expectedText.forEach { line ->
                generatedFile.contains(line)
            }
        }
    }

    // -- visibility modifier --
    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun optionVisibilityModifierAllGeneratedCodeHaveIt(kspVersion: KSPVersion) {
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

        runKtorGenProcessor(source, kspVersion = kspVersion) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generated = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            expectedVisibility.forEach { line ->
                generated.contains(line)
            }
        }
    }

    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun optionVisibilityModifierInvalidValueThrowsError(kspVersion: KSPVersion) {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGen

                @KtorGen(visibilityModifier = "aksjdkajsdkjaskfh")
                interface TestService
            """.trimIndent(),
        )

        runKtorGenProcessor(source, kspVersion = kspVersion) {
            it.hasErrorCount(1)
            it.hasNoWarnings()
            it.hasErrorContaining(KtorGenLogger.INVALID_VISIBILITY_MODIFIER.trim())
        }
    }

    // -- class visibility modifier --
    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun optionClassVisibilityIsGenerated(kspVersion: KSPVersion) {
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

        runKtorGenProcessor(source, kspVersion = kspVersion) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generated = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            expectedVisibility.forEach { line ->
                generated.contains(line)
            }
        }
    }

    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun privateClassVisibilityWithoutFunctionThrowsError(kspVersion: KSPVersion) {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGen

                @KtorGen(classVisibilityModifier = "PRIVATE", generateTopLevelFunction = false)
                interface TestService
            """.trimIndent(),
        )

        runKtorGenProcessor(source, kspVersion = kspVersion) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(1)
            compilationResultSubject.hasErrorContaining(KtorGenLogger.PRIVATE_CLASS_NO_ACCESS.trim())
        }
    }

    // -- constructor visibility modifier --
    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun optionConstructorVisibilityModifierIsGenerated(kspVersion: KSPVersion) {
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

        runKtorGenProcessor(source, kspVersion = kspVersion) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generated = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            expectedVisibility.forEach { line ->
                generated.contains(line)
            }
        }
    }

    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun privateConstructorVisibilityThrowsError(kspVersion: KSPVersion) {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGen

                @KtorGen(constructorVisibilityModifier = "private")
                interface TestService
            """.trimIndent(),
        )

        runKtorGenProcessor(source, kspVersion = kspVersion) {
            it.hasErrorCount(1)
            it.hasNoWarnings()
            it.hasErrorContaining(KtorGenLogger.PRIVATE_CONSTRUCTOR.trim())
        }
    }

    // -- function visibility modifier --
    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun optionFunctionVisibilityModifierIsGenerated(kspVersion: KSPVersion) {
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

        runKtorGenProcessor(source, kspVersion = kspVersion) { compilationResultSubject ->
            compilationResultSubject.hasNoWarnings()
            compilationResultSubject.hasErrorCount(0)
            val generated = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            expectedVisibility.forEach { line ->
                generated.contains(line)
            }
        }
    }

    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun privateFunctionVisibilityThrowsError(kspVersion: KSPVersion) {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGen

                @KtorGen(functionVisibilityModifier = "private")
                interface TestService
            """.trimIndent(),
        )

        runKtorGenProcessor(source, kspVersion = kspVersion) {
            it.hasErrorCount(1)
            it.hasNoWarnings()
            it.hasErrorContaining(KtorGenLogger.PRIVATE_FUNCTION.trim())
        }
    }

    // -- custom File Header --
    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun optionCustomFileHeaderIsGenerated(kspVersion: KSPVersion) {
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

        runKtorGenProcessor(source, kspVersion = kspVersion) { compilationResultSubject ->
            compilationResultSubject.hasErrorCount(0)
            compilationResultSubject.hasNoWarnings()
            val generated = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            generated.contains(expectedText)
            generated.doesNotContain(nonExpectedText)
        }
    }

    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun defaultCustomFileHeaderIsGenerated(kspVersion: KSPVersion) {
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

        runKtorGenProcessor(source, kspVersion = kspVersion) { compilationResultSubject ->
            compilationResultSubject.hasErrorCount(0)
            compilationResultSubject.hasNoWarnings()
            val generated = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            generated.contains(expectedText)
        }
    }

    // -- custom class header --
    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun optionCustomClassHeaderIsGenerated(kspVersion: KSPVersion) {
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

        runKtorGenProcessor(source, kspVersion = kspVersion) { compilationResultSubject ->
            compilationResultSubject.hasErrorCount(0)
            compilationResultSubject.hasNoWarnings()
            val generated = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            generated.contains(expectedHeader)
            generated.doesNotContain(nonExpectedHeader)
        }
    }

    @ParameterizedTest
    @EnumSource(KSPVersion::class)
    fun defaultCustomClassHeaderIsEmpty(kspVersion: KSPVersion) {
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

        runKtorGenProcessor(source, kspVersion = kspVersion) { compilationResultSubject ->
            compilationResultSubject.hasErrorCount(0)
            compilationResultSubject.hasNoWarnings()
            val generated = compilationResultSubject.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            generated.contains(expectedHeader)
        }
    }
}
