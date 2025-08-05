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
            extensions.toExtensionsMap()
        } catch (_: NoSuchElementException) {
            emptyMap()
        },
    )
}

fun Array<Cookie.PairString>.toExtensionsMap() = this.associate { (key, value) ->
    // clean here because don't need validation, otherwise don't do it
    key.removeWhitespace() to value.removeWhitespace().takeIf(String::isNotBlank)
}

private operator fun Cookie.PairString.component1() = first
private operator fun Cookie.PairString.component2() = second
