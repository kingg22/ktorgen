package io.github.kingg22.ktorgen.extractor

import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import io.github.kingg22.ktorgen.DiagnosticSender
import io.github.kingg22.ktorgen.model.FunctionData

fun interface DeclarationFunctionMapper : DeclarationLoggerMapper {
    /** Convert a [KSFunctionDeclaration] to [FunctionData] */
    context(timer: DiagnosticSender)
    fun mapToModel(
        declaration: KSFunctionDeclaration,
        onAddImport: (String) -> Unit,
        basePath: String,
    ): Pair<FunctionData?, List<KSAnnotated>>

    override fun getLoggerNameFor(declaration: KSDeclaration): String =
        "Function Mapper for [${declaration.simpleName.asString()}]"

    companion object {
        val DEFAULT: DeclarationFunctionMapper by lazy { FunctionMapper() }
    }
}
