package io.github.kingg22.ktorgen.model

import com.google.devtools.ksp.symbol.KSValueParameter
import io.github.kingg22.ktorgen.model.annotations.ParameterAnnotation

class ParameterData(
    val nameString: String,
    val typeData: TypeData,
    val ksValueParameter: KSValueParameter,
    val ktorgenAnnotations: List<ParameterAnnotation>,
    val isHttpRequestBuilderLambda: Boolean,
) {
    val isVararg = ksValueParameter.isVararg

    // see https://api.ktor.io/ktor-client/ktor-client-core/io.ktor.client.request/-http-request-builder/index.html?query=class%20HttpRequestBuilder%20:%20HttpMessageBuilder#-1196439924%2FFunctions%2F-1897681819
    val isValidTakeFrom =
        typeData.typeName in setOf(HttpRequestBuilderTypeName, HttpRequestTypeName, HttpRequestDataTypeName)

    inline fun <reified T : ParameterAnnotation> findAnnotationOrNull() =
        ktorgenAnnotations.filterIsInstance<T>().firstOrNull()

    inline fun <reified T : ParameterAnnotation> findAllAnnotations() = ktorgenAnnotations.filterIsInstance<T>()

    inline fun <reified T : ParameterAnnotation> hasAnnotation() = findAnnotationOrNull<T>() != null
}
