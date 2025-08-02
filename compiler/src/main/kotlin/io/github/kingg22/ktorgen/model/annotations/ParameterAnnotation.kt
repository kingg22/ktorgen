package io.github.kingg22.ktorgen.model.annotations

/** Annotation at a parameter */
sealed class ParameterAnnotation {
    data object Body : ParameterAnnotation()
    data class Path(val value: String, val encoded: Boolean) : ParameterAnnotation()
    data class Query(val value: String, val encoded: Boolean) : ParameterAnnotation()
    data class QueryName(val encoded: Boolean) : ParameterAnnotation()
    data class QueryMap(val encoded: Boolean) : ParameterAnnotation()
    data class Header(val value: String) : ParameterAnnotation()
    data object HeaderMap : ParameterAnnotation()
    data object Url : ParameterAnnotation()
    data class Field(val value: String, val encoded: Boolean) : ParameterAnnotation()
    data class FieldMap(val encoded: Boolean) : ParameterAnnotation()
    data class Part(val value: String, val encoding: String) : ParameterAnnotation()
    data class PartMap(val encoding: String) : ParameterAnnotation()
    data class Tag(val value: String) : ParameterAnnotation()
}
