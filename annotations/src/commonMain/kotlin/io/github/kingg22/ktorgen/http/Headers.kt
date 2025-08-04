@file:Suppress("ktlint:standard:property-naming", "unused", "ConstPropertyName")

package io.github.kingg22.ktorgen.http

/**
 * Add headers to a request
 *
 * Each header must be in the format: `"Header-Name: header-value"`
 *
 * ```kotlin
 * @Headers("Accept: application/json", "Content-Type: application/json")
 * @GET("comments")
 * suspend fun requestWithHeaders(): List<Comment>
 *
 * // type-safe
 * @Headers("${Headers.ContentType}: ${Headers.ContentTypes.Application.Json}")
 * @GET("comments/related")
 * suspend fun request(): List<Comment>
 * ```
 *
 * By default, Headers do not overwrite each other:
 * all headers with the same name will be included in the request.
 * Except headers mentioned as _singleton_,
 * e.g. [Content-Type](https://www.rfc-editor.org/rfc/rfc9110.html#name-content-type)
 *
 * @see Header
 * @see HeaderMap
 * @see Headers.Companion
 * @see Headers.ContentTypes
 * @see Headers.AcceptEncodings
 * @see <a href="https://www.rfc-editor.org/rfc/rfc9110.html">RFC 9110 - HTTP Semantics</a>
 * @see <a href="https://ktor.io/docs/client-requests.html#headers">Ktor Client Request - Headers</a>
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class Headers(
    /** One or more header strings in `"Name: Value"` format. */
    vararg val value: String,
) {
    /**
     * Contain most used headers **name**
     *
     * Extracted of [Http Headers Ktor](https://github.com/ktorio/ktor/blob/main/ktor-http/common/src/io/ktor/http/HttpHeaders.kt)
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers">Http Headers MDN</a>
     */
    companion object {
        // Permanently registered standard HTTP headers
        // The list is taken from http://www.iana.org/assignments/message-headers/message-headers.xml#perm-headers
        const val Accept = "Accept"
        const val AcceptCharset = "Accept-Charset"
        const val AcceptEncoding = "Accept-Encoding"
        const val AcceptLanguage = "Accept-Language"
        const val AcceptRanges = "Accept-Ranges"
        const val Age = "Age"
        const val Allow = "Allow"

        // Application-Layer Protocol Negotiation, HTTP/2
        const val ALPN = "ALPN"
        const val AuthenticationInfo = "Authentication-Info"
        const val Authorization = "Authorization"
        const val CacheControl = "Cache-Control"
        const val Connection = "Connection"
        const val ContentDisposition = "Content-Disposition"
        const val ContentEncoding = "Content-Encoding"
        const val ContentLanguage = "Content-Language"
        const val ContentLength = "Content-Length"
        const val ContentLocation = "Content-Location"
        const val ContentRange = "Content-Range"
        const val ContentType = "Content-Type"
        const val Cookie = "Cookie"

        // WebDAV Search
        const val DASL = "DASL"
        const val Date = "Date"

        // WebDAV
        const val DAV = "DAV"
        const val Depth = "Depth"

        const val Destination = "Destination"
        const val ETag = "ETag"
        const val Expect = "Expect"
        const val Expires = "Expires"
        const val From = "From"
        const val Forwarded = "Forwarded"
        const val Host = "Host"
        const val HTTP2Settings = "HTTP2-Settings"
        const val If = "If"
        const val IfMatch = "If-Match"
        const val IfModifiedSince = "If-Modified-Since"
        const val IfNoneMatch = "If-None-Match"
        const val IfRange = "If-Range"
        const val IfScheduleTagMatch = "If-Schedule-Tag-Match"
        const val IfUnmodifiedSince = "If-Unmodified-Since"
        const val LastModified = "Last-Modified"
        const val Location = "Location"
        const val LockToken = "Lock-Token"
        const val Link = "Link"
        const val MaxForwards = "Max-Forwards"
        const val MIMEVersion = "MIME-Version"
        const val OrderingType = "Ordering-Type"
        const val Origin = "Origin"
        const val Overwrite = "Overwrite"
        const val Position = "Position"
        const val Pragma = "Pragma"
        const val Prefer = "Prefer"
        const val PreferenceApplied = "Preference-Applied"
        const val ProxyAuthenticate = "Proxy-Authenticate"
        const val ProxyAuthenticationInfo = "Proxy-Authentication-Info"
        const val ProxyAuthorization = "Proxy-Authorization"
        const val constKeyPins = "const-Key-Pins"
        const val constKeyPinsReportOnly = "const-Key-Pins-Report-Only"
        const val Range = "Range"
        const val Referrer = "Referer"
        const val RetryAfter = "Retry-After"
        const val ScheduleReply = "Schedule-Reply"
        const val ScheduleTag = "Schedule-Tag"
        const val SecWebSocketAccept = "Sec-WebSocket-Accept"
        const val SecWebSocketExtensions = "Sec-WebSocket-Extensions"
        const val SecWebSocketKey = "Sec-WebSocket-Key"
        const val SecWebSocketProtocol = "Sec-WebSocket-Protocol"
        const val SecWebSocketVersion = "Sec-WebSocket-Version"
        const val Server = "Server"
        const val SetCookie = "Set-Cookie"

        // Atom Publishing
        const val SLUG = "SLUG"
        const val StrictTransportSecurity = "Strict-Transport-Security"
        const val TE = "TE"
        const val Timeout = "Timeout"
        const val Trailer = "Trailer"
        const val TransferEncoding = "Transfer-Encoding"
        const val Upgrade = "Upgrade"
        const val UserAgent = "User-Agent"
        const val Vary = "Vary"
        const val Via = "Via"
        const val Warning = "Warning"
        const val WWWAuthenticate = "WWW-Authenticate"

        // CORS
        const val AccessControlAllowOrigin = "Access-Control-Allow-Origin"
        const val AccessControlAllowMethods = "Access-Control-Allow-Methods"
        const val AccessControlAllowCredentials = "Access-Control-Allow-Credentials"
        const val AccessControlAllowHeaders = "Access-Control-Allow-Headers"

        const val AccessControlRequestMethod = "Access-Control-Request-Method"
        const val AccessControlRequestHeaders = "Access-Control-Request-Headers"
        const val AccessControlExposeHeaders = "Access-Control-Expose-Headers"
        const val AccessControlMaxAge = "Access-Control-Max-Age"

        // Unofficial de-facto headers
        const val XHttpMethodOverride = "X-Http-Method-Override"
        const val XForwardedHost = "X-Forwarded-Host"
        const val XForwardedServer = "X-Forwarded-Server"
        const val XForwardedProto = "X-Forwarded-Proto"
        const val XForwardedFor = "X-Forwarded-For"

        const val XForwardedPort = "X-Forwarded-Port"

        const val XRequestId = "X-Request-ID"
        const val XCorrelationId = "X-Correlation-ID"
        const val XTotalCount = "X-Total-Count"
    }

    /**
     * Contain common types of header `Content-Type` subdivide in objects as `Type/Subtype`
     *
     * Extracted of [ContentTypes Ktor](https://github.com/ktorio/ktor/blob/main/ktor-http/common/src/io/ktor/http/ContentTypes.kt)
     */
    object ContentTypes {
        /** Represents a pattern `* / *` to match any content type. */
        const val Any = "*/*"

        /** Provides a list of standard subtypes of an `application` content type. */
        object Application {
            const val TYPE: String = "application"

            /** Represents a pattern `application / *` to match any application content type. */
            const val Any = "$TYPE/*"
            const val Atom = "$TYPE/atom+xml"
            const val Cbor = "$TYPE/cbor"
            const val Json = "$TYPE/json"
            const val HalJson = "$TYPE/hal+json"
            const val JavaScript = "$TYPE/javascript"
            const val OctetStream = "$TYPE/octet-stream"
            const val Rss = "$TYPE/rss+xml"
            const val Soap = "$TYPE/soap+xml"
            const val Xml = "$TYPE/xml"
            const val Xml_Dtd = "$TYPE/xml-dtd"
            const val Yaml = "$TYPE/yaml"
            const val Zip = "$TYPE/zip"
            const val GZip = "$TYPE/gzip"
            const val FormUrlEncoded = "$TYPE/x-www-form-urlencoded"
            const val Pdf = "$TYPE/pdf"
            const val Xlsx = "$TYPE/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            const val Docx = "$TYPE/vnd.openxmlformats-officedocument.wordprocessingml.document"
            const val Pptx = "$TYPE/vnd.openxmlformats-officedocument.presentationml.presentation"
            const val ProtoBuf = "$TYPE/protobuf"
            const val Wasm = "$TYPE/wasm"
            const val ProblemJson = "$TYPE/problem+json"
            const val ProblemXml = "$TYPE/problem+xml"
        }

        /** Provides a list of standard subtypes of an `audio` content type. */
        object Audio {
            const val TYPE: String = "audio"

            const val Any = "$TYPE/*"
            const val MP4 = "$TYPE/mp4"
            const val MPEG = "$TYPE/mpeg"
            const val OGG = "$TYPE/ogg"
        }

        /** Provides a list of standard subtypes of an `image` content type. */
        object Image {
            const val TYPE: String = "image"

            const val Any = "$TYPE/*"
            const val GIF = "$TYPE/gif"
            const val JPEG = "$TYPE/jpeg"
            const val PNG = "$TYPE/png"
            const val SVG = "$TYPE/svg+xml"
            const val XIcon = "$TYPE/x-icon"
        }

        /** Provides a list of standard subtypes of a `message` content type. */
        object Message {
            const val TYPE: String = "message"

            const val Any = "$TYPE/*"
            const val Http = "$TYPE/http"
        }

        /** Provides a list of standard subtypes of a `multipart` content type. */
        object MultiPart {
            const val TYPE: String = "multipart"

            const val Any = "$TYPE/*"
            const val Mixed = "$TYPE/mixed"
            const val Alternative = "$TYPE/alternative"
            const val Related = "$TYPE/related"
            const val FormData = "$TYPE/form-data"
            const val Signed = "$TYPE/signed"
            const val Encrypted = "$TYPE/encrypted"
            const val ByteRanges = "$TYPE/byteranges"
        }

        /** Provides a list of standard subtypes of a `text` content type. */
        object Text {
            const val TYPE: String = "text"

            const val Any = "$TYPE/*"
            const val Plain = "$TYPE/plain"
            const val CSS = "$TYPE/css"
            const val CSV = "$TYPE/csv"
            const val Html = "$TYPE/html"
            const val JavaScript = "$TYPE/javascript"
            const val VCard = "$TYPE/vcard"
            const val Xml = "$TYPE/xml"
            const val EventStream = "$TYPE/event-stream"
        }

        /** Provides a list of standard subtypes of a `video` content type. */
        object Video {
            const val TYPE: String = "video"

            const val Any = "$TYPE/*"
            const val MPEG = "$TYPE/mpeg"
            const val MP4 = "$TYPE/mp4"
            const val OGG = "$TYPE/ogg"
            const val QuickTime = "$TYPE/quicktime"
        }

        /** Provides a list of standard subtypes of a `font` content type. */
        object Font {
            const val TYPE: String = "font"

            const val Any = "$TYPE/*"
            const val Collection = "$TYPE/collection"
            const val Otf = "$TYPE/otf"
            const val Sfnt = "$TYPE/sfnt"
            const val Ttf = "$TYPE/ttf"
            const val Woff = "$TYPE/woff"
            const val Woff2 = "$TYPE/woff2"
        }
    }

    /**
     * Contain commonly used `Accept-Encoding` values.
     *
     * Extracted of [AcceptEncoding Ktor](https://github.com/ktorio/ktor/blob/main/ktor-http/common/src/io/ktor/http/header/AcceptEncoding.kt)
     */
    object AcceptEncodings {
        const val All = "*"
        const val Gzip = "gzip"
        const val Compress = "compress"
        const val Deflate = "deflate"
        const val Br = "br"
        const val Zstd = "zstd"
        const val Identity = "identity"
    }
}
