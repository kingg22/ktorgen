@file:JvmName("DiagnosticSenderExt")
@file:JvmMultifileClass

package io.github.kingg22.ktorgen

import com.google.devtools.ksp.symbol.KSNode
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/** Run all the work in try-catch-finally, this handle start to finish and die if occurs exceptions, rethrows when a nested fatal error occurs */
internal inline fun <R> DiagnosticSender.work(block: () -> R): R {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return try {
        if (!isStarted()) start()
        val result = block()
        if (isInProgress()) finish()
        result
    } catch (e: KtorGenFatalError) {
        throw e
    } catch (e: Exception) {
        die(e.message ?: "", null, e)
    }
}

/** If the condition is false, die */
@Suppress("NOTHING_TO_INLINE")
internal inline fun DiagnosticSender.require(condition: Boolean, message: String, symbol: KSNode? = null) {
    contract { returns() implies condition }
    if (!condition) die(message, symbol)
}

/** If the value is null, die */
@Suppress("NOTHING_TO_INLINE")
internal inline fun <T> DiagnosticSender.requireNotNull(value: T?, message: String, symbol: KSNode? = null): T {
    contract { returns() implies (value != null) }
    if (value == null) die(message, symbol)
    return value
}

inline fun <T> T.applyIf(condition: Boolean, block: T.() -> Unit): T {
    contract { callsInPlace(block, InvocationKind.AT_MOST_ONCE) }
    if (condition) block()
    return this
}

inline fun <T, R> T.applyIfNotNull(nullable: R?, block: T.(R) -> Unit): T {
    contract { callsInPlace(block, InvocationKind.AT_MOST_ONCE) }
    if (nullable != null) block(nullable)
    return this
}
