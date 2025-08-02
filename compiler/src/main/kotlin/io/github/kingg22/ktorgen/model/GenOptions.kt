package io.github.kingg22.ktorgen.model

import com.squareup.kotlinpoet.AnnotationSpec

open class GenOptions(
    val goingToGenerate: Boolean = true,
    val visibilityModifier: String = "public",
    val propagateAnnotations: Boolean = true,
    val annotationsToPropagate: Set<AnnotationSpec> = emptySet(),
    val optIns: Set<AnnotationSpec> = emptySet(),
    val customHeader: String = KTORG_GENERATED_COMMENT,
) {
    /** Options on Interface or Companion Object */
    open class GenTypeOption(
        val generatedName: String,
        goingToGenerate: Boolean = true,
        visibilityModifier: String = "public",
        val generateTopLevelFunction: Boolean = true,
        val generateCompanionFunction: Boolean = false,
        val generateExtensions: Boolean = false,
        val jvmStatic: Boolean = false,
        val jsStatic: Boolean = false,
        val generatePublicConstructor: Boolean = false,
        propagateAnnotations: Boolean = true,
        annotationsToPropagate: Set<AnnotationSpec> = emptySet(),
        optIns: Set<AnnotationSpec> = emptySet(),
        customFileHeader: String = KTORG_GENERATED_FILE_COMMENT,
        customClassHeader: String = KTORG_GENERATED_COMMENT,
    ) : GenOptions(
        goingToGenerate = goingToGenerate,
        visibilityModifier = visibilityModifier,
        propagateAnnotations = propagateAnnotations,
        annotationsToPropagate = annotationsToPropagate,
        optIns = optIns,
        customHeader = customClassHeader,
    )
}
