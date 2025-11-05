package io.github.kingg22.ktorgen.extractor

import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import io.github.kingg22.ktorgen.DiagnosticSender
import io.github.kingg22.ktorgen.model.ClassData

fun interface DeclarationMapper {
    /** Convert a [KSClassDeclaration] to [ClassData] */
    context(timer: DiagnosticSender)
    fun mapToModel(
        declaration: KSClassDeclaration,
        expectFunctions: List<KSFunctionDeclaration>,
    ): Pair<ClassData?, List<KSAnnotated>>

    fun getLoggerNameFor(declaration: KSClassDeclaration): String =
        "Class Mapper for [${getInterfaceNameOf(declaration)}]"

    fun getInterfaceNameOf(declaration: KSClassDeclaration): String = declaration.simpleName.asString()

    companion object {
        @JvmStatic
        val DEFAULT: DeclarationMapper by lazy { ClassMapper() }
    }
}
