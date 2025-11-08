@file:JvmName("AnnotationExt")
@file:JvmMultifileClass
@file:OptIn(io.github.kingg22.ktorgen.core.KtorGenExperimental::class)

package io.github.kingg22.ktorgen.model.annotations

import io.github.kingg22.ktorgen.DiagnosticSender
import io.github.kingg22.ktorgen.KtorGenLogger.Companion.COOKIE_ON_FUNCTION_WITHOUT_VALUE
import io.github.kingg22.ktorgen.core.KtorGen
import io.github.kingg22.ktorgen.core.KtorGenFunction
import io.github.kingg22.ktorgen.core.KtorGenFunctionKmp
import io.github.kingg22.ktorgen.http.*
import io.github.kingg22.ktorgen.model.KTORGEN_DEFAULT_VALUE
import io.github.kingg22.ktorgen.requireNotNull

fun String.removeWhitespace(): String = this.trim().replace("\\s+".toRegex(), "")

// a lot of try-catch because default values is not working for KMP builds https://github.com/google/ksp/issues/2356

/**
 * This is a mapper function, handle try-catch of default values, die when [parameterName] is required, but is null.
 * @receiver the [Cookie] annotation to convert to [CookieValues]
 * @param timer the [DiagnosticSender] to use for logging
 * @param parameterName the name of the parameter, used as a fallback value
 * @return [CookieValues] cleaned
 */
context(timer: DiagnosticSender)
fun Cookie.toCookieValues(parameterName: String? = null): CookieValues {
    // clean here because don't need validation, otherwise don't do it
    var isParameter: Boolean

    val finalValue = try {
        when (val cleanValue = value.removeWhitespace()) {
            KTORGEN_DEFAULT_VALUE -> {
                isParameter = true
                timer.requireNotNull(parameterName, COOKIE_ON_FUNCTION_WITHOUT_VALUE)
            }
            else -> {
                isParameter = false
                cleanValue
            }
        }
    } catch (e: NoSuchElementException) {
        isParameter = true
        timer.addStep("Caught exception while parsing cookie value, assuming it's a parameter")
        timer.requireNotNull(parameterName, COOKIE_ON_FUNCTION_WITHOUT_VALUE, cause = e)
    }

    return CookieValues(
        name = try {
            name.removeWhitespace()
        } catch (e: NoSuchElementException) {
            timer.die("Cookie name is required", cause = e)
        },
        value = finalValue,
        isValueParameter = isParameter,
        maxAge = try {
            maxAge
        } catch (_: NoSuchElementException) {
            timer.addStep("Cookie maxAge is not set, assuming it's 0")
            0
        },
        expiresTimestamp = try {
            expiresTimestamp.takeIf { it != -1L }
        } catch (_: NoSuchElementException) {
            timer.addStep("Cookie expiresTimestamp is not set, assuming it's null")
            null
        },
        domain = try {
            domain.removeWhitespace().takeIf { it.isNotBlank() }
        } catch (_: NoSuchElementException) {
            timer.addStep("Cookie domain is not set, assuming it's null")
            null
        },
        path = try {
            path.removeWhitespace().takeIf { it.isNotBlank() }
        } catch (_: NoSuchElementException) {
            timer.addStep("Cookie path is not set, assuming it's null")
            null
        },
        secure = try {
            secure
        } catch (_: NoSuchElementException) {
            timer.addStep("Cookie secure is not set, assuming it's false")
            false
        },
        httpOnly = try {
            httpOnly
        } catch (_: NoSuchElementException) {
            timer.addStep("Cookie httpOnly is not set, assuming it's false")
            false
        },
        extensions = try {
            extensions.associate { (key, value) ->
                key.removeWhitespace() to value.removeWhitespace().takeIf { it.isNotBlank() }
            }
        } catch (_: NoSuchElementException) {
            timer.addStep("Cookie extensions are not set, assuming it's empty")
            emptyMap()
        },
    )
}

private operator fun Cookie.PairString.component1() = first
private operator fun Cookie.PairString.component2() = second

// experimental is a meta-annotation (documented) for optIn only. this doesn't process it
val ktorGenAnnotationsClass = setOf(KtorGen::class)

val ktorGenAnnotationsIndication = setOf(
    KtorGen::class,
    KtorGenFunction::class,
    KtorGenFunctionKmp::class,
)

val ktorGenAnnotationsFunction = setOf(
    KtorGenFunction::class,
    HTTP::class,
    GET::class,
    POST::class,
    PUT::class,
    PATCH::class,
    DELETE::class,
    HEAD::class,
    OPTIONS::class,
    Header::class,
    Cookie::class,
    FormUrlEncoded::class,
    Multipart::class,
    Fragment::class,
)

val ktorGenAnnotationsParameter = setOf(
    Body::class,
    Cookie::class,
    Url::class,
    Field::class,
    FieldMap::class,
    HeaderMap::class,
    HeaderParam::class,
    Part::class,
    PartMap::class,
    Path::class,
    Query::class,
    QueryMap::class,
    QueryName::class,
    Tag::class,
)

val ktorGenAnnotations =
    ktorGenAnnotationsClass + ktorGenAnnotationsIndication + ktorGenAnnotationsFunction + ktorGenAnnotationsParameter
