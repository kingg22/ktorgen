package io.github.kingg22.ktorgen.http

/**
 * Add a hash mark `#` introduces the optional fragment near the end of the URL.
 *
 * @see <a href="https://ktor.io/docs/client-requests.html#url-fragment">Ktor Client Request - Url fragment</a>
 * @see <a href="https://api.ktor.io/ktor-http/io.ktor.http/-u-r-l-builder/index.html#1393910007%2FProperties%2F-87403492">Ktor API - Url Builder fragment</a>
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/URI/Reference/Fragment">MDN - URI fragment</a>
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/URI/Reference/Fragment/Text_fragments">MDN - URI text fragments</a>
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
@MustBeDocumented
annotation class Fragment(
    /** A sequence of any characters. The resource itself defines the exact format of the fragment. */
    val value: String,
    /** Specifies whether the names and values are already URL encoded. */
    val encoded: Boolean = false,
)
