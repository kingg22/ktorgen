package io.github.kingg22.ktorgen

import androidx.room.compiler.processing.util.Source
import kotlin.test.Test

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
}
