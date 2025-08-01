package io.github.kingg22.ktorgen.extractor

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSValueParameter
import io.github.kingg22.ktorgen.model.ParameterData

fun interface DeclarationParameterMapper {
    /** Convert a [KSValueParameter] to [ParameterData] */
    fun mapToModel(declaration: KSValueParameter): ParameterData

    companion object {
        val DEFAULT: DeclarationParameterMapper by lazy { ParameterMapper() }

        /** Safe get and cast the properties of annotation */
        inline fun <reified T> KSAnnotation.getArgumentValueByName(name: String): T? = this.arguments.firstOrNull {
            it.name?.asString() == name && it.value != null && it.value is T
        }?.value as? T
    }
}
