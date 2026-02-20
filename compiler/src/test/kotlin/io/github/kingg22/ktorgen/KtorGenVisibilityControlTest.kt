package io.github.kingg22.ktorgen

class KtorGenVisibilityControlTest {
    // -- visibility modifier --
    @Test
    fun optionVisibilityModifierAllGeneratedCodeHaveIt() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGenExperimental
                import io.github.kingg22.ktorgen.core.KtorGenTopLevelFactory
                import io.github.kingg22.ktorgen.core.KtorGenVisibility
                import io.github.kingg22.ktorgen.core.KtorGenVisibilityControl

                @OptIn(KtorGenExperimental::class)
                @KtorGenVisibilityControl(visibilityModifier = KtorGenVisibility.INTERNAL)
                @KtorGenTopLevelFactory
                interface TestService
            """.trimIndent(),
        )

        val expectedVisibility = listOf(
            "internal class TestServiceImpl",
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

    // -- class visibility modifier --
    @Test
    fun optionClassVisibilityIsGenerated() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGenExperimental
                import io.github.kingg22.ktorgen.core.KtorGenTopLevelFactory
                import io.github.kingg22.ktorgen.core.KtorGenVisibility
                import io.github.kingg22.ktorgen.core.KtorGenVisibilityControl

                @OptIn(KtorGenExperimental::class)
                @KtorGenVisibilityControl(classVisibilityModifier = KtorGenVisibility.PRIVATE)
                @KtorGenTopLevelFactory
                interface TestService
            """.trimIndent(),
        )

        val expectedVisibility = listOf(
            "private class TestServiceImpl",
            "public fun TestService(",
            "public constructor", // can't be private because the function needs the instance it
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

                import io.github.kingg22.ktorgen.core.KtorGenExperimental
                import io.github.kingg22.ktorgen.core.KtorGenVisibility
                import io.github.kingg22.ktorgen.core.KtorGenVisibilityControl

                @OptIn(KtorGenExperimental::class)
                @KtorGenVisibilityControl(classVisibilityModifier = KtorGenVisibility.PRIVATE)
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

                import io.github.kingg22.ktorgen.core.KtorGenExperimental
                import io.github.kingg22.ktorgen.core.KtorGenTopLevelFactory
                import io.github.kingg22.ktorgen.core.KtorGenVisibility
                import io.github.kingg22.ktorgen.core.KtorGenVisibilityControl

                @OptIn(KtorGenExperimental::class)
                @KtorGenVisibilityControl(constructorVisibilityModifier = KtorGenVisibility.INTERNAL)
                @KtorGenTopLevelFactory
                interface TestService
            """.trimIndent(),
        )

        val expectedVisibility = listOf(
            "public class TestServiceImpl",
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

                import io.github.kingg22.ktorgen.core.KtorGenExperimental
                import io.github.kingg22.ktorgen.core.KtorGenVisibility
                import io.github.kingg22.ktorgen.core.KtorGenVisibilityControl

                @OptIn(KtorGenExperimental::class)
                @KtorGenVisibilityControl(constructorVisibilityModifier = KtorGenVisibility.PRIVATE)
                interface TestService
            """.trimIndent(),
        )

        runKtorGenProcessor(source) { compilation ->
            compilation.hasErrorCount(1)
            compilation.hasNoWarnings()
            compilation.hasErrorContaining(KtorGenLogger.PRIVATE_CONSTRUCTOR.trim())
        }
    }

    // -- function visibility modifier --
    @Test
    fun optionFunctionVisibilityModifierIsGenerated() {
        val source = Source.kotlin(
            "Source.kt",
            """
                package com.example.api

                import io.github.kingg22.ktorgen.core.KtorGenExperimental
                import io.github.kingg22.ktorgen.core.KtorGenTopLevelFactory
                import io.github.kingg22.ktorgen.core.KtorGenVisibility
                import io.github.kingg22.ktorgen.core.KtorGenVisibilityControl

                @OptIn(KtorGenExperimental::class)
                @KtorGenVisibilityControl(factoryFunctionVisibilityModifier = KtorGenVisibility.INTERNAL)
                @KtorGenTopLevelFactory
                interface TestService
            """.trimIndent(),
        )

        val expectedVisibility = listOf(
            "public class TestServiceImpl",
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

                import io.github.kingg22.ktorgen.core.KtorGenExperimental
                import io.github.kingg22.ktorgen.core.KtorGenTopLevelFactory
                import io.github.kingg22.ktorgen.core.KtorGenVisibility
                import io.github.kingg22.ktorgen.core.KtorGenVisibilityControl

                @OptIn(KtorGenExperimental::class)
                @KtorGenVisibilityControl(factoryFunctionVisibilityModifier = KtorGenVisibility.PRIVATE)
                interface TestService
            """.trimIndent(),
        )

        runKtorGenProcessor(source) {
            it.hasErrorCount(1)
            it.hasNoWarnings()
            it.hasErrorContaining(KtorGenLogger.PRIVATE_FUNCTION.trim())
        }
    }
}
