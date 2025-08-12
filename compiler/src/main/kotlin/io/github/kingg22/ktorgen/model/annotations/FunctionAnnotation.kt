package io.github.kingg22.ktorgen.model.annotations

/** Annotation at a function */
sealed interface FunctionAnnotation {
    val isRepeatable: Boolean get() = false

    /** See [io.github.kingg22.ktorgen.http.Fragment] */
    class Fragment(val value: String, val encoded: Boolean) : FunctionAnnotation

    /**
     * See
     * [io.github.kingg22.ktorgen.http.HTTP],
     * [io.github.kingg22.ktorgen.http.GET],
     * [io.github.kingg22.ktorgen.http.POST],
     * [io.github.kingg22.ktorgen.http.PUT],
     * [io.github.kingg22.ktorgen.http.DELETE],
     * [io.github.kingg22.ktorgen.http.HEAD],
     * [io.github.kingg22.ktorgen.http.OPTIONS],
     * [io.github.kingg22.ktorgen.http.PATCH],
     */
    class HttpMethodAnnotation(val path: String, val httpMethod: HttpMethod) : FunctionAnnotation

    /** See [io.github.kingg22.ktorgen.http.Header] */
    class Headers(val value: List<Pair<String, String>>) : FunctionAnnotation {
        override val isRepeatable = true
    }

    /** See [io.github.kingg22.ktorgen.http.Cookie] */
    class Cookies(val value: List<CookieValues>) : FunctionAnnotation {
        override val isRepeatable = true
    }

    /** See [io.github.kingg22.ktorgen.http.FormUrlEncoded] */
    object FormUrlEncoded : FunctionAnnotation

    /** See [io.github.kingg22.ktorgen.http.Body] */
    object Multipart : FunctionAnnotation
}
