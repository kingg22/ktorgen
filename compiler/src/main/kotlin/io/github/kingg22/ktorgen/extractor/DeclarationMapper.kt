package io.github.kingg22.ktorgen.extractor

import com.google.devtools.ksp.symbol.KSClassDeclaration
import io.github.kingg22.ktorgen.model.ClassData

fun interface DeclarationMapper {
    fun mapToModel(declaration: KSClassDeclaration): ClassData

    companion object {
        val DEFAULT = DeclarationMapper { declaration ->
            ClassData("", "", emptyList(), emptySet(), declaration.containingFile!!, emptySet())
        }
    }
}
