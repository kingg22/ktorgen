@file:Suppress("ktlint:standard:property-naming", "unused", "ConstPropertyName")

package io.github.kingg22.ktorgen.http

import io.github.kingg22.ktorgen.core.KTORGEN_DEFAULT_NAME

/**
 * Add a cookie to the request.
 *
 * **If the Cookie plugin is installed,
 * cookies added in the request are ignored.** Read Ktor docs for more information.
 *
 * This annotation is modeled from [HttpMessageBuilder.cookie](https://api.ktor.io/ktor-client/ktor-client-core/io.ktor.client.request/cookie.html),
 * but adapted to annotation restrictions:
 * - _null_ is not allowed → special values are used to represent absence:
 *
 *    `expiresTimestamp = -1L` → no expiration.
 *
 *    `domain = ""` and `path = ""` → no value assigned.
 *
 * - The `extensions` field is defined as [`Array<PairString>`][PairString] for added security and type-safety when mapping cookie extensions.
 *
 * Example usage:
 * ```kotlin
 * @Cookie(
 *     name = "session_id",
 *     value = "abc123", // required in function
 *     maxAge = 3600,
 *     expiresTimestamp = 1735689600000, // 01/01/2025 00:00:00 GMT
 *     secure = true,
 *     httpOnly = true,
 *     extensions = [Cookie.PairString("SameSite", "Strict")],
 * )
 * @GET
 * suspend fun myRequest(@Cookie("yummy_cookie") flavor: String): SecureResponse
 *
 * // Generated code
 * this.cookie(
 *    name = """session_id""",
 *    value: """abc123""",
 *    maxAge = 3_600,
 *    expires = GMTDate(timestamp = 1_735_689_600_000),
 *    domain = null, // empty string is null
 *    path = null, // empty string is null
 *    secure = true,
 *    httpOnly = true,
 *    extensions = mapOf("""SameSite""" to """Strict"""),
 * )
 * ```
 *
 * @property name Name of the cookie
 * @property value Value of the cookie (applied to **parameter**, is obtained of it, else is **function is required**)
 *
 * @see Cookie.PairString
 * @see Cookie.Companion
 * @see Cookie.SameSites
 * @see <a href="https://ktor.io/docs/client-requests.html#cookies">Ktor Client Request - Cookies</a>
 * @see <a href="https://api.ktor.io/ktor-http/io.ktor.http/-cookie/index.html">Ktor Cookie</a>
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Guides/Cookies">MDN Cookies</a>
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
@Repeatable
@MustBeDocumented
annotation class Cookie(
    val name: String,
    val value: String = KTORGEN_DEFAULT_NAME,

    /** Time to live in seconds (0 for a session cookie) */
    val maxAge: Int = 0,

    /** Timestamp in milliseconds for the expiration date, or -1 to indicate null */
    val expiresTimestamp: Long = -1,

    /** Domain to which the cookie applies, empty or blank if not applicable */
    val domain: String = "",

    /** Path to the cookie, empty or blank if not applicable */
    val path: String = "",

    /** Indicates whether the cookie should only be sent over HTTPS */
    val secure: Boolean = false,

    /** Indicates whether the cookie should not be accessible via JavaScript */
    val httpOnly: Boolean = false,

    /** List of extensions as key/value pairs */
    val extensions: Array<PairString> = [],
) {
    /**
     * Represent a pair key/value, equivalent a Map<String, String?>
     *
     * Sample: `PairString("SameSite", "Strict")`
     *
     * @param first key
     * @param second value if is empty or blank means null
     */
    @Target // only parameter of other annotation
    @Retention(AnnotationRetention.SOURCE)
    @MustBeDocumented
    annotation class PairString(val first: String, val second: String)

    /** Contain most used cookie **name** */
    companion object {
        // -------------------------
        // Session management
        // -------------------------
        const val SessionId = "session_id" // ID de sesión genérico
        const val SID = "SID" // ID de sesión corto, usado en varios sistemas
        const val Token = "token" // Token genérico
        const val AccessToken = "access_token" // Token de acceso OAuth/JWT
        const val RefreshToken = "refresh_token" // Token de refresco OAuth/JWT

        // -------------------------
        // Authentication
        // -------------------------
        const val Auth = "auth" // Auth genérico
        const val AuthToken = "auth_token" // Token de autenticación
        const val Bearer = "bearer" // Token tipo Bearer
        const val IdToken = "id_token" // Token de identidad (OpenID Connect)

        // -------------------------
        // CSRF / Anti-forgery
        // -------------------------
        const val Csrf = "csrf" // Token CSRF genérico
        const val CsrfToken = "csrf_token" // Variante explícita
        const val XSRF_TOKEN = "XSRF-TOKEN" // Variante usada en cabeceras y cookies

        // -------------------------
        // Localization / Preferences
        // -------------------------
        const val Lang = "lang" // Idioma del usuario
        const val Locale = "locale" // Configuración regional
        const val Theme = "theme" // Preferencia de tema (light/dark)

        // -------------------------
        // Tracking / State
        // -------------------------
        const val TrackId = "track_id" // Identificador de seguimiento
        const val ClientId = "client_id" // Identificador de cliente
        const val SessionTrack = "session_track" // Seguimiento de sesión

        // -------------------------
        // Security attributes (pseudo-cookie names)
        // -------------------------
        const val SameSite = "SameSite" // Valores: Strict, Lax, None
    }

    /** Values of Cookie with name `SameSite` */
    object SameSites {
        /*
         SameSite
            Qué hace: Controla cuándo se envía la cookie en solicitudes de otros sitios (cross-site requests).

            Formato: Uno de los valores:

            "Strict" → Solo en requests del mismo dominio.

            "Lax" → Permite en navegaciones normales, pero no en la mayoría de cross-site POSTs.

            "None" → Siempre se envía (requiere Secure en la mayoría de navegadores modernos).
         */

        const val Strict = "Strict"
        const val Lax = "Lax"
        const val None = "None"
    }
}
