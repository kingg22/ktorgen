package io.github.kingg22.ktorgen.model

import com.google.devtools.ksp.symbol.KSValueParameter
import io.github.kingg22.ktorgen.model.annotations.ParameterAnnotation

class ParameterData(
    val nameString: String,
    val typeData: TypeData,
    val ksValueParameter: KSValueParameter,
    val ktorgenAnnotations: List<ParameterAnnotation> = emptyList(),
    val isHttpRequestBuilderLambda: Boolean = false,
    val isHttpRequestBuilder: Boolean = typeData.typeName == HttpRequestBuilderTypeName,
) {
    val isVararg: Boolean = ksValueParameter.isVararg

    inline fun <reified T : ParameterAnnotation> findAnnotationOrNull(): T? =
        this.ktorgenAnnotations.filterIsInstance<T>().firstOrNull()

    inline fun <reified T : ParameterAnnotation> hasAnnotation(): Boolean = this.findAnnotationOrNull<T>() != null
}
