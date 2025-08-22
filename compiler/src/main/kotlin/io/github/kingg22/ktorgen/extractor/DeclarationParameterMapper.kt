package io.github.kingg22.ktorgen.extractor

import com.google.devtools.ksp.symbol.KSValueParameter
import io.github.kingg22.ktorgen.DiagnosticSender
import io.github.kingg22.ktorgen.model.ParameterData

fun interface DeclarationParameterMapper {
    /** Convert a [KSValueParameter] to [ParameterData] */
    fun mapToModel(declaration: KSValueParameter, timer: (String) -> DiagnosticSender): ParameterData

    companion object {
        val DEFAULT: DeclarationParameterMapper by lazy { ParameterMapper() }
    }
}
