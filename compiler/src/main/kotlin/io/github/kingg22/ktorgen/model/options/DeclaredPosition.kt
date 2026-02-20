package io.github.kingg22.ktorgen.model.options

interface DeclaredPosition {
    val isDeclaredAtInterface: Boolean
    val isDeclaredAtCompanionObject: Boolean
    val isDeclaredAtFunctionLevel: Boolean get() = false
}
