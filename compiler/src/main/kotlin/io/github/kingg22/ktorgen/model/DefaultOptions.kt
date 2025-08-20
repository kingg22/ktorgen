package io.github.kingg22.ktorgen.model

import com.squareup.kotlinpoet.AnnotationSpec

class DefaultOptions
/** Options on Interface or Companion Object */
constructor(
    override val generatedName: String,
    override val visibilityModifier: String,
    override val basePath: String = "",
    override val goingToGenerate: Boolean = true,
    override val generateTopLevelFunction: Boolean = true,
    override val generateCompanionExtFunction: Boolean = false,
    override val generateHttpClientExtension: Boolean = false,
    override val propagateAnnotations: Boolean = true,
    override val annotationsToPropagate: Set<AnnotationSpec> = emptySet(),
    override val optIns: Set<AnnotationSpec> = emptySet(),
    override val optInAnnotation: AnnotationSpec? = null,
    override val customFileHeader: String = KTORG_GENERATED_FILE_COMMENT,
    customClassHeader: String = "",
) : GenOptions.GenTypeOption {
    override val customHeader = customClassHeader

    /** Options on Function */
    constructor(
        visibilityModifier: String,
        goingToGenerate: Boolean = true,
        propagateAnnotations: Boolean = true,
        annotationsToPropagate: Set<AnnotationSpec> = emptySet(),
        optIns: Set<AnnotationSpec> = emptySet(),
        optInAnnotation: AnnotationSpec? = null,
        customHeader: String = "",
    ) : this(
        generatedName = "",
        goingToGenerate = goingToGenerate,
        visibilityModifier = visibilityModifier,
        propagateAnnotations = propagateAnnotations,
        annotationsToPropagate = annotationsToPropagate,
        optInAnnotation = optInAnnotation,
        optIns = optIns,
        customClassHeader = customHeader,
    )

    override fun copy(
        generatedName: String,
        basePath: String,
        goingToGenerate: Boolean,
        visibilityModifier: String,
        generateTopLevelFunction: Boolean,
        generateCompanionExtFunction: Boolean,
        generateHttpClientExtension: Boolean,
        propagateAnnotations: Boolean,
        annotationsToPropagate: Set<AnnotationSpec>,
        optIns: Set<AnnotationSpec>,
        optInAnnotation: AnnotationSpec?,
        customFileHeader: String,
        customClassHeader: String,
    ): GenOptions.GenTypeOption = DefaultOptions(
        generatedName = generatedName,
        basePath = basePath,
        goingToGenerate = goingToGenerate,
        generateTopLevelFunction = generateTopLevelFunction,
        generateCompanionExtFunction = generateCompanionExtFunction,
        generateHttpClientExtension = generateHttpClientExtension,
        visibilityModifier = visibilityModifier,
        propagateAnnotations = propagateAnnotations,
        annotationsToPropagate = annotationsToPropagate,
        optIns = optIns,
        optInAnnotation = optInAnnotation,
        customFileHeader = customFileHeader,
        customClassHeader = customClassHeader,
    )

    override fun copy(
        goingToGenerate: Boolean,
        visibilityModifier: String,
        propagateAnnotations: Boolean,
        annotationsToPropagate: Set<AnnotationSpec>,
        optIns: Set<AnnotationSpec>,
        optInAnnotation: AnnotationSpec?,
        customClassHeader: String,
    ): GenOptions.GenTypeOption = DefaultOptions(
        generatedName = generatedName,
        basePath = basePath,
        goingToGenerate = goingToGenerate,
        generateTopLevelFunction = generateTopLevelFunction,
        generateCompanionExtFunction = generateCompanionExtFunction,
        generateHttpClientExtension = generateHttpClientExtension,
        visibilityModifier = visibilityModifier,
        propagateAnnotations = propagateAnnotations,
        annotationsToPropagate = annotationsToPropagate,
        optIns = optIns,
        optInAnnotation = optInAnnotation,
        customFileHeader = customFileHeader,
        customClassHeader = customClassHeader,
    )
}
