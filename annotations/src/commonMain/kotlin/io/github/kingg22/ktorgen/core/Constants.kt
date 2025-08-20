@file:JvmName("-Constants")
@file:JvmMultifileClass

package io.github.kingg22.ktorgen.core

import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName

/**
 * Indicate a default value going to be generated,
 * depends on where is used the value can be different,
 * see doc on property.
 *
 * This constant is _internal_ and only used to generate code.
 */
internal const val KTORGEN_DEFAULT_NAME = "__DEFAULT__"
