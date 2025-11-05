package io.github.kingg22.ktorgen.extractor

import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSValueParameter
import io.github.kingg22.ktorgen.DiagnosticSender
import io.github.kingg22.ktorgen.model.ParameterData

fun interface DeclarationParameterMapper {
    /** Convert a [KSValueParameter] to [ParameterData] */
    context(timer: DiagnosticSender)
    fun mapToModel(declaration: KSValueParameter): Pair<ParameterData?, List<KSAnnotated>>

    fun getLoggerNameFor(declaration: KSValueParameter): String =
        "Parameter Mapper for [${declaration.name?.asString().orEmpty()}]"

    companion object {
        val DEFAULT: DeclarationParameterMapper by lazy { ParameterMapper() }
    }
}
