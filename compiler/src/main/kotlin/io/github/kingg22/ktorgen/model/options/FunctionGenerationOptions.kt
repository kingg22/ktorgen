package io.github.kingg22.ktorgen.model.options

data class FunctionGenerationOptions(override val goingToGenerate: Boolean, val customHeader: String?) : Generable {
    companion object {
        @JvmField
        val DEFAULT = FunctionGenerationOptions(goingToGenerate = true, customHeader = null)
    }
}
