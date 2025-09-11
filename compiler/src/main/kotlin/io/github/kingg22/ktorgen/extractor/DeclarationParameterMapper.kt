package io.github.kingg22.ktorgen.extractor

import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSValueParameter
import io.github.kingg22.ktorgen.DiagnosticSender
import io.github.kingg22.ktorgen.model.ParameterData

fun interface DeclarationParameterMapper {
    /** Convert a [KSValueParameter] to [ParameterData] */
    fun mapToModel(
        declaration: KSValueParameter,
        timer: (String) -> DiagnosticSender,
    ): Pair<ParameterData?, List<KSAnnotated>>

    companion object {
        val DEFAULT: DeclarationParameterMapper by lazy { ParameterMapper() }
    }
}
