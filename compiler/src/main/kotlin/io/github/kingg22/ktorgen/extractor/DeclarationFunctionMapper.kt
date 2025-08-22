package io.github.kingg22.ktorgen.extractor

import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import io.github.kingg22.ktorgen.DiagnosticSender
import io.github.kingg22.ktorgen.model.FunctionData

fun interface DeclarationFunctionMapper {
    /** Convert a [KSFunctionDeclaration] to [FunctionData] */
    fun mapToModel(
        declaration: KSFunctionDeclaration,
        onAddImport: (String) -> Unit,
        basePath: String,
        timer: (String) -> DiagnosticSender,
    ): FunctionData

    companion object {
        val DEFAULT: DeclarationFunctionMapper by lazy { FunctionMapper() }
    }
}
