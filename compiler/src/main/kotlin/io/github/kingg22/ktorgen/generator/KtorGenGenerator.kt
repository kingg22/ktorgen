package io.github.kingg22.ktorgen.generator

import com.squareup.kotlinpoet.FileSpec
import io.github.kingg22.ktorgen.DiagnosticSender
import io.github.kingg22.ktorgen.model.ClassData

fun interface KtorGenGenerator {
    context(timer: DiagnosticSender)
    fun generate(classData: ClassData): List<FileSpec>

    companion object {
        @JvmStatic
        val DEFAULT: KtorGenGenerator by lazy { KotlinpoetGenerator() }
    }
}
