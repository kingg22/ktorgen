package io.github.kingg22.ktorgen

/** This class tests the compatiblity of ktorgen with java sources, is not planned to bring full support or coverage */
class JavaSourceTest {
    // Ktorgen and KSP read and generate code correctly, but kotlinc doesn't compile because java symbols are not found
    @Ignore("Room compiler testing don't offer a method to compile java and kotlin at the same time")
    @Test
    fun testFlowReturnTypeInJavaCompileSuccess() {
        val source = Source.java(
            "TestService",
            """
                package com.example.api;

                import io.github.kingg22.ktorgen.core.KtorGen;
                import io.github.kingg22.ktorgen.http.GET;
                import io.github.kingg22.ktorgen.http.Query;
                import kotlinx.coroutines.flow.Flow;

                @KtorGen
                public interface TestService {
                    @GET("/posts")
                    Flow<String> test(@Query("name") String name);
                }
            """.trimIndent(),
        )

        runKtorGenProcessor(source) { result ->
            result.hasErrorCount(0)
            result.hasNoWarnings()
            if (result.compilationResult.toString().contains("CompilationResult (with javac)") ||
                result.compilationResult.toString().contains("CompilationResult (with kapt)")
            ) {
                return@runKtorGenProcessor
            }
            val generated = result.generatedSourceFileWithPath(TEST_SERVICE_IMPL_PATH)
            // non-suspend override and uses flow builder
            generated.contains("override fun test(): Flow<String>")
            generated.contains("flow {")
            generated.contains("httpClient.request")
            generated.contains(".body<String>()")
            generated.contains("this.emit(_result)")
        }
    }
}
