@file:OptIn(InternalKtorGen::class)

package io.github.kingg22.ktorgen.http

import io.github.kingg22.ktorgen.core.InternalKtorGen
import io.github.kingg22.ktorgen.core.KTORGEN_DEFAULT_NAME

/**
 * Query parameter appended to the URL
 *
 * ```kotlin
 * @GET("comments")
 * suspend fun getCommentsById(@Query("postId") id: String): List<Comment>
 *
 * getCommentsById(3)
 * // Generate comments?postId=3
 * ```
 *
 * ```kotlin
 * @GET("comments")
 * suspend fun getCommentsById(@Query("postId") postId: List<String?>): List<Comment>
 *
 * getCommentsById(listOf("3",null,"4"))
 * // Generate comments?postId=3&postId=4
 * ```
 *
 * A `null` values are ignored
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class Query(
    /** Name of the query parameter. Default the name of the _function parameter_. */
    val value: String = KTORGEN_DEFAULT_NAME,
    /** Specifies whether the argument value to the annotated method parameter is already URL encoded */
    val encoded: Boolean = false,
)
