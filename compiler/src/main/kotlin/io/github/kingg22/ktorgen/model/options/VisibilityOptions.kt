package io.github.kingg22.ktorgen.model.options

data class VisibilityOptions(
    val classVisibilityModifier: String,
    val constructorVisibilityModifier: String,
    val factoryFunctionVisibilityModifier: String,
) {
    companion object {
        @JvmStatic
        fun default(visibilityModifier: String) = VisibilityOptions(
            classVisibilityModifier = visibilityModifier,
            constructorVisibilityModifier = visibilityModifier,
            factoryFunctionVisibilityModifier = visibilityModifier,
        )
    }
}
