package io.github.kingg22.ktorgen

import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.runKspProcessorTest
import kotlin.test.Test

class IntegrationTest {
    @Test
    fun getWithBodyThrowWarningsAsError() {
        val source = Source.kotlin(
            "foo.bar.MyGetWarnings.kt",
            """
                package foo.bar

                import io.github.kingg22.ktorgen.http.*

                data class IssueData(val title: String, val body: String)

                interface MyGetWarnings {
                    // ⚠️ @GET con @Body
                    @GET("users/filter")
                    suspend fun getUsersWithBody(@Body filter: IssueData): List<Any>
                }
            """.trimIndent(),
        )

        runKspProcessorTest(
            sources = listOf(source),
            options = mapOf("ktorgen_check_type" to "1"),
            symbolProcessorProviders = listOf(KtorGenSymbolProcessorProvider()),
        ) { compilationResultSubject ->
            compilationResultSubject.hasError()
            compilationResultSubject.hasErrorCount(1)
        }
    }
}
