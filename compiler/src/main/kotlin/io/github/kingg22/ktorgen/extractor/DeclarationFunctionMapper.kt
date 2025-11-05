package io.github.kingg22.ktorgen.extractor

import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import io.github.kingg22.ktorgen.DiagnosticSender
import io.github.kingg22.ktorgen.model.FunctionData

fun interface DeclarationFunctionMapper : DeclarationLoggerMapper {
    /** Convert a [KSFunctionDeclaration] to [FunctionData] */
    context(timer: DiagnosticSender)
    fun mapToModel(declaration: KSFunctionDeclaration, basePath: String): FunctionDataOrDeferredSymbols

    override fun getLoggerNameFor(declaration: KSDeclaration): String =
        "Function Mapper for [${declaration.simpleName.asString()}]"

    /**
     * A pair of [FunctionData] and a list of [deferred symbols][KSAnnotated].
     *
     * The list of deferred symbols is used to resolve symbols that are not available at the time of mapping.
     *
     * If the list is empty, the [FunctionData] is guaranteed to be non-null.
     * Otherwise, the [FunctionData] is guaranteed to be null.
     */
    typealias FunctionDataOrDeferredSymbols = Pair<FunctionData?, List<KSAnnotated>>

    companion object {
        @JvmStatic
        val DEFAULT: DeclarationFunctionMapper by lazy { FunctionMapper() }
    }
}
