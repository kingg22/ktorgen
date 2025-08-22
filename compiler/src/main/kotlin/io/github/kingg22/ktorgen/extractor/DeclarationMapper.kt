package io.github.kingg22.ktorgen.extractor

import com.google.devtools.ksp.symbol.KSClassDeclaration
import io.github.kingg22.ktorgen.DiagnosticSender
import io.github.kingg22.ktorgen.model.ClassData

fun interface DeclarationMapper {
    /** Convert a [KSClassDeclaration] to [ClassData] */
    fun mapToModel(declaration: KSClassDeclaration, timer: (String) -> DiagnosticSender): ClassData

    companion object {
        val DEFAULT: DeclarationMapper by lazy { ClassMapper() }
    }
}
