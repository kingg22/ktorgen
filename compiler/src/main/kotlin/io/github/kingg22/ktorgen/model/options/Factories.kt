package io.github.kingg22.ktorgen.model.options

import com.google.devtools.ksp.symbol.KSFunctionDeclaration

sealed interface Factories {
    data class TopLevelFactory(
        override val name: String,
        override val isDeclaredAtInterface: Boolean,
        override val isDeclaredAtCompanionObject: Boolean,
    ) : Factories,
        Named,
        DeclaredPosition
    data class CompanionExtension(
        override val name: String,
        override val isDeclaredAtInterface: Boolean,
        override val isDeclaredAtCompanionObject: Boolean,
    ) : Factories,
        Named,
        DeclaredPosition
    data class HttpClientExtension(
        override val name: String,
        override val isDeclaredAtInterface: Boolean,
        override val isDeclaredAtCompanionObject: Boolean,
    ) : Factories,
        Named,
        DeclaredPosition

    // TODO change type of function to FunctionData or minimal class
    data class KmpExpectActual(val expectFunctions: List<KSFunctionDeclaration>) : Factories
}
