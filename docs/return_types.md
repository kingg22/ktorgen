# Return Types and Request Customization

Ktorgen currently supports [**suspend functions**](https://kotlinlang.org/docs/async-programming.html#coroutines) and [**Coroutines Flow**](https://kotlinlang.org/docs/flow.html) as valid return types.
Each generated call is backed by a real [HttpClient](https://api.ktor.io/ktor-client-core/io.ktor.client/-http-client/index.html) request,
and therefore all functions must be either `suspend` or return a `Flow` stream that internally performs network calls asynchronously.

---

### ✅ Supported Return Types

| Type              | Description                                                                                                                                                            |
|-------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `suspend fun`     | Standard coroutine-based function. Returns the result of a single HTTP call.                                                                                           |
| `Flow<T>`         | Streams multiple responses or deferred requests as a Kotlin `Flow`, using safe `flow {}` and `emit()`.                                                                 |
| `Result<T>`       | Optional wrapper of [Kotlin stdlib](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-result/) for error-safe results using `try/catch` inside the generated code. |
| `Flow<Result<T>>` | Variant flow of [Result](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-result/) wrapper using `try/catch` inside the generated code.                           |


### ⚠️ Unsupported Features
- Generic types in functions are not allowed.
- Context parameters are not supported yet.
- Return a **prepared request** support soon**

## Examples

**Example – Using suspend function**

```kotlin
@GET("/user/{id}")
suspend fun getUser(@Path("id") id: String): UserResponse
```

**Example – Returning Flow**

```kotlin
@GET("/updates")
fun observeUpdates(): Flow<UpdateEvent>
```

**Example – Using Result for error handling**

```kotlin
@GET("/info")
suspend fun getInfo(): Result<InfoResponse>
```

**Example – Using Flow and Result for error handling**

```kotlin
@GET("/info")
fun getInfo(): Flow<Result<InfoResponse>>
```

## Summary

- Functions must be `suspend` or return `Flow`.
- Optional wrapping with [`Result` of Kotlin stdlib](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-result/) is supported.
