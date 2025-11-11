## Core Annotations

This section documents the **core annotations** used to define options and how the compiler interprets them.
These annotations control how Ktorgen scans interfaces, generates client factories, and connects them to Ktor’s HTTP client.

---

## 🧩 `@KtorGen`

The main entry point annotation for Ktorgen.
It’s applied to an **interface** or its **companion object** that defines the HTTP endpoints and client configuration.

### Definition
```kotlin
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class KtorGen(
    val name: String = KTORGEN_DEFAULT_NAME,
    val generate: Boolean = true,
    @property:Language("http-url-reference")
    val basePath: String = "",
    val generateTopLevelFunction: Boolean = true,
    val generateCompanionExtFunction: Boolean = false,
    val generateHttpClientExtension: Boolean = false,
    @property:KtorGenExperimental
    val propagateAnnotations: Boolean = true,
    @property:KtorGenExperimental
    val annotations: Array<KClass<out Annotation>> = [],
    @property:KtorGenExperimental
    val optInAnnotations: Array<KClass<out Annotation>> = [],
    @property:KtorGenExperimental
    val functionAnnotations: Array<KClass<out Annotation>> = [],
    @property:KtorGenExperimental
    val visibilityModifier: String = KTORGEN_DEFAULT_NAME,
    @property:KtorGenExperimental
    val classVisibilityModifier: String = KTORGEN_DEFAULT_NAME,
    @property:KtorGenExperimental
    val constructorVisibilityModifier: String = KTORGEN_DEFAULT_NAME,
    @property:KtorGenExperimental
    val functionVisibilityModifier: String = KTORGEN_DEFAULT_NAME,
    val customFileHeader: String = KTORGEN_DEFAULT_NAME,
    val customClassHeader: String = "",
)
```

### Description

Marks an interface as a **Ktorgen API**.
The compiler will analyze all annotated functions within this interface and generate an implementation that uses `io.ktor.client.HttpClient` under the hood.


| Parameter                       | Type                                             | Description                                                                                              | Default value                                     |
|---------------------------------|--------------------------------------------------|----------------------------------------------------------------------------------------------------------|---------------------------------------------------|
| `name`                          | `String`                                         | Optional identifier used to name the generated client class.                                             | Interface name with `_` prefix and `Impl` suffix. |
| `generate`                      | `Boolean`                                        | Whether the annotated interface participates in code generation.                                         | `true`                                            |
| `basePath`                      | `String`                                         | Base path or URL prepended to all relative endpoints in functions.                                       | `""`                                              |
| `generateTopLevelFunction`      | `Boolean`                                        | Generate a top-level factory function in the same package.                                               | `true`                                            |
| `generateCompanionExtFunction`  | `Boolean`                                        | Generate a companion extension factory (`.create(HttpClient)`). **Requires a companion object declared** | `false`                                           |
| `generateHttpClientExtension`   | `Boolean`                                        | Generate an extension on `HttpClient` to instantiate the client. (`.createMyApi()`)                      | `false`                                           |
| `propagateAnnotations`          | `Boolean` *(Experimental)*                       | Copy annotations from interface to generated code.                                                       | `true`                                            |
| `annotations`                   | `Array<KClass<out Annotation>>` *(Experimental)* | Explicit annotations to propagate. Only those with empty constructors or default values.                 | `[]`                                              |
| `optInAnnotations`              | `Array<KClass<out Annotation>>` *(Experimental)* | `@RequiresOptIn` annotations to be added to generated code with `OptIn()`.                               | `[]`                                              |
| `functionAnnotations`           | `Array<KClass<out Annotation>>` *(Experimental)* | Annotations to add to generated extension functions.                                                     | `[]`                                              |
| `visibilityModifier`            | `String` *(Experimental)*                        | Global visibility (`public`, `internal`).                                                                | Same as interface                                 |
| `classVisibilityModifier`       | `String` *(Experimental)*                        | Visibility for generated implementation class.                                                           | Same as `visibilityModifier`                      |
| `constructorVisibilityModifier` | `String` *(Experimental)*                        | Visibility for generated primary constructor.                                                            | Same as `visibilityModifier`                      |
| `functionVisibilityModifier`    | `String` *(Experimental)*                        | Visibility for generated extension functions.                                                            | Same as `visibilityModifier`                      |
| `customFileHeader`              | `String`                                         | Custom file-level comment inserted at top of generated file.                                             | Generated notice                                  |
| `customClassHeader`             | `String`                                         | Custom KDoc comment or annotations for generated class.                                                  | `""`                                              |

