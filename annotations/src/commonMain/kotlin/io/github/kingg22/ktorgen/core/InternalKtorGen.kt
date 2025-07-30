package io.github.kingg22.ktorgen.core

/**
 * INTERNAL API can need to be public for a reason
 *
 * External use is not allowed, binary compatibility is not guaranteed
 */
@RequiresOptIn(
    "This is a internal API of KtorGen, you don't need to use this.",
    RequiresOptIn.Level.ERROR,
)
@MustBeDocumented
annotation class InternalKtorGen
