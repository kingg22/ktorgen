package io.github.kingg22.ktorgen.model.options

import io.github.kingg22.ktorgen.KtorGenWithoutCoverage

interface DeclaredPosition {
    val isDeclaredAtInterface: Boolean
    val isDeclaredAtCompanionObject: Boolean

    @get:KtorGenWithoutCoverage
    @property:KtorGenWithoutCoverage
    val isDeclaredAtFunctionLevel: Boolean get() = false
}
