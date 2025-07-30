package io.github.kingg22.ktorgen.generator

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import io.github.kingg22.ktorgen.model.ClassData

fun interface KtorGenGenerator {
    fun generate(classData: ClassData): FileSpec

    companion object {
        val DEFAULT = KtorGenGenerator {
            FileSpec.builder(
                ClassName("io.github.kingg22.ktorgen.generated", "_EmptyFileImpl"),
            ).build()
        }
    }
}
