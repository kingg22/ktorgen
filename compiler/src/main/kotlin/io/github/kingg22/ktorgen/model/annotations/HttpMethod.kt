package io.github.kingg22.ktorgen.model.annotations

/** Take of [ktor http](https://github.com/ktorio/ktor/blob/main/ktor-http/common/src/io/ktor/http/HttpMethod.kt) */
class HttpMethod(val value: String) {
    val supportsRequestBody: Boolean
        get() = this !in REQUESTS_WITHOUT_BODY
    val ktorMethodName = value.lowercase().replaceFirstChar { it.uppercaseChar() }

    override fun toString() = value

    companion object {
        @JvmField
        val Get: HttpMethod = HttpMethod("GET")

        @JvmField
        val Post: HttpMethod = HttpMethod("POST")

        @JvmField
        val Put: HttpMethod = HttpMethod("PUT")

        // https://tools.ietf.org/html/rfc5789
        @JvmField
        val Patch: HttpMethod = HttpMethod("PATCH")

        @JvmField
        val Delete: HttpMethod = HttpMethod("DELETE")

        @JvmField
        val Head: HttpMethod = HttpMethod("HEAD")

        @JvmField
        val Options: HttpMethod = HttpMethod("OPTIONS")

        // custom when the annotation is not present and is obtained in other way
        @JvmField
        val Absent: HttpMethod = HttpMethod("\u0000")

        @JvmField
        val ktorMethods = listOf(Get, Post, Put, Patch, Delete, Head, Options)

        @JvmField
        val REQUESTS_WITHOUT_BODY = setOf(
            Get,
            Head,
            Options,
            HttpMethod("TRACE"),
        )

        @JvmStatic
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
    }
}
