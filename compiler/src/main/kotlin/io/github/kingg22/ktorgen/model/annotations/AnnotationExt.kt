@file:OptIn(KtorGenExperimental::class)

package io.github.kingg22.ktorgen.model.annotations

import io.github.kingg22.ktorgen.KtorGenLogger.Companion.COOKIE_ON_FUNCTION_WITHOUT_VALUE
import io.github.kingg22.ktorgen.core.*
import io.github.kingg22.ktorgen.http.*
import io.github.kingg22.ktorgen.model.KTORGEN_DEFAULT_VALUE

fun String.removeWhitespace(): String = this.trim().replace("\\s+".toRegex(), "")

// a lot of try-catch because default values is not working for KMP builds https://github.com/google/ksp/issues/2356

/** This is a mapper function, handle try-catch of arrays. Throw exception when parameterName is required */
fun Cookie.toCookieValues(parameterName: String? = null): CookieValues {
    // clean here because don't need validation, otherwise don't do it
    var isParameter: Boolean

    val finalValue = try {
        when (val cleanValue = value.removeWhitespace()) {
            KTORGEN_DEFAULT_VALUE -> {
                isParameter = true
                requireNotNull(parameterName) { COOKIE_ON_FUNCTION_WITHOUT_VALUE }
            }
            else -> {
                isParameter = false
                cleanValue
            }
        }
    } catch (_: NoSuchElementException) {
        isParameter = true
        requireNotNull(parameterName) { COOKIE_ON_FUNCTION_WITHOUT_VALUE }
    }

    return CookieValues(
        name = try {
            name.removeWhitespace()
        } catch (_: NoSuchElementException) {
            throw IllegalArgumentException("Cookie name is required")
        },
        value = finalValue,
        isValueParameter = isParameter,
        maxAge = try {
            maxAge
        } catch (_: NoSuchElementException) {
            0
        },
        expiresTimestamp = try {
            expiresTimestamp.takeIf { it != -1L }
        } catch (_: NoSuchElementException) {
            null
        },
        domain = try {
            domain.removeWhitespace().takeIf(String::isNotBlank)
        } catch (_: NoSuchElementException) {
            null
        },
        path = try {
            path.removeWhitespace().takeIf(String::isNotBlank)
        } catch (_: NoSuchElementException) {
            null
        },
        secure = try {
            secure
        } catch (_: NoSuchElementException) {
            false
        },
        httpOnly = try {
            httpOnly
        } catch (_: NoSuchElementException) {
            false
        },
        extensions = try {
            extensions.associate { (key, value) ->
                key.removeWhitespace() to value.removeWhitespace().takeIf(String::isNotBlank)
            }
        } catch (_: NoSuchElementException) {
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
