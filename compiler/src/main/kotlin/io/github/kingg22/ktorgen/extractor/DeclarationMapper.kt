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
    ): ClassDataOrDeferredSymbols

    override fun getLoggerNameFor(declaration: KSDeclaration): String =
        "Class Mapper for [${declaration.simpleName.asString()}]"

    /**
     * A pair of [ClassData] and a list of [deferred symbols][KSAnnotated].
     *
     * The list of deferred symbols is used to resolve symbols that are not available at the time of mapping.
     *
     * If the list is empty, the [ClassData] is guaranteed to be non-null.
     * Otherwise, the [ClassData] is guaranteed to be null.
     */
    typealias ClassDataOrDeferredSymbols = Pair<ClassData?, List<KSAnnotated>>

    companion object {
        @JvmStatic
        val DEFAULT: DeclarationMapper get() = ClassMapper()
    }
}
