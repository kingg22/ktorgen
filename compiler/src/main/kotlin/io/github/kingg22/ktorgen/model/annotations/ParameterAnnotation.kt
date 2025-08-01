package io.github.kingg22.ktorgen.model.annotations

/** Annotation at a parameter */
sealed class ParameterAnnotation {
    object Body : ParameterAnnotation()

    class Path(val value: String, val encoded: Boolean = false) : ParameterAnnotation()

    class Query(val value: String, val encoded: Boolean = false) : ParameterAnnotation()

    class QueryName(val encoded: Boolean = false) : ParameterAnnotation()

    class QueryMap(val encoded: Boolean = false) : ParameterAnnotation()

    class Header(val path: String) : ParameterAnnotation()

    object HeaderMap : ParameterAnnotation()

    object Url : ParameterAnnotation()

    class Field(val value: String, val encoded: Boolean = false) : ParameterAnnotation()

    class FieldMap(val encoded: Boolean) : ParameterAnnotation()

    class Part(val value: String = "", val encoding: String = "binary") : ParameterAnnotation()

    class PartMap(val encoding: String = "binary") : ParameterAnnotation()

    class Tag(val value: String) : ParameterAnnotation()
}
