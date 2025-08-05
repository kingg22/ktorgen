package io.github.kingg22.ktorgen.model.annotations

/** See values of [io.github.kingg22.ktorgen.http.Cookie] */
data class CookieValues(
    val name: String,
    val value: String,
    val isValueParameter: Boolean,
    val maxAge: Int,
    val expiresTimestamp: Long?,
    val domain: String?,
    val path: String?,
    val secure: Boolean,
    val httpOnly: Boolean,
    val extensions: Map<String, String?>,
)
