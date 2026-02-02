package io.github.kingg22.ktorgen

import com.google.devtools.ksp.symbol.KSNode
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/** Run all the work in try-catch-finally, this handle start to finish and die if occurs exceptions, rethrows when a nested fatal error occurs */
internal inline fun <R> DiagnosticSender.work(block: () -> R): R {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return try {
        if (!isStarted) start()
        val result = block()
        if (isInProgress) finish()
        result
    } catch (e: KtorGenFatalError) {
        throw e
    } catch (e: Exception) {
        die(e.message.orEmpty(), cause = e)
    }
}

/** If the condition is false, die */
@Suppress("NOTHING_TO_INLINE") // Don't add this file to stacktrace, the message can be a lambda*
@KtorGenWithoutCoverage // The idea is not to throw this error
internal inline fun DiagnosticSender.require(
    condition: Boolean,
    message: String,
    symbol: KSNode? = null,
    cause: Exception? = null,
) {
    contract { returns() implies condition }
    if (!condition) die(message, symbol, cause)
}

@KtorGenWithoutCoverage // The idea is not to throw this error
internal inline fun DiagnosticSender.require(
    condition: Boolean,
    symbol: KSNode? = null,
    cause: Exception? = null,
    lazyMessage: () -> String,
) {
    contract { returns() implies condition }
    if (!condition) {
        val (message, suppressException) = safeMessage(lazyMessage)
        die(message, symbol, cause?.applyIfNotNull(suppressException, Exception::addSuppressed) ?: suppressException)
    }
}

/** If the value is null, die */
@Suppress("NOTHING_TO_INLINE") // Don't add this file to stacktrace, the message can be a lambda*
@KtorGenWithoutCoverage // The idea is not to throw this error
internal inline fun <T> DiagnosticSender.requireNotNull(
    value: T?,
    message: String,
    symbol: KSNode? = null,
    cause: Exception? = null,
): T {
    contract { returns() implies (value != null) }
    if (value == null) die(message, symbol, cause)
    return value
}

internal inline fun <T> T.applyIf(condition: Boolean, block: T.() -> Unit): T {
    contract { callsInPlace(block, InvocationKind.AT_MOST_ONCE) }
    if (condition) block()
    return this
}

internal inline fun <T, R> T.applyIfNotNull(nullable: R?, block: T.(R) -> Unit): T {
    contract { callsInPlace(block, InvocationKind.AT_MOST_ONCE) }
    if (nullable != null) block(nullable)
    return this
}

/**
 * Checks the implementation by evaluating the given condition and throws an error if the condition is not met.
 * Differs to [DiagnosticSender] methods because this is an internal implementation of processor logic,
 * not an error of user code.
 *
 * @param value A boolean condition that is checked. If false, an error is thrown.
 * @param lazyMessage A lambda function to generate the error message in case of failure.
 * The lambda is called at most once and only if the condition is false.
 * @throws KtorGenFatalError.KtorGenImplementationError if the condition is false.
 */
@KtorGenWithoutCoverage // The idea is not to throw this error
internal inline fun checkImplementation(value: Boolean, lazyMessage: () -> String) {
    contract {
        returns() implies value
        callsInPlace(lazyMessage, InvocationKind.AT_MOST_ONCE)
    }
    if (!value) checkImplementation(lazyMessage)
}

/**
 * Throws [KtorGenFatalError.KtorGenImplementationError] with the given message.
 * Handle errors of lambda with a default message.
 * Differs to [DiagnosticSender] methods because this is an internal implementation of processor logic,
 * not an error of user code.
 * @param lazyMessage A lambda function to generate the error message
 * @throws KtorGenFatalError.KtorGenImplementationError
 */
@KtorGenWithoutCoverage // The idea is not to throw this error
internal inline fun checkImplementation(lazyMessage: () -> String): Nothing {
    contract { callsInPlace(lazyMessage, InvocationKind.EXACTLY_ONCE) }
    val (message, suppressException) = safeMessage(lazyMessage)
    throw KtorGenFatalError.KtorGenImplementationError(message, suppressException)
}

/**
 * Provide a try-catch with a default message if the lambda throws an exception, plus the exception itself
 * @param lazyMessage A lambda function to generate the error message
 * @return The error message and the exception if any
 */
@KtorGenWithoutCoverage
@JvmOverloads
@Suppress("WRONG_INVOCATION_KIND")
internal inline fun safeMessage(
    lazyMessage: () -> String,
    defaultMessage: String = "Caught exception during error message generation",
): Pair<String, Exception?> {
    contract { callsInPlace(lazyMessage, InvocationKind.EXACTLY_ONCE) }
    return try {
        lazyMessage() to null
    } catch (e: Exception) {
        defaultMessage to e
    }
}
