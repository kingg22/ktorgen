package io.github.kingg22.ktorgen.core

/**
 * Whether to generate a top-level function in the same package as the interface.
 *
 * This function will allow instantiating the implementation by passing an `HttpClient`.
 *
 * Example: `fun UserRoute(client: HttpClient): UserRoute = UserRouteImpl(httpClient = client)`
 *
 * @param name The name of the function to generate. Defaults to the interface name.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class KtorGenTopLevelFactory(val name: String = KTORGEN_DEFAULT_NAME)

/**
 * Whether to generate a `create(client)` extension function of interface's companion object.
 *
 * This **requires** the annotation to be placed on the companion itself or **declared a companion object explicit**.
 *
 * Example:
 * ```kotlin
 * interface UserRoute {
 *     @KtorGenCompanionExtFactory
 *     companion object
 * }
 * fun UserRoute.Companion.create(client): UserRoute =
 *     UserRouteImpl(httpClient = client)
 * ```
 *
 * @param name The name of the function to generate. Defaults to `create`.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class KtorGenCompanionExtFactory(val name: String = "create")

/**
 * Whether to generate an extension function on `HttpClient` to instantiate the API.
 *
 * Example: `fun HttpClient.userRoute(): UserRoute`
 *
 * @param name The name of the function to generate. Defaults to the interface name.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class KtorGenHttpClientExtFactory(val name: String = KTORGEN_DEFAULT_NAME)

// -- EXPERIMENTAL --

/**
 * An annotation used to mark an **expect factory function** for multi-platform support in KSP running on each platform.
 *
 * This annotation signals to code generation tools that an actual function declaration
 * should be generated for Kotlin Multiplatform platforms.
 *
 * The signature of the function needs to match a constructor or factory function defined in [@KtorGen][KtorGen].
 * If not, the code generation will fail.
 *
 * It is part of the **experimental API**.
 *
 * Example:
 * ```kotlin
 * // Only function can be `expect`
 * interface ApiService {
 *     @GET("/users")
 *     suspend fun getUsers(): List<User>
 * }
 *
 * // KtorGen going to generate an actual function for each platform with valid implementation
 * @KtorGenKmpFactory
 * expect fun ApiService(httpClient: HttpClient): ApiService
 * ```
 * @see KtorGen
 * @see KtorGenFunction
 */
@OptIn(ExperimentalMultiplatform::class)
@KtorGenExperimental
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
@OptionalExpectation
expect annotation class KtorGenKmpFactory()
