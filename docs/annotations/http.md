# HTTP Annotations

These annotations define how **Ktorgen** maps Kotlin interfaces and methods into **HTTP requests** for **Ktor Client**.

Ktorgen’s HTTP annotations are inspired by **Retrofit** and **Ktorfit**, providing a familiar developer experience while maintaining:

- **KMP compatibility**
- **Type safety**
- **Compile-time validation**

---

## HTTP Method Annotations

HTTP method annotations describe the **type of HTTP request** the generated function will perform.

---

### [`@HTTP`](https://github.com/kingg22/ktorgen/blob/main/annotations/src/commonMain/kotlin/io/github/kingg22/ktorgen/http/HTTP.kt)

Generic annotation for defining a **custom HTTP method**.
Can be used instead of specific method annotations (`@GET`, `@POST`, etc.).

**Parameters**

| Name      | Type      | Description                                                                                                                                           | Default      |
|-----------|-----------|-------------------------------------------------------------------------------------------------------------------------------------------------------|--------------|
| `method`  | `String`  | The HTTP method name (e.g. `"TRACE"`).                                                                                                                | **Required** |
| `path`    | `String`  | Endpoint path relative to [`@KtorGen.basePath`](core.md#description) or [Ktor default request](https://ktor.io/docs/client-default-request.html#url). | `""`         |
| `hasBody` | `Boolean` | Whether this method allows a request body.                                                                                                            | `false`      |

#### Examples

**Custom method**

```kotlin
@HTTP(method = "PROPFIND", path = "meta", hasBody = false)
suspend fun getMetadata(): Metadata
```

**Body with non-standard verb**

```kotlin
@HTTP(method = "LINK", path = "relations", hasBody = true)
suspend fun linkUser(@Body data: RelationBody): Result
```

---

### Standard HTTP Methods

Shortcut annotations for common HTTP methods.

Equivalent to using `@HTTP(method = "...", path = "...", hasBody = ...)`.

Supported annotations:

* [`@GET`](https://github.com/kingg22/ktorgen/blob/main/annotations/src/commonMain/kotlin/io/github/kingg22/ktorgen/http/GET.kt)
* [`@POST`](https://github.com/kingg22/ktorgen/blob/main/annotations/src/commonMain/kotlin/io/github/kingg22/ktorgen/http/POST.kt)
* [`@PUT`](https://github.com/kingg22/ktorgen/blob/main/annotations/src/commonMain/kotlin/io/github/kingg22/ktorgen/http/PUT.kt)
* [`@PATCH`](https://github.com/kingg22/ktorgen/blob/main/annotations/src/commonMain/kotlin/io/github/kingg22/ktorgen/http/PATCH.kt)
* [`@DELETE`](https://github.com/kingg22/ktorgen/blob/main/annotations/src/commonMain/kotlin/io/github/kingg22/ktorgen/http/DELETE.kt)
* [`@HEAD`](https://github.com/kingg22/ktorgen/blob/main/annotations/src/commonMain/kotlin/io/github/kingg22/ktorgen/http/HEAD.kt)
* [`@OPTIONS`](https://github.com/kingg22/ktorgen/blob/main/annotations/src/commonMain/kotlin/io/github/kingg22/ktorgen/http/OPTIONS.kt)

**Parameters**

| Name   | Type     | Description                           | Default |
|--------|----------|---------------------------------------|---------|
| `path` | `String` | Relative path appended to `basePath`. | `""`    |

#### Examples

**Basic GET**

```kotlin
@GET("users/{id}")
suspend fun getUser(@Path id: Long): User
```

**POST with body**

```kotlin
@POST("users/new")
suspend fun createUser(@Body user: UserCreateRequest): Response<User>
```

---

## Request Encoding Annotations

These annotations define **how request data is encoded** before being sent to the server.

---

### [`@FormUrlEncoded`](https://github.com/kingg22/ktorgen/blob/main/annotations/src/commonMain/kotlin/io/github/kingg22/ktorgen/http/FormUrlEncoded.kt)

Marks a request body as `application/x-www-form-urlencoded`.

* Usually combined with [`@Field` or `@FieldMap`](#field-fieldmap)
* This annotation can be **omitted**, as it is inferred automatically when those parameters are present

#### Examples

```kotlin
@FormUrlEncoded
@POST("login")
suspend fun login(
  @Field("user") user: String,
  @Field("pass") password: String
): Token
```

```kotlin
@FormUrlEncoded
@POST("register")
suspend fun register(@FieldMap data: Map<String, String>): ApiResponse
```

---

### [`@Multipart`](https://github.com/kingg22/ktorgen/blob/main/annotations/src/commonMain/kotlin/io/github/kingg22/ktorgen/http/Multipart.kt)

Marks a request as `multipart/form-data`.

* Combined with [`@Part` or `@PartMap`](#part-partmap)
* Can also be **omitted**, as it is inferred from parameters

#### Examples

```kotlin
@Multipart
@POST("upload")
suspend fun uploadFile(@Part file: ByteArray): UploadResponse
```

```kotlin
@Multipart
@POST("profile")
suspend fun updateProfile(
  @Part("avatar") image: File,
  @Part("bio") bio: String
): Profile
```

---

## Parameter Annotations

Parameter annotations describe how individual function parameters are mapped into the HTTP request.

---

### [`@Body`](https://github.com/kingg22/ktorgen/blob/main/annotations/src/commonMain/kotlin/io/github/kingg22/ktorgen/http/Body.kt)

Marks a parameter as the **request body**.

**Rules**

* Only one `@Body` is allowed per function
* Incompatible with:
    * `@FormUrlEncoded`
    * `@Multipart`

#### Examples

```kotlin
@POST("users")
suspend fun create(@Body user: User): ApiResponse
```

```kotlin
@PUT("users/{id}")
suspend fun update(
  @Path id: Long,
  @Body update: UserUpdate
): ApiResponse
```

---

### [`@Path`](https://github.com/kingg22/ktorgen/blob/main/annotations/src/commonMain/kotlin/io/github/kingg22/ktorgen/http/Path.kt)

Replaces placeholders inside the request path.

**Rules**

* Names must match placeholders in the endpoint path.

**Parameters**

| Name      | Type      | Description                               | Default        |
|-----------|-----------|-------------------------------------------|----------------|
| `value`   | `String`  | Placeholder name (without `{}`).          | Parameter name |
| `encoded` | `Boolean` | Whether the value is already URL-encoded. | `false`        |

#### Examples

```kotlin
@GET("users/{id}")
suspend fun get(@Path id: Int): User
```

```kotlin
@DELETE("users/{id}")
suspend fun delete(@Path("id") userId: Long)
```

---

### [`@Query`](https://github.com/kingg22/ktorgen/blob/main/annotations/src/commonMain/kotlin/io/github/kingg22/ktorgen/http/Query.kt) / [`@QueryMap`](https://github.com/kingg22/ktorgen/blob/main/annotations/src/commonMain/kotlin/io/github/kingg22/ktorgen/http/QueryMap.kt)

Appends query parameters to the URL.

**Rules**

* `null` values are omitted
* Lists are supported
* Map keys cannot be null

**Parameters**

| Name      | Type      | Description                          | Default        |
|-----------|-----------|--------------------------------------|----------------|
| `value`   | `String`  | Query parameter name                 | Parameter name |
| `encoded` | `Boolean` | Whether the value is already encoded | `false`        |

#### Examples

```kotlin
@GET("search")
suspend fun search(@Query("q") query: String): List<Result>
```

```kotlin
@GET("filter")
suspend fun filter(@QueryMap params: Map<String, String>): ApiResponse
```

---

### [`@QueryName`](https://github.com/kingg22/ktorgen/blob/main/annotations/src/commonMain/kotlin/io/github/kingg22/ktorgen/http/QueryName.kt)

Adds a **query parameter without value**, commonly used as flags.

| Parameter | Type      | Description                                            | Default                   |
|-----------|-----------|--------------------------------------------------------|---------------------------|
| `encoded` | `Boolean` | If `true`, the parameter is assumed to be pre-encoded. | `false`                   |

#### Examples

```kotlin
@GET("users")
suspend fun all(@QueryName("active") active: Boolean = true): List<User>
```

```kotlin
@GET("posts")
suspend fun includeDrafts(
  @QueryName includeDrafts: Boolean = false
): List<Post>
```

---

## Header Annotations

Headers in Ktorgen are **type-safe**, **repeatable**, and validated at compile time.

Can a use a [list of most used header names](https://github.com/kingg22/ktorgen/blob/main/annotations/src/commonMain/kotlin/io/github/kingg22/ktorgen/http/Header.kt#L54)
defined in `Header.Companion`. Extracted from [Http Headers Ktor](https://github.com/ktorio/ktor/blob/main/ktor-http/common/src/io/ktor/http/HttpHeaders.kt)

Contain common types of header `Content-Type` subdivide in objects as `Type/Subtype`. Extracted of [ContentTypes Ktor](https://github.com/ktorio/ktor/blob/main/ktor-http/common/src/io/ktor/http/ContentTypes.kt)

Contain commonly used `Accept-Encoding` values. Extracted of [AcceptEncoding Ktor](https://github.com/ktorio/ktor/blob/main/ktor-http/common/src/io/ktor/http/header/AcceptEncoding.kt)

See [Http Headers MDN]("https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers) for more information.

---

### [`@Header`](https://github.com/kingg22/ktorgen/blob/main/annotations/src/commonMain/kotlin/io/github/kingg22/ktorgen/http/Header.kt)

Defines a **static header** at compile time.

**Parameters**

| Name    | Type     | Description  |
|---------|----------|--------------|
| `name`  | `String` | Header name  |
| `value` | `String` | Header value |

#### Example

```kotlin
@Header("Content-Type", "application/json")
@Header(Header.Accept, Header.ContentType.Aplication.Json)
suspend fun request(): String
```

---

### [`@HeaderParam`](https://github.com/kingg22/ktorgen/blob/main/annotations/src/commonMain/kotlin/io/github/kingg22/ktorgen/http/HeaderParam.kt)

Defines a **dynamic header** provided at runtime.

| Parameter | Type     | Description  |
|-----------|----------|--------------|
| `value`   | `String` | Header name. |

### Examples

```kotlin
@GET("users")
suspend fun list(
  @HeaderParam("Authorization") token: String,
  @HeaderParam(Header.AcceptLanguage) lang: String
): List<User>
```

---

### [`@HeaderMap`](https://github.com/kingg22/ktorgen/blob/main/annotations/src/commonMain/kotlin/io/github/kingg22/ktorgen/http/HeaderMap.kt)

Adds multiple headers dynamically from a map.

#### Examples

```kotlin
@GET("secure")
suspend fun auth(@HeaderMap headers: Map<String, String>): Token
```

---

## Form & Multipart Parameters

---

### [`@Field`](https://github.com/kingg22/ktorgen/blob/main/annotations/src/commonMain/kotlin/io/github/kingg22/ktorgen/http/Field.kt) / [`@FieldMap`](https://github.com/kingg22/ktorgen/blob/main/annotations/src/commonMain/kotlin/io/github/kingg22/ktorgen/http/FieldMap.kt)

Used inside [`@FormUrlEncoded`](#formurlencoded) request to send key-value pairs.

#### Example

```kotlin
@FormUrlEncoded
@POST("auth")
suspend fun login(
  @Field("username") user: String,
  @Field("password") pass: String
)
```

---

### [`@Part`](https://github.com/kingg22/ktorgen/blob/main/annotations/src/commonMain/kotlin/io/github/kingg22/ktorgen/http/Part.kt) / [`@PartMap`](https://github.com/kingg22/ktorgen/blob/main/annotations/src/commonMain/kotlin/io/github/kingg22/ktorgen/http/PartMap.kt)

Used inside [`@Multipart`](#multipart) requests to send files or multiple parts.

**Important**

If the type is [PartData](https://api.ktor.io/ktor-http/io.ktor.http.content/-part-data/index.html)
the value will be used directly with its content type.

#### Example

```kotlin
@Multipart
@POST("files")
suspend fun upload(@Part("file") file: File)
```

---

## URL & Fragment Annotations

---

### [`@Url`](https://github.com/kingg22/ktorgen/blob/main/annotations/src/commonMain/kotlin/io/github/kingg22/ktorgen/http/Url.kt)

Replaces the **entire request URL**, ignoring `basePath`.

Can be a type of:

* `String`
* [Ktor `Url`](https://api.ktor.io/ktor-http/io.ktor.http/-url/index.html)
* [Ktor `UrlBuilder`](https://api.ktor.io/ktor-http/io.ktor.http/-u-r-l-builder/index.html)

More details, see [Url.takeFrom](https://api.ktor.io/ktor-http/io.ktor.http/take-from.html)

**Rules**:

* Cannot be used together with a path [HTTP method annotations](http.md#http-method-annotations).
* Cannot be used together with [`@Query`, `@QueryMap`](#query-querymap), [`@Path`](#path)
* Only one `@Url` is allowed per function.

#### Examples

```kotlin
@GET
suspend fun fromFullUrl(@Url url: String): ApiResponse
```

---

### [`@Fragment`](https://github.com/kingg22/ktorgen/blob/main/annotations/src/commonMain/kotlin/io/github/kingg22/ktorgen/http/Fragment.kt)

Injects URL fragments dynamically.

**Rules**

* Only one fragment per function
* `null` values are ommited

#### Examples

```kotlin
@GET("/users/{id}")
@Fragment("collection")
suspend fun fetchDynamic(@Path id: String): Response
```

```kotlin
@GET("/users/{id}")
suspend fun fetchDynamic(@Path id: String, @Fragment collection: String): Response
```

---

## Cookie Annotation

---

### [`@Cookie`](https://github.com/kingg22/ktorgen/blob/main/annotations/src/commonMain/kotlin/io/github/kingg22/ktorgen/http/Cookie.kt)

Adds cookies to the request.

This annotation mirrors the behavior of [`HttpMessageBuilder.cookie`](https://api.ktor.io/ktor-client-core/io.ktor.client.request/cookie.html),
but includes some adaptations due to annotation limitations:

- `null` values are not allowed.
  Instead, the following conventions are used:
  - `expiresTimestamp = -1L` → no expiration date.
  - `domain = ""` and `path = ""` → no value assigned.
- `extensions` is defined as an array of `PairString` for type safety when mapping cookie extensions.

**Important**

If the Ktor **Cookie plugin** is installed, cookies defined here are ignored at runtime.
For more information, refer to the official [Ktor Client Request - Cookies](https://ktor.io/docs/client-requests.html#cookies) documentation.

**Rules**:

* The value of the cookie is required in the function target.

#### Example

```kotlin
@Cookie(
  name = "session_id",
  value = "abc123",
  maxAge = 3600,
  secure = true,
  httpOnly = true,
  extensions = [Cookie.PairString("SameSite", "Strict")],
)
@GET
suspend fun myRequest(
  @Cookie("yummy_cookie") flavor: String
): SecureResponse
```

---

## Tag Annotation

---

### [`@Tag`](https://github.com/kingg22/ktorgen/blob/main/annotations/src/commonMain/kotlin/io/github/kingg22/ktorgen/http/Tag.kt)

Adds metadata to requests, useful for interceptors, logging, or [custom plugins](https://ktor.io/docs/client-custom-plugins.html#call-state)
as [AttributeKey](https://api.ktor.io/ktor-utils/io.ktor.util/-attribute-key/index.html).

**Rules**

* `null` values are ommited.

#### Example

```kotlin
@POST("upload")
suspend fun upload(
  @Body file: File,
  @Tag("UploadInterceptor") interceptor: String,
  @Tag tag: String?
)
```

---

## Deprecated Annotations

---

### ~~`@Streaming`~~

Deprecated.

Use return types like:

* `HttpResponse`
* `ByteReadChannel`
* `ByteArray`
* `String`

See [**Return Types**](../return_types.md) documentation for more information.

---

### ~~`@Headers`~~

Deprecated.
This is only for easy migration from other libraries, using **this doesn't generate code**

From
```kotlin
@Headers("Content-Type: application/json", "Accept: application/json")
```
to
```kotlin
@Header(Header.ContentType, Header.ContentTypes.Application.Json)
@Header("Accept", "application/json)
```

See [**Header annotations**](#header-annotations) for more information.

---

## Summary

Ktorgen’s HTTP annotations:

* Follow Retrofit semantics
* Are **KMP-compatible**
* Are **type-safe and repeatable**
* Integrate directly with **Ktor Client**

To migrate from Retrofit:

```kotlin
import io.github.kingg22.ktorgen.http.*
```

And you're done 🚀
