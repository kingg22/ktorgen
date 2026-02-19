package io.github.kingg22.ktorgen.model.options

import io.github.kingg22.ktorgen.model.KTORG_GENERATED_FILE_COMMENT

/** Equivalent to [io.github.kingg22.ktorgen.core.KtorGen] */
data class ClassGenerationOptions(
    val generatedName: String,
    override val goingToGenerate: Boolean,
    val customFileName: String,
    val customFileHeader: String,
    val customClassHeader: String,
    val basePath: String,
    override val isDeclaredAtInterface: Boolean,
    override val isDeclaredAtCompanionObject: Boolean,
) : Generable,
    DeclaredPosition {
    companion object {
        @JvmStatic
        fun default(generatedName: String, isDeclaredAtCompanionObject: Boolean, isDeclaredAtInterface: Boolean) =
            ClassGenerationOptions(
                generatedName = generatedName,
                goingToGenerate = true,
                customFileName = generatedName,
                customFileHeader = KTORG_GENERATED_FILE_COMMENT,
                customClassHeader = "",
                basePath = "",
                isDeclaredAtInterface = isDeclaredAtInterface,
                isDeclaredAtCompanionObject = isDeclaredAtCompanionObject,
            )
    }
}
