package io.github.kingg22.ktorgen.model

import com.squareup.kotlinpoet.AnnotationSpec
import io.github.kingg22.ktorgen.KtorGenWithoutCoverage

open class ClassGenerationOptions(
    val generatedName: String,
    goingToGenerate: Boolean,
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
    goingToGenerate = goingToGenerate,
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

    @Suppress("kotlin:S107") // Needs to create a copy method manually because is open class
    @KtorGenWithoutCoverage
    fun copy(
        generatedName: String = this.generatedName,
        basePath: String = this.basePath,
        goingToGenerate: Boolean = this.goingToGenerate,
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

    @KtorGenWithoutCoverage
    override fun toString() =
        "ClassGenerationOptions(generatedName='$generatedName', classVisibilityModifier='$classVisibilityModifier', constructorVisibilityModifier='$constructorVisibilityModifier', functionVisibilityModifier='$functionVisibilityModifier', customFileHeader='$customFileHeader', customClassHeader='$customClassHeader', basePath='$basePath', generateTopLevelFunction=$generateTopLevelFunction, generateCompanionExtFunction=$generateCompanionExtFunction, generateHttpClientExtension=$generateHttpClientExtension, extensionFunctionAnnotation=$extensionFunctionAnnotation, options=${super.toString()})"

    companion object {
        @JvmStatic
        fun default(generatedName: String, visibilityModifier: String) = ClassGenerationOptions(
            generatedName = generatedName,
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
