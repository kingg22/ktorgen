package io.github.kingg22.ktorgen.model

import com.squareup.kotlinpoet.AnnotationSpec

class DefaultOptions
/** Options on Interface or Companion Object */
constructor(
    override val generatedName: String,
    override val basePath: String = "",
    override val goingToGenerate: Boolean = true,
    override val visibilityModifier: String = "public",
    override val generateTopLevelFunction: Boolean = true,
    override val generateCompanionExtFunction: Boolean = false,
    override val generateHttpClientExtension: Boolean = false,
    override val propagateAnnotations: Boolean = true,
    override val annotationsToPropagate: Set<AnnotationSpec.Builder> = emptySet(),
    override val optIns: Set<AnnotationSpec.Builder> = emptySet(),
    override val customFileHeader: String = KTORG_GENERATED_FILE_COMMENT,
    customClassHeader: String = "",
) : GenOptions.GenTypeOption {
    override val customHeader = customClassHeader

    /** Options on Function */
    constructor(
        goingToGenerate: Boolean = true,
        visibilityModifier: String = "public",
        propagateAnnotations: Boolean = true,
        annotationsToPropagate: Set<AnnotationSpec.Builder> = emptySet(),
        optIns: Set<AnnotationSpec.Builder> = emptySet(),
        customHeader: String = "",
    ) : this(
        generatedName = "",
        goingToGenerate = goingToGenerate,
        visibilityModifier = visibilityModifier,
        propagateAnnotations = propagateAnnotations,
        annotationsToPropagate = annotationsToPropagate,
        optIns = optIns,
        customClassHeader = customHeader,
    )
}
