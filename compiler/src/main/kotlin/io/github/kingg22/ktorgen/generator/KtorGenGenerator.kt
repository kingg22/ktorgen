package io.github.kingg22.ktorgen.generator

import com.google.devtools.ksp.processing.CodeGenerator
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ksp.writeTo
import io.github.kingg22.ktorgen.DiagnosticSender
import io.github.kingg22.ktorgen.model.ClassData

fun interface KtorGenGenerator {
    fun generate(classData: ClassData, timer: DiagnosticSender): List<FileSpec>

    companion object {
        /** Generate the Impl class using [KotlinpoetGenerator] of ksp */
        @JvmStatic
        fun generateKsp(classData: ClassData, codeGenerator: CodeGenerator, timer: DiagnosticSender) {
            DEFAULT.generate(classData, timer).forEach { it.writeTo(codeGenerator, false) }
        }

        @JvmStatic
        val DEFAULT: KtorGenGenerator
            get() = KotlinpoetGenerator()
    }
}
