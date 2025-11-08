package io.github.kingg22.ktorgen.model.annotations

/** Annotation at a parameter */
sealed interface ParameterAnnotation {
    val isRepeatable: Boolean get() = false
    sealed interface WithMapOrPairType : ParameterAnnotation

    // -- url --

    /** See [io.github.kingg22.ktorgen.http.Url] */
    object Url : ParameterAnnotation

    /** See [io.github.kingg22.ktorgen.http.Path] */
    class Path(val value: String, val encoded: Boolean) : ParameterAnnotation

    /** See [io.github.kingg22.ktorgen.http.Query] */
    class Query(val value: String, val encoded: Boolean) : ParameterAnnotation

    /** See [io.github.kingg22.ktorgen.http.QueryName] */
    class QueryName(val encoded: Boolean) : ParameterAnnotation

    /** See [io.github.kingg22.ktorgen.http.QueryMap] */
    class QueryMap(val encoded: Boolean) : WithMapOrPairType

    // -- header --

    /** See [io.github.kingg22.ktorgen.http.HeaderParam] */
    class Header(val value: String) : ParameterAnnotation {
        override val isRepeatable = true
    }

    /** [io.github.kingg22.ktorgen.http.HeaderMap] */
    object HeaderMap : WithMapOrPairType

    /** See [io.github.kingg22.ktorgen.http.Cookie] */
    class Cookies(val value: List<CookieValues>) : ParameterAnnotation {
        override val isRepeatable = true
    }

    // -- body --

    /** See [io.github.kingg22.ktorgen.http.Body] */
    object Body : ParameterAnnotation

    /** See [io.github.kingg22.ktorgen.http.Field] */
    class Field(val value: String, val encoded: Boolean) : ParameterAnnotation

    /** See [io.github.kingg22.ktorgen.http.FieldMap] */
    class FieldMap(val encoded: Boolean) : WithMapOrPairType

    /** See [io.github.kingg22.ktorgen.http.Part] */
    class Part(val value: String, val encoding: String) : ParameterAnnotation

    /** See [io.github.kingg22.ktorgen.http.PartMap] */
    class PartMap(val encoding: String) : WithMapOrPairType

    /** See [io.github.kingg22.ktorgen.http.Tag] */
    class Tag(val value: String) : ParameterAnnotation
}
