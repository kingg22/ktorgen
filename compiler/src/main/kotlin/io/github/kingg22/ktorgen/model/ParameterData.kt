package io.github.kingg22.ktorgen.model

import com.google.devtools.ksp.symbol.KSValueParameter
import com.squareup.kotlinpoet.AnnotationSpec
import io.github.kingg22.ktorgen.model.annotations.ParameterAnnotation

class ParameterData(
    val nameString: String,
    val typeData: TypeData,
    val ksValueParameter: KSValueParameter,
    val ktorgenAnnotations: List<ParameterAnnotation>,
    val nonKtorgenAnnotations: Set<AnnotationSpec>,
    val optInAnnotation: AnnotationSpec?,
    val isHttpRequestBuilderLambda: Boolean,
) {
    val isVararg
        inline get() = ksValueParameter.isVararg

    val isValidTakeFrom
        inline get() = typeData.typeName in VALID_HTTP_REQUEST_TAKE_FROM

    inline fun <reified T : ParameterAnnotation> findAnnotationOrNull() =
        ktorgenAnnotations.filterIsInstance<T>().firstOrNull()

    inline fun <reified T : ParameterAnnotation> findAnnotation() = ktorgenAnnotations.filterIsInstance<T>().first()

    inline fun <reified T : ParameterAnnotation> findAllAnnotations() = ktorgenAnnotations.filterIsInstance<T>()

    inline fun <reified T : ParameterAnnotation> hasAnnotation() = findAnnotationOrNull<T>() != null
}
