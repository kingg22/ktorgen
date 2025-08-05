package io.github.kingg22.ktorgen.model.annotations

/** Annotation at a function */
sealed interface FunctionAnnotation {
    val isRepeatable: Boolean get() = false

    /** See [io.github.kingg22.ktorgen.core.KtorGenIgnore] */
    data object Ignore : FunctionAnnotation

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
    data class HttpMethodAnnotation(val path: String, val httpMethod: HttpMethod) : FunctionAnnotation

    /** See [io.github.kingg22.ktorgen.http.Header] */
    data class Headers(val value: List<Pair<String, String>>) : FunctionAnnotation {
        override val isRepeatable = true
    }

    /** See [io.github.kingg22.ktorgen.http.Cookie] */
    data class Cookies(val value: List<CookieValues>) : FunctionAnnotation {
        override val isRepeatable = true
    }

    /** See [io.github.kingg22.ktorgen.http.FormUrlEncoded] */
    data object FormUrlEncoded : FunctionAnnotation

    /** See [io.github.kingg22.ktorgen.http.Streaming] */
    data object Streaming : FunctionAnnotation

    /** See [io.github.kingg22.ktorgen.http.Body] */
    data object Multipart : FunctionAnnotation
}
