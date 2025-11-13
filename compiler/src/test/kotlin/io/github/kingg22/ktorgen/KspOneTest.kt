package io.github.kingg22.ktorgen

import io.github.kingg22.ktorgen.KSPVersion.KSP1

/** Only to test `androidx.room:room-compiler-processing-testing` runs with ksp1 */
class KspOneTest {
    @Test
    fun testKspOne() {
        val detectionSource = Source.kotlin(
            "Detect.kt",
            """
                @Target(AnnotationTarget.CLASS)
                annotation class Dummy

                @Dummy
                value class Foo(val id: String)
            """.trimIndent(),
        )

        runKtorGenProcessor(detectionSource, kspVersion = KSP1) { result -> result.hasError() }
    }
}
