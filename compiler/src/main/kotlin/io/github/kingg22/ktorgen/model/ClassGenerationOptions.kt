package io.github.kingg22.ktorgen.model

import com.squareup.kotlinpoet.AnnotationSpec

open class ClassGenerationOptions(
    val generatedName: String,
    goingToGenerate: Boolean,
    val visibilityModifier: String,
    val classVisibilityModifier: String,
    val constructorVisibilityModifier: String,
    val functionVisibilityModifier: String,
    propagateAnnotations: Boolean,
    annotations: Set<AnnotationSpec>,
    optIns: Set<AnnotationSpec>,
    val customFileHeader: String,
    val customClassHeader: String,
    val basePath: String,
    val generateTopLevelFunction: Boolean,
    val generateCompanionExtFunction: Boolean,
    val generateHttpClientExtension: Boolean,
    val extensionFunctionAnnotation: Set<AnnotationSpec>,
    optInAnnotation: AnnotationSpec? = null,
) : Options(
    generate = goingToGenerate,
    propagateAnnotations = propagateAnnotations,
    annotations = annotations,
    optIns = optIns,
    optInAnnotation = optInAnnotation,
) {
    constructor(
        options: ClassGenerationOptions,
    ) : this(
        generatedName = options.generatedName,
        goingToGenerate = options.goingToGenerate,
        visibilityModifier = options.visibilityModifier,
        classVisibilityModifier = options.classVisibilityModifier,
        constructorVisibilityModifier = options.constructorVisibilityModifier,
        functionVisibilityModifier = options.functionVisibilityModifier,
        propagateAnnotations = options.propagateAnnotations,
        annotations = options.annotations,
        optIns = options.optIns,
        customFileHeader = options.customFileHeader,
        customClassHeader = options.customClassHeader,
        basePath = options.basePath,
        generateTopLevelFunction = options.generateTopLevelFunction,
        generateCompanionExtFunction = options.generateCompanionExtFunction,
        generateHttpClientExtension = options.generateHttpClientExtension,
        extensionFunctionAnnotation = options.extensionFunctionAnnotation,
        optInAnnotation = options.optInAnnotation,
    )

    fun copy(block: (ClassGenerationOptions) -> ClassGenerationOptions) = block(this)

    @Suppress("kotlin:S107")
    fun copy(
        generatedName: String = this.generatedName,
        basePath: String = this.basePath,
        goingToGenerate: Boolean = true,
        visibilityModifier: String = this.visibilityModifier,
        classVisibilityModifier: String = this.classVisibilityModifier,
        constructorVisibilityModifier: String = this.constructorVisibilityModifier,
        functionVisibilityModifier: String = this.functionVisibilityModifier,
        generateTopLevelFunction: Boolean = this.generateTopLevelFunction,
        generateCompanionExtFunction: Boolean = this.generateCompanionExtFunction,
        generateHttpClientExtension: Boolean = this.generateHttpClientExtension,
        propagateAnnotations: Boolean = this.propagateAnnotations,
        annotationsToPropagate: Set<AnnotationSpec> = this.annotations,
        optIns: Set<AnnotationSpec> = this.optIns,
        optInAnnotation: AnnotationSpec? = this.optInAnnotation,
        extensionFunctionAnnotation: Set<AnnotationSpec> = this.extensionFunctionAnnotation,
        customFileHeader: String = this.customFileHeader,
        customClassHeader: String = this.customClassHeader,
    ) = ClassGenerationOptions(
        generatedName = generatedName,
        basePath = basePath,
        goingToGenerate = goingToGenerate,
        generateTopLevelFunction = generateTopLevelFunction,
        generateCompanionExtFunction = generateCompanionExtFunction,
        generateHttpClientExtension = generateHttpClientExtension,
        visibilityModifier = visibilityModifier,
        classVisibilityModifier = classVisibilityModifier,
        constructorVisibilityModifier = constructorVisibilityModifier,
        functionVisibilityModifier = functionVisibilityModifier,
        propagateAnnotations = propagateAnnotations,
        annotations = annotationsToPropagate,
        optIns = optIns,
        extensionFunctionAnnotation = extensionFunctionAnnotation,
        customFileHeader = customFileHeader,
        customClassHeader = customClassHeader,
        optInAnnotation = optInAnnotation,
    )

    companion object {
        fun default(generatedName: String, visibilityModifier: String) = ClassGenerationOptions(
            generatedName = generatedName,
            visibilityModifier = visibilityModifier,
            classVisibilityModifier = visibilityModifier,
            constructorVisibilityModifier = visibilityModifier,
            functionVisibilityModifier = visibilityModifier,
            basePath = "",
            goingToGenerate = true,
            generateTopLevelFunction = true,
            generateCompanionExtFunction = false,
            generateHttpClientExtension = false,
            propagateAnnotations = true,
            annotations = emptySet(),
            optIns = emptySet(),
            extensionFunctionAnnotation = emptySet(),
            customFileHeader = KTORG_GENERATED_FILE_COMMENT,
            customClassHeader = "",
        )
    }
}
