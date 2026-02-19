package io.github.kingg22.ktorgen.core

import org.intellij.lang.annotations.Language

/**
 * Annotation used to generate client implementations for Ktor client interfaces.
 *
 * This annotation is typically used to mark interfaces intended to define API contracts,
 * enabling automatic generation of client-side code based on the provided configuration.
 *
 * The annotation provides customization options such as specifying a custom implementation class name,
 * controlling generation behavior, defining base paths, adding custom file and class headers, and more.
 *
 * By default, the generated implementation will have:
 * - Named as the interface name plus the suffix `Impl`.
 * - A class with the same visibility as the interface.
 * - A constructor with a single `HttpClient` parameter + properties + delegated if are defined in the interface.
 *
 * @see KtorGenFunction
 * @see KtorGenAnnotationPropagation
 * @see KtorGenVisibilityControl
 * @see KtorGenTopLevelFactory
 * @see KtorGenHttpClientExtFactory
 * @see KtorGenCompanionExtFactory
 * @see KtorGenKmpFactory
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class KtorGen(
    /**
     * Custom name for the generated implementation class.
     *
     * If not specified, a default naming convention will be used (name + suffix `Impl`).
     *
     * Example: `name = "UserRoutes"` → class `UserRoutesImpl` will be generated.
     */
    val name: String = KTORGEN_DEFAULT_NAME,

    /** Indicate the annotated interface going to generate the code or not. */
    val generate: Boolean = true,

    /**
     * Set a base path (or URL) for the current interface, all generated functions going to init with this and it path.
     *
     * By default, Ktor Client handle base url for the `HttpClient`.
     *
     * Invalid syntax like `https://github.com///path` _(triple slash where is not common to use)_ throw warnings.
     * @see io.github.kingg22.ktorgen.http.Url
     * @see io.github.kingg22.ktorgen.http.HTTP.path
     */
    @Language("http-url-reference")
    val basePath: String = "",

    /**
     * Custom header **comment** inserted at the top of the generated Kotlin file.
     *
     * Useful for license declarations, warnings, or documentation.
     *
     * By default, there is a notice of _generated_ code.
     */
    val customFileHeader: String = KTORGEN_DEFAULT_NAME,

    /**
     * Custom KDoc comment or annotations for the generated implementation class.
     *
     * Useful to indicate that the class is auto-generated and shouldn't be modified or annotated with custom code.
     */
    val customClassHeader: String = "",

    /**
     * Custom file name for the generated implementation class.
     *
     * By default, the generated file name is the [name] of the class.
     */
    val customFileName: String = KTORGEN_DEFAULT_NAME,
)
