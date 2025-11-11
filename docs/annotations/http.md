## HTTP Annotations

These annotations define how Ktorgen maps Kotlin interfaces and methods into **HTTP requests** for Ktor Client.

Ktorgen’s HTTP annotations are inspired by **Retrofit** and **Ktorfit**, providing a familiar developer experience while
maintaining **KMP compatibility** and **type-safety**.

---

## HTTP Method Annotations

These annotations describe the **type of HTTP request** the generated function will perform.

### `@HTTP`

Generic annotation for defining a custom HTTP method.
Can be used instead of specific method annotations (`@GET`, `@POST`, etc.).

| Parameter | Type      | Description                                                            | Default        |
|-----------|-----------|------------------------------------------------------------------------|----------------|
| `method`  | `String`  | The HTTP method name (e.g. `"TRACE"`).                                 | **Required**** |
| `path`    | `String`  | Endpoint path relative to `@KtorGen.basePath` or Ktor default request. | `""`           |
| `hasBody` | `Boolean` | Whether this method allows a request body.                             | `false`        |

**Example 1 – Custom Method**

```kotlin
@HTTP(method = "PROPFIND", path = "meta", hasBody = false)
suspend fun getMetadata(): Metadata
```

**Example 2 – Body with Nonstandard Verb**

```kotlin
@HTTP(method = "LINK", path = "relations", hasBody = true)
suspend fun linkUser(@Body data: RelationBody): Result
```

---

### `@GET`, `@POST`, `@PUT`, `@PATCH`, `@DELETE`, `@HEAD`, `@OPTIONS`

Shortcut annotations for common HTTP methods.
They are equivalent to using `@HTTP(method = "GET", path = "...")`.

| Parameter | Type     | Description                           | Default |
|-----------|----------|---------------------------------------|---------|
| `path`    | `String` | Relative path appended to `basePath`. | `""`    |

**Example 1 – Basic**

```kotlin
@GET("users/{id}")
suspend fun getUser(@Path id: Long): User
```

**Example 2 – POST with Body**

```kotlin
@POST("users/new")
suspend fun createUser(@Body user: UserCreateRequest): Response<User>
```

---

## Encoding Annotations

These define how request data is encoded when sent to the server.

### `@FormUrlEncoded`

Marks a function whose request body will be sent using `application/x-www-form-urlencoded`.

Combine with `@Field` or `@FieldMap` in parameters.

KtorGen offers omit this annotation, is deduced of parameters with `@Field` and `@FieldMap` ;)

**Example 1**

```kotlin
@FormUrlEncoded
@POST("login")
suspend fun login(@Field("user") user: String, @Field("pass") password: String): Token
```

**Example 2**

```kotlin
@FormUrlEncoded
@POST("register")
suspend fun register(@FieldMap data: Map<String, String>): ApiResponse
```

---

### `@Multipart`

Mark a function whose request will use multipart encoding.

Combine with `@Part` or `@PartMap`.

Can omit this annotation, is deduced of parameters with `@Part` or `@PartMap` ;)

**Example 1**

```kotlin
@Multipart
@POST("upload")
suspend fun uploadFile(@Part file: ByteArray): UploadResponse
```

**Example 2**

```kotlin
@Multipart
@POST("profile")
suspend fun updateProfile(@Part("avatar") image: File, @Part("bio") bio: String): Profile
```

---

## Parameter Annotations

### `@Body`

Indicates the parameter is the **request body**.

Only one `@Body` parameter is allowed per function.

Is **incompatible** with `@FormUrlEncoded`, `@Multipart`.

**Example 1**

```kotlin
@Header("Content-Type", "application/xml")
@POST("users")
suspend fun create(@Body user: User): ApiResponse
```

**Example 2**

```kotlin
@Header("Content-Type", "application/json")
@PUT("users/{id}")
suspend fun update(@Path id: Long, @Body update: UserUpdate): ApiResponse
```

---

### `@Path`

Substitutes a path segment in the URL.
Names must match placeholders in the endpoint path.

| Parameter | Type      | Description                                                                                    | Default                   |
|-----------|-----------|------------------------------------------------------------------------------------------------|---------------------------|
| `value`   | `String`  | Placeholder name (without `{}`).                                                               | The name of the parameter |
| `encoded` | `Boolean` | Specifies whether the argument value to the annotated method parameter is already URL encoded. | `false`                   |

