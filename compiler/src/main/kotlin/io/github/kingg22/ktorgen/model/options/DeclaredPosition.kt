package io.github.kingg22.ktorgen.model.options

interface DeclaredPosition {
    val isDeclaredAtInterface: Boolean get() = false
    val isDeclaredAtCompanionObject: Boolean get() = false
    val isDeclaredAtFunctionLevel: Boolean get() = false
}
