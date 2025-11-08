package io.github.kingg22.ktorgen.extractor

import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSValueParameter
import io.github.kingg22.ktorgen.DiagnosticSender
import io.github.kingg22.ktorgen.model.ParameterData

fun interface DeclarationParameterMapper {
    /** Convert a [KSValueParameter] to [ParameterData] */
    context(timer: DiagnosticSender)
    fun mapToModel(declaration: KSValueParameter): ParameterDataOrDeferredSymbols

    fun getLoggerNameFor(declaration: KSValueParameter): String =
        "Parameter Mapper for [${declaration.name?.asString().orEmpty()}]"

    /**
     * A pair of [ParameterData] and a list of [deferred symbols][KSAnnotated].
     *
     * The list of deferred symbols is used to resolve symbols that are not available at the time of mapping.
     *
     * If the list is empty, the [ParameterData] is guaranteed to be non-null.
     * Otherwise, the [ParameterData] is guaranteed to be null.
     */
    typealias ParameterDataOrDeferredSymbols = Pair<ParameterData?, List<KSAnnotated>>

    companion object {
        @JvmStatic
        val DEFAULT: DeclarationParameterMapper get() = ParameterMapper()
    }
}
