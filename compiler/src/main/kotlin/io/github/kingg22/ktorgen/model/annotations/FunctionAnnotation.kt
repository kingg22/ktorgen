package io.github.kingg22.ktorgen.model.annotations

/** Annotation at a function */
sealed class FunctionAnnotation {
    class HttpMethodAnnotation(val path: String, val httpMethod: HttpMethod) : FunctionAnnotation()

    class Headers(val value: List<String>) : FunctionAnnotation()

    object FormUrlEncoded : FunctionAnnotation()

    object Streaming : FunctionAnnotation()

    object Multipart : FunctionAnnotation()
}