### Example

**User code**
```kotlin
@KtorGen(
    name = "UserApiClient",
    basePath = "/users/",
)
interface UserApi {
    @GET("{id}")
    suspend fun getUser(@Path("id") id: String): User
}
```

**Generated code**
```kotlin
@file:Suppress("...")
package ...
import ...

/** Generated by KtorGen */
@Generated
public class UserApiClient public constructor(
    private val _httpClient: HttpClient,
) : UserApi {
    /** Generated by KtorGen */
    @Generated
    override suspend fun getUser(id: String): User = _httpClient.request {
        this.method = HttpMethod.Get
        this.url {
            this.takeFrom("""/users/${id.encodeURLPath()}""")
        }
    }.body()
}
```
**User code**
```kotlin
@KtorGen(
    basePath = "album/",
    visibilityModifier = "internal",
    classVisibilityModifier = "private",
    functionAnnotations = [JvmSynthetic::class, InternalDeezerClient::class],
    annotations = [InternalDeezerClient::class],
)
interface AlbumRoutes {
    @GET("{id}")
    @JvmSynthetic
    suspend fun getById(@Path id: Long): Album
}
```

**Generated code**
```kotlin
@Generated
@InternalDeezerClient
@JvmSynthetic
internal fun AlbumRoutes(httpClient: HttpClient): AlbumRoutes = _AlbumRoutes(httpClient)

@Generated
@InternalDeezerClient
private class _AlbumRoutesImpl internal constructor(
    private val _httpClient: HttpClient
) : AlbumRoutes {
    @Generated
    @JvmSynthetic
    override suspend fun getById(id: Long): Album = _httpClient.request {
      this.method = HttpMethod.Get
      this.url {
          this.takeFrom("album/$id")
      }
    }.body()
}
```

---

### Notes

- Some parameters marked as *experimental* may generate invalid code depending on platform support or KSP version.
- Misconfigured visibility modifiers can lead `fatal error occurred, invalid visibility` or propagated annotations can lead to `unresolved symbols` during compilation.
- Annotations like `@JvmOverloads` or `@JvmSynthetic` are only valid on `common`, `jvm`, or `android`; using them on multiplatform-specific configurations may fail, including annotation in params.
- Custom annotations must have **empty constructors** to be propagated automatically when it's added as parameter.
- Works across all supported KMP targets.
- If you omit `name`, the compiler generates a default class name based on the interface (`_UserApiImpl`).
- You can inject a custom `HttpClient` when instantiating the generated client.
- Indentation can differ, but has 4 spaces by default*
- By default, the generated code have `@Generated` and comments indicate generated code.

---

## ⚙️ `@KtorGenFunction`

Marks a single **function** inside a `@KtorGen` interface as participating in code generation.

Allows fine-grained control over how annotations and visibility are transferred from the source definition to the generated method.

### Definition
```kotlin
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class KtorGenFunction(
    val generate: Boolean = true,
    @property:KtorGenExperimental
    val propagateAnnotations: Boolean = true,
    @property:KtorGenExperimental
    val annotations: Array<KClass<out Annotation>> = [],
    @property:KtorGenExperimental
    val optInAnnotations: Array<KClass<out Annotation>> = [],
    val customHeader: String = "",
)
```

### Description

When combined with standard HTTP annotations (`@GET`, `@POST`, etc.), this signals the compiler to generate a corresponding HTTP call implementation.

| Parameter              | Type                                             | Description                                                                              | Default value |
|------------------------|--------------------------------------------------|------------------------------------------------------------------------------------------|---------------|
| `generate`             | `Boolean`                                        | Whether this function should generate code.                                              | `true`        |
| `propagateAnnotations` | `Boolean` *(Experimental)*                       | Copy annotations from source method to generated method.                                 | `true`        |
| `annotations`          | `Array<KClass<out Annotation>>` *(Experimental)* | Explicit annotations to propagate as-is. Must have empty constructors or default values. | `[]`          |
| `optInAnnotations`     | `Array<KClass<out Annotation>>` *(Experimental)* | Propagate `@RequiresOptIn` or `@SubclassOptInRequired` annotations.                      | `[]`          |
| `customHeader`         | `String`                                         | Custom KDoc header for the generated implementation.                                     | `""`          |

### Example

