package io.github.kingg22.ktorgen.generator

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.squareup.kotlinpoet.ksp.writeTo
import io.github.kingg22.ktorgen.model.ClassData

/** Generate the Impl class for all interfaces valid */
class KtorGenKspGenerator {
    fun generate(classData: ClassData, codeGenerator: CodeGenerator) {
        KtorGenGenerator.DEFAULT
            .generate(classData)
            .writeTo(codeGenerator, Dependencies(false, classData.ksFile))
    }
}