**Example 1**

```kotlin
@GET("users/{id}")
suspend fun get(@Path id: Int): User
```

**Example 2**

```kotlin
@DELETE("users/{id}")
suspend fun delete(@Path("id") userId: Long)
```

---

### `@Query` and `@QueryMap`

Appends query parameters to the URL.
`@QueryMap` allows passing multiple entries from a map.
- Null values are omitted.
- Can be a `List<String>` but null values are filtered.
- For `Map` the key can't be nullable.

| Parameter | Type      | Description                                            | Default                   |
|-----------|-----------|--------------------------------------------------------|---------------------------|
| `value`   | `String`  | Query name.                                            | The name of the parameter |
| `encoded` | `Boolean` | If `true`, the parameter is assumed to be pre-encoded. | `false`                   |

**Example 1**

```kotlin
@GET("search")
suspend fun search(@Query("q") query: String): List<Result>
```

**Example 2**

```kotlin
@GET("filter")
suspend fun filter(@QueryMap params: Map<String, String>): ApiResponse
```

---

### `@QueryName`

Adds a query parameter **without value**, common for boolean flags.

| Parameter | Type      | Description                                            | Default                   |
|-----------|-----------|--------------------------------------------------------|---------------------------|
| `encoded` | `Boolean` | If `true`, the parameter is assumed to be pre-encoded. | `false`                   |

**Example 1**

```kotlin
@GET("users")
suspend fun all(@QueryName("active") active: Boolean = true): List<User>
```

**Example 2**

```kotlin
@GET("posts")
suspend fun includeDrafts(@QueryName includeDrafts: Boolean = false): List<Post>
```

---

### `@Header` and `@HeaderParam`

Headers are now **type-safe** and **repeatable**.
This replaces Retrofit’s `@Headers` and `@Header`.

#### Migration Example

Before:

```kotlin
@Headers("Content-Type: application/json", "Accept: application/json")
suspend fun request(@Header("Authentication") token: String): String
```

After:

```kotlin
@Header("Content-Type", "application/json")
@Header("Accept", "application/json")
suspend fun request(@HeaderParam("Authentication") token: String): String
```

#### `@Header`

Static header defined at compile time.
Can appear multiple times on the same function.

| Parameter | Type     | Description   | Default  |
|-----------|----------|---------------|----------|
| `name`    | `String` | Header name.  | Required |
| `value`   | `String` | Header value. | Required |

**Example 1**

```kotlin
@Header("Accept", "application/json")
@Header("Cache-Control", "no-cache")
@GET("profile")
suspend fun profile(): Profile
```

**Example 2**

```kotlin
@Header("User-Agent", "KtorgenClient/1.0")
@GET("meta")
suspend fun getMeta(): Meta
```

#### `@HeaderParam`

Dynamic header set at runtime via parameter.

| Parameter | Type     | Description  |
|-----------|----------|--------------|
| `value`   | `String` | Header name. |

**Example 1**

```kotlin
@GET("users")
suspend fun list(@HeaderParam("Authorization") token: String): List<User>
```

**Example 2**

```kotlin
@POST("upload")
suspend fun upload(@HeaderParam("X-Api-Key") apiKey: String, @Body data: UploadData): Response
```

---

### `@HeaderMap`

Adds multiple headers dynamically from a map.

**Example 1**

```kotlin
@GET("secure")
suspend fun auth(@HeaderMap headers: Map<String, String>): Token
```

**Example 2**

```kotlin
@POST("data")
suspend fun send(@HeaderMap extraHeaders: Map<String, String>, @Body content: Any): Result
```

---

### `@Field` and `@FieldMap`

Used inside `@FormUrlEncoded` methods to send key-value pairs.

**Example 1**

```kotlin
@FormUrlEncoded
@POST("auth")
suspend fun login(@Field("username") user: String, @Field("password") pass: String)
```

**Example 2**

```kotlin
@FormUrlEncoded
@POST("create")
suspend fun create(@FieldMap fields: Map<String, String>)
```

---

### `@Part` and `@PartMap`

Used inside `@Multipart` requests to send files or multiple parts.

