@file:OptIn(InternalKtorGen::class)

package io.github.kingg22.ktorgen.http

import io.github.kingg22.ktorgen.core.InternalKtorGen
import io.github.kingg22.ktorgen.core.KTORGEN_DEFAULT_NAME

/**
 * Adds the argument instance as a request tag using the type as AttributeKey.
 *
 * ```kotlin
 * @GET("/")
 * fun foo(@Tag tag: String)
 * ```
 *
 * Tag arguments may be `null` which will omit them from the request.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class Tag(
    /** Name of the key. Default the name of the _function parameter_ */
    val value: String = KTORGEN_DEFAULT_NAME,
)
