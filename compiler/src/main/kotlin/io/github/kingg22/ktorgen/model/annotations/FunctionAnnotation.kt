package io.github.kingg22.ktorgen.model.annotations

/** Annotation at a function */
sealed class FunctionAnnotation {
    data object Ignore : FunctionAnnotation()
    data class HttpMethodAnnotation(val path: String, val httpMethod: HttpMethod) : FunctionAnnotation()

    data class Headers(val value: Set<String>) : FunctionAnnotation()

    data object FormUrlEncoded : FunctionAnnotation()

    data object Streaming : FunctionAnnotation()

    data object Multipart : FunctionAnnotation()
}
