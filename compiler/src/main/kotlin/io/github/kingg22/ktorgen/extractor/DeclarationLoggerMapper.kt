package io.github.kingg22.ktorgen.extractor

import com.google.devtools.ksp.symbol.KSDeclaration

@Suppress("kotlin:S6517") // Function is not abstract
interface DeclarationLoggerMapper {
    fun getLoggerNameFor(declaration: KSDeclaration): String = "Mapper for ${declaration.simpleName.asString()}"
}
