package io.github.kingg22.ktorgen.model.annotations

/** Annotation at a parameter */
sealed interface ParameterAnnotation {
    val isRepeatable: Boolean get() = false
    sealed interface WithMapOrPairType : ParameterAnnotation

    // -- url --

    /** See [io.github.kingg22.ktorgen.http.Url] */
    data object Url : ParameterAnnotation

    /** See [io.github.kingg22.ktorgen.http.Path] */
    data class Path(val value: String, val encoded: Boolean) : ParameterAnnotation

    /** See [io.github.kingg22.ktorgen.http.Query] */
    data class Query(val value: String, val encoded: Boolean) : ParameterAnnotation

    /** See [io.github.kingg22.ktorgen.http.QueryName] */
    data class QueryName(val encoded: Boolean) : ParameterAnnotation

    /** See [io.github.kingg22.ktorgen.http.QueryMap] */
    data class QueryMap(val encoded: Boolean) : WithMapOrPairType

    // -- header --

    /** See [io.github.kingg22.ktorgen.http.HeaderParam] */
    data class Header(val value: String) : ParameterAnnotation {
        override val isRepeatable = true
    }

    /** [io.github.kingg22.ktorgen.http.HeaderMap] */
    data object HeaderMap : WithMapOrPairType

    /** See [io.github.kingg22.ktorgen.http.Cookie] */
    data class Cookies(val value: List<CookieValues>) : ParameterAnnotation {
        override val isRepeatable = true
    }

    // -- body --

    /** See [io.github.kingg22.ktorgen.http.Body] */
    data object Body : ParameterAnnotation

    /** See [io.github.kingg22.ktorgen.http.Field] */
    data class Field(val value: String, val encoded: Boolean) : ParameterAnnotation

    /** See [io.github.kingg22.ktorgen.http.FieldMap] */
    data class FieldMap(val encoded: Boolean) : WithMapOrPairType

    /** See [io.github.kingg22.ktorgen.http.Part] */
    data class Part(val value: String, val encoding: String) : ParameterAnnotation

    /** See [io.github.kingg22.ktorgen.http.PartMap] */
    data class PartMap(val encoding: String) : WithMapOrPairType

    /** See [io.github.kingg22.ktorgen.http.Tag] */
    data class Tag(val value: String) : ParameterAnnotation
}
