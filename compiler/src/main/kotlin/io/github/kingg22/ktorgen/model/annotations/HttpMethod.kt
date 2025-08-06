package io.github.kingg22.ktorgen.model.annotations

/** Take of [ktor http](https://github.com/ktorio/ktor/blob/main/ktor-http/common/src/io/ktor/http/HttpMethod.kt) */
class HttpMethod(val value: String) {
    val supportsRequestBody: Boolean
        get() = this !in REQUESTS_WITHOUT_BODY

    override fun toString() = value

    companion object {
        val Get: HttpMethod = HttpMethod("GET")
        val Post: HttpMethod = HttpMethod("POST")
        val Put: HttpMethod = HttpMethod("PUT")

        // https://tools.ietf.org/html/rfc5789
        val Patch: HttpMethod = HttpMethod("PATCH")
        val Delete: HttpMethod = HttpMethod("DELETE")
        val Head: HttpMethod = HttpMethod("HEAD")
        val Options: HttpMethod = HttpMethod("OPTIONS")

        // custom when the annotation is not present and is obtained in other way
        val Absent: HttpMethod = HttpMethod("")

        fun parse(method: String): HttpMethod = when (method) {
            Get.value -> Get
            Post.value -> Post
            Put.value -> Put
            Patch.value -> Patch
            Delete.value -> Delete
            Head.value -> Head
            Options.value -> Options
            else -> HttpMethod(method)
        }

        val REQUESTS_WITHOUT_BODY = setOf(
            Get,
            Head,
            Options,
            HttpMethod("TRACE"),
        )
    }
}
