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
    val annotationsToPropagate: Set<AnnotationSpec.Builder>
        get() = emptySet()
    val optIns: Set<AnnotationSpec.Builder>
        get() = emptySet()
    val customHeader: String
        get() = ""

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
    }
}