If the type is [PartData](https://api.ktor.io/ktor-http/io.ktor.http.content/-part-data/index.html)
the value will be used directly with its content type.

**Example 1**

```kotlin
@Multipart
@POST("files")
suspend fun upload(@Part("file") file: File)
```

**Example 2**

```kotlin
@Multipart
@POST("update")
suspend fun updateProfile(@PartMap parts: Map<String, Any>)
```

---

### `@Url`

Replaces the full request URL at runtime, ignoring the `basePath`.

**Example 1**

```kotlin
@GET
suspend fun fromFullUrl(@Url url: String): ApiResponse
```

**Example 2**

```kotlin
@POST
suspend fun postTo(@Url url: String, @Body data: Data): Response
```

---

### `@Fragment`**

Allows inserting fragments in URLs, useful for modular routing.
Rarely needed but powerful in dynamic routing scenarios.

**Example 1** In function

```kotlin
@GET("/users/{id}")
@Fragment("collection")
suspend fun fetchDynamic(@Path id: String): Response
```

**Example 2** In parameter **soon***

```kotlin
@POST("collection/id") // don't include the fragment in url template
suspend fun postData(@Fragment name: String)
```

---

### `@Cookie`

## @Cookie

Add a cookie to the request.

**Important:**
If the Ktor **Cookie plugin** is installed, any cookies added through this annotation are ignored at runtime.
For more information, refer to the official [Ktor Client Request - Cookies](https://ktor.io/docs/client-requests.html#cookies) documentation.

This annotation mirrors the behavior of [`HttpMessageBuilder.cookie`](https://api.ktor.io/ktor-client/ktor-client-core/io.ktor.client.request/cookie.html),
but includes some adaptations due to annotation limitations:

- `null` values are not allowed.
  Instead, the following conventions are used:
  - `expiresTimestamp = -1L` → no expiration date.
  - `domain = ""` and `path = ""` → no value assigned.
- `extensions` is defined as an array of `PairString` for type safety when mapping cookie extensions.

### Example

**Example 1**

```kotlin
@GET("session")
@Cookie("session_id", "123456")
suspend fun getSession(): Session
```

**Example 2**

```kotlin
@GET("user")
suspend fun getUser(@Cookie("session_id") id: String): User
```

**Example 3**

```kotlin
@Cookie(
  name = "session_id",
  value = "abc123", // required if not declared as a parameter
  maxAge = 3600,
  expiresTimestamp = 1735689600000, // 01/01/2025 00:00:00 GMT
  secure = true,
  httpOnly = true,
  extensions = [Cookie.PairString("SameSite", "Strict")],
)
@GET
suspend fun myRequest(@Cookie("yummy_cookie") flavor: String): SecureResponse
```

Generated code:
```kotlin
this.cookie(
    name = """session_id""",
    value = """abc123""",
    maxAge = 3_600,
    expires = GMTDate(timestamp = 1_735_689_600_000),
    domain = null, // empty string is null
    path = null, // empty string is null
    secure = true,
    httpOnly = true,
    extensions = mapOf("""SameSite""" to """Strict"""),
)
this.cookie(
    name = """yummy_cookie""",
    value = """$flavor""",
    maxAge = 0,
    expires = null,
    domain = null,
    path = null,
    secure = false,
    httpOnly = false,
    extensions = emptyMap(),
)
```

---

### ~~`@Streaming`~~ Deprecated*

Allow receiving the raw response.

If you want the raw `HttpResponse` use that as return type, Ktor Client handle it, or `ByteReadChannel`, `ByteArray`, `String`, etc.

---

## Tag Annotation

### `@Tag`

Adds metadata to requests, useful for client interceptors, logging or [plugins of Ktor client](https://ktor.io/docs/client-custom-plugins.html#call-state)
as AttributeKey.

Null value is omitted.

**Example 1**

```kotlin
@GET("items")
suspend fun getItems(@Tag logging: String?): List<Item>
```

**Example 2**

```kotlin
@POST("upload")
suspend fun upload(@Body file: File, @Tag("UploadInterceptor") interceptor: String)
```

Code generated
```kotlin
httpClient.request {
   this.attributes.put(AttributeKey("UploadInterceptor"), interceptor)
}
```

---

## Summary

Ktorgen’s HTTP annotations follow the same semantics as Retrofit’s but are:

- **KMP-compatible**
- **Repeatable and type-safe**
- **Designed for direct Ktor Client integration**

Replace all Retrofit imports with:

```kotlin
import io.github.kingg22.ktorgen.http.*
```

adapt your headers and your migration is complete.
