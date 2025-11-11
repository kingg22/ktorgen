
## 🛠 Request Customization (Advanced*)

You can customize the outgoing request using one of three supported parameters:

| Parameter type                  | Scope               | Description                                                     |
|---------------------------------|---------------------|-----------------------------------------------------------------|
| `HttpRequestBuilder.() -> Unit` | Lambda              | Adds inline modifications before sending the request.           |
| `HttpRequestBuilder`            | Mutable object      | Passes a preconfigured `HttpRequestBuilder` instance.           |
| `HttpRequest`                   | Immutable reference | Uses an already built `HttpRequest` for advanced scenarios.     |
| `HttpRequestData`               | Immutable reference | Uses an already built `HttpRequestData` for advanced scenarios. |

**Example – Inline customization**

```kotlin
@GET("/profile")
suspend fun getProfile(builder: HttpRequestBuilder.() -> Unit): UserProfile
```

Usage:

```kotlin
service.getProfile {
    header("Authorization", "Bearer $token")
    expectSuccess = false
}
```

**Example – Using a preconfigured builder**

```kotlin
@GET("/profile")
suspend fun getProfile(builder: HttpRequestBuilder): UserProfile
```

Usage:

```kotlin
val builder = HttpRequestBuilder().apply {
    header("X-Api-Version", "2")
    parameter("mode", "compact")
}

service.getProfile(builder)
```

**Example – Using an HttpRequest directly**

```kotlin
@GET("/profile")
suspend fun getProfile(request: HttpRequest): UserProfile
```

Usage:

_I don't know sincerely, sorry, but is supported!_

**Example - Using an HttpRequestData**

```kotlin
@GET("/profile")
suspend fun getProfile(request: HttpRequestData): UserProfile
```

Usage:

```kotlin
val customRequest: HttpRequestData = HttpRequestBuilder("https://api.example.com/profile").build()
service.getProfile(customRequest)
```

## Summary
- Request customization is available through builder or request parameters.
- Focus remains on simplicity, extensibility, and clear debugging aligned with Ktor Client’s design.