**User code**
```kotlin
interface TodoApi {
    @KtorGenFunction(generate = false)
    suspend fun createTask(@Body task: Task): Response<Task> = "some default value..."
}
```

**Generated code**
```kotlin
@Generated
public class _TodoApiImpl public constructor(private val httpClient: HttpClient) : TodoApi
// Skip code generation of createTask because have a default value defined in the interface
```

**User code**
```kotlin
interface UserRoutes {
    @GET("/users/{id}")
    @KtorGenFunction(
        propagateAnnotations = true,
        annotations = [JvmSynthetic::class],
        optInAnnotations = [ExperimentalApi::class],
        customHeader = "Auto-generated API method"
    )
    @ExperimentalApi
    suspend fun getUser(@Path id: Int): User
}
```

**Generated code**
```kotlin

/** Auto-generated API method */
@Generated
@OptIn(ExperimentalApi::class)
@JvmSynthetic
suspend fun getUser(id: Int): User = _httpClient.request {
    this.method = HttpMethod.Get
    this.url {
        this.takeFrom("/users/$id")
    }
}.body()
```

---

### Notes

- Use `@KtorGenFunction` when a function requires explicit generation control, otherwise don't include it.
- The processor will skip any function marked with `generate = false`, except  if it doesn't have default value because impl class must implement abstract members of the [interface](https://kotlinlang.org/docs/interfaces.html)
- Experimental properties (`propagateAnnotations`, `annotations`, `optInAnnotations`) may cause invalid code if incompatible annotations are propagated.
- In multiplatform projects, prefer keeping propagated annotations restricted to `common`-compatible ones.

---

## 🧪 `@KtorGenFunctionKmp` (Experimental)

This is an **experimental** annotation designed for **Kotlin Multiplatform (KMP)**.
It allows you to define generated functions compatible across multiple platforms, particularly where expect/actual declarations are used.

### Definition
```kotlin
@KtorGenExperimental
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class KtorGenFunctionKmp()
```

### Description

When applied, the compiler can generate `expect` declarations for shared functions, which are then completed by platform-specific `actual` implementations.

### Example

**User code**
```kotlin
@OptIn(KtorGenExperimental::class)
@KtorGenFunctionKmp
expect fun createAnalyticsApi(httpClient: HttpClient): AnalyticsApi

@KtorGen
interface AnalyticsApi {
    @POST("/events")
    suspend fun sendEvent(@Body event: Event)
}
```

**Generated code (each platform)**
```kotlin
actual fun createAnalyticsApi(httpClient: HttpClient): AnalyticsApi = _AnalyticsApiImpl(httpClient)

@Generated
public class _AnalyticsApiImpl public constructor(
    private val _httpClient: HttpClient
) : AnalyticsApi {
    @Generated
    override suspend fun sendEvent(event: Event) {
        this._httpClient.request {
          this.method = HttpMethod.Post
          this.url {
            this.takeFrom("https://api.example.com/events")
          }
          this.setBody(event)
        }
    }
}
```

### Notes

- Only recommended for advanced KMP setups.
- May produce unresolved symbols in some Kotlin versions if combined with JVM-specific annotations like `@JvmOverloads`.
- Intended to be stabilized once KSP adds full KMP common source generation support.
- Only `expect function` is supported with exact constructor properties.
- `expect interface` is not supported.

---

## 🪶 `@Generated`

This annotation is automatically inserted by the Ktorgen compiler into all generated source files.

### Definition

```kotlin
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.FILE)
@Retention(AnnotationRetention.BINARY)
annotation class Generated
```

### Description

Serves as a **marker** indicating that a file or class was generated automatically by the compiler.
Used internally by Ktorgen and for developer awareness (e.g., IDE highlighting or lint exclusions).

### Example (Generated Output)

```kotlin
// in file level
@file:Generated

// in factory top-level function
@Generated
public fun WeatherApi(httpClient: HttpClient): WeatherApi = _WeatherApiImpl(httpClient)

// in class level
@Generated
public class _WeatherApiImpl public constructor(
    private val _httpClient: HttpClient
) : WeatherApi {
    // in member functions
    @Generated
    override suspend fun getForecast(city: String): Forecast = // ...
}
```

### Notes

- Do not use this annotation manually.
- Future versions may include metadata such as generator version, option to don't include it, custom annotation.

---

## 🔗 Related Topics

- [HTTP annotations](http.md)
- [Configuration Guide](../configuration.md)
