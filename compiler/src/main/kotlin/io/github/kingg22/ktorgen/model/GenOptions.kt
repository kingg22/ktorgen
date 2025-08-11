package io.github.kingg22.ktorgen.model

import com.squareup.kotlinpoet.AnnotationSpec

/** Options on each function */
interface GenOptions {
    val goingToGenerate: Boolean
        get() = true
    val visibilityModifier: String
        get() = "public"
    val propagateAnnotations: Boolean
        get() = true
    val annotationsToPropagate: Set<AnnotationSpec>
        get() = emptySet()
    val optIns: Set<AnnotationSpec>
        get() = emptySet()
    val optInAnnotation: AnnotationSpec?
        get() = null
    val customHeader: String
        get() = ""

    fun copy(
        goingToGenerate: Boolean = true,
        visibilityModifier: String = this.visibilityModifier,
        propagateAnnotations: Boolean = this.propagateAnnotations,
        annotationsToPropagate: Set<AnnotationSpec> = this.annotationsToPropagate,
        optIns: Set<AnnotationSpec> = this.optIns,
        optInAnnotation: AnnotationSpec? = this.optInAnnotation,
        customClassHeader: String = this.customHeader,
    ): GenOptions

    /** Options on Interface or it Companion Object */
    interface GenTypeOption : GenOptions {
        val generatedName: String
        val basePath: String
            get() = ""
        val generateTopLevelFunction: Boolean
            get() = true
        val generateCompanionExtFunction: Boolean
            get() = false
        val generateHttpClientExtension: Boolean
            get() = false
        val customFileHeader: String
            get() = KTORG_GENERATED_FILE_COMMENT

        @Suppress("kotlin:S107")
        fun copy(
            generatedName: String = this.generatedName,
            basePath: String = this.basePath,
            goingToGenerate: Boolean = true,
            visibilityModifier: String = this.visibilityModifier,
            generateTopLevelFunction: Boolean = this.generateTopLevelFunction,
            generateCompanionExtFunction: Boolean = this.generateCompanionExtFunction,
            generateHttpClientExtension: Boolean = this.generateHttpClientExtension,
            propagateAnnotations: Boolean = this.propagateAnnotations,
            annotationsToPropagate: Set<AnnotationSpec> = this.annotationsToPropagate,
            optIns: Set<AnnotationSpec> = this.optIns,
            optInAnnotation: AnnotationSpec? = this.optInAnnotation,
            customFileHeader: String = this.customFileHeader,
            customClassHeader: String = this.customHeader,
        ): GenTypeOption
    }
}
