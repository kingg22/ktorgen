package io.github.kingg22.ktorgen.extractor

import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import io.github.kingg22.ktorgen.DiagnosticSender
import io.github.kingg22.ktorgen.model.ClassData

fun interface DeclarationMapper : DeclarationLoggerMapper {
    /** Convert a [KSClassDeclaration] to [ClassData] */
    context(timer: DiagnosticSender)
    fun mapToModel(
        declaration: KSClassDeclaration,
        expectFunctions: List<KSFunctionDeclaration>,
    ): Pair<ClassData?, List<KSAnnotated>>

    override fun getLoggerNameFor(declaration: KSDeclaration): String =
        "Class Mapper for [${declaration.simpleName.asString()}]"

    companion object {
        @JvmStatic
        val DEFAULT: DeclarationMapper by lazy { ClassMapper() }
    }
}
